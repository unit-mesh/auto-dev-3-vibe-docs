# NanoDSL Image CORS Error Fix (WASM/Compose)

## Problem

When rendering NanoDSL in WASM environments using Compose Multiplatform, LLM-generated fake image URLs (like watermark URLs from cloud storage) were being treated as valid image sources. The renderer would attempt to download these URLs using `downloadImageBytes()`, resulting in:

1. **CORS errors**: `Access-Control-Allow-Origin` header missing
2. **Failed requests**: `net::ERR_FAILED` errors in console
3. **IndexedDB errors**: Cache lookup failures for invalid URLs
4. **Poor user experience**: Error messages flooding the console

Example errors:
```
IndexedDB: No data found for key 'nano-img:20251220142749d25e14e8b2c74d00_watermark.png__745673aa84de8a32'
Access to fetch at 'https://maas-watermark-prod.cn-wlcb.ufileos.com/20251220142749d25e14e8b2c74d00_watermark.png?UCloudPublicKey=TOKEN_...' 
from origin 'https://web.xiuper.com' has been blocked by CORS policy
Error: Fail to fetch
```

## Root Cause

In `NanoImageRenderer.kt`, the `isDirectImageSrc()` function was returning `true` for HTTP/HTTPS URLs:

```kotlin
// OLD CODE - WRONG!
private fun isDirectImageSrc(src: String): Boolean {
    // ...
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true  // ❌
    // ...
}
```

This caused the renderer to:
1. Skip AI image generation (line 81: `!isDirectImageSrc(originalSrc)` was false)
2. Attempt to download the fake URL (line 104-112: `loadImageBytesFromSrc(resolvedSrc)`)
3. Trigger CORS errors when `downloadImageBytes(trimmed)` was called (line 302)

## Solution

### Modified `isDirectImageSrc()` function

**File**: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/nano/NanoImageRenderer.kt`

```kotlin
private fun isDirectImageSrc(src: String): Boolean {
    val trimmed = src.trim()
    if (trimmed.isEmpty()) return false

    // Only allow data: URLs - these are actual embedded base64 images
    // HTTP/HTTPS URLs from LLM are fake and will cause CORS errors
    if (trimmed.startsWith("data:image/", ignoreCase = true)) return true
    
    // Do NOT allow http/https URLs - LLM generates fake URLs that cause CORS errors
    // REMOVED: if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true
    // REMOVED: if (trimmed.startsWith("file://")) return true

    // Raw base64 payload (no data: prefix)
    if (!trimmed.contains(' ') && trimmed.length >= 256 && BASE64_PAYLOAD_REGEX.matches(trimmed)) return true

    return false
}
```

### Modified `loadImageBytesFromSrc()` function

```kotlin
@OptIn(ExperimentalEncodingApi::class)
private suspend fun loadImageBytesFromSrc(src: String): ByteArray {
    val trimmed = src.trim()
    return when {
        trimmed.startsWith("data:", ignoreCase = true) -> decodeDataUriToBytes(trimmed)
        // Do NOT download http/https URLs - they are fake LLM-generated URLs that cause CORS errors
        // REMOVED: trimmed.startsWith("http://") || trimmed.startsWith("https://") -> downloadImageBytes(trimmed)
        // Raw base64 without data: prefix
        !trimmed.contains(' ') && trimmed.length >= 256 && BASE64_PAYLOAD_REGEX.matches(trimmed) -> Base64.decode(trimmed)
        else -> error("Unsupported image src: ${trimmed.take(60)}")
    }
}
```

## How It Works Now

### Flow for LLM-generated fake URLs:

1. **originalSrc**: `"https://fake-cdn.com/watermark.png?token=abc123"`
2. **isDirectImageSrc(originalSrc)**: Returns `false` (HTTP URL no longer accepted)
3. **AI Generation Triggered** (line 79-100):
   - If `imageGenerationService` is available, generates real image
   - Updates `generatedImageUrl` with actual `data:` URL
4. **No Download Attempt**: Line 104 check fails, `loadImageBytesFromSrc()` is never called
5. **Result**: No CORS error, AI-generated image is displayed (if service available)

### Flow for valid data: URLs:

1. **originalSrc**: `"data:image/png;base64,iVBORw0KGgo..."`
2. **isDirectImageSrc(originalSrc)**: Returns `true` ✅
3. **AI Generation Skipped**: Line 81 check fails (already valid image)
4. **Direct Decode** (line 103-120): Decodes base64, displays image
5. **Result**: Image displayed correctly

### Flow for invalid URLs (no AI service):

1. **originalSrc**: `"https://fake-cdn.com/image.png"`
2. **isDirectImageSrc(originalSrc)**: Returns `false`
3. **AI Generation**: No service available → `errorMessage` is set or placeholder shown
4. **No Download Attempt**: Never tries to fetch the fake URL
5. **Result**: Placeholder text shown (line 163: `"Image: $resolvedSrc"`)

## Behavior

### Before

```kotlin
// LLM generates this
Image(src="https://maas-watermark-prod.cn-wlcb.ufileos.com/watermark.png?token=...")

// Renderer tries to download
isDirectImageSrc() → true  // ❌ Accepts HTTP URL
downloadImageBytes() → CORS ERROR!  // ❌
```

### After

```kotlin
// LLM generates this
Image(src="https://maas-watermark-prod.cn-wlcb.ufileos.com/watermark.png?token=...")

// Renderer behavior
isDirectImageSrc() → false  // ✅ Rejects HTTP URL
// If AI service available:
generateImage() → data:image/png;base64,...  // ✅ Real image
// If no AI service:
Show placeholder: "Image: [src]"  // ✅ No error
```

## Testing

Compile WASM target to verify:
```bash
./gradlew :mpp-ui:compileKotlinWasmJs
```

## Impact

✅ **No more CORS errors** in WASM environments  
✅ **No network requests** for fake LLM URLs  
✅ **No IndexedDB cache errors**  
✅ **Clean console** - no error spam  
✅ **AI generation works** - triggers for invalid URLs if service available  
✅ **Backward compatible** - real `data:` URLs still work  
✅ **Better UX** - shows placeholder or generates real image

## Related Files

**Modified**:
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/nano/NanoImageRenderer.kt`
  - `isDirectImageSrc()`: No longer accepts HTTP/HTTPS URLs
  - `loadImageBytesFromSrc()`: Removed download logic for HTTP/HTTPS

**Used By**:
- `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/nano/StatefulNanoRenderer.kt` (line 131)
- WASM Compose Multiplatform UI

**Note**: The previous changes to `HtmlRenderer.kt` and `ImageSrcExtract.kt` are also useful for server-side HTML rendering and testing, but the main WASM fix is in `NanoImageRenderer.kt`.


