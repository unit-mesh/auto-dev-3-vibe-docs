# iOS Podfile 自动化配置

## 问题背景

在 iOS 开发中使用 CocoaPods 时,每次运行 `pod install` 都会重新生成配置文件,导致手动添加的编译参数丢失。

### 丢失的配置示例

```xcconfig
OTHER_LDFLAGS = $(inherited) -ObjC -l"c++" -framework "AutoDevUI" -lsqlite3
FRAMEWORK_SEARCH_PATHS = $(inherited) "${PODS_ROOT}/../../mpp-core/build/bin/iosSimulatorArm64/debugFramework" ...
```

这些配置对于 Kotlin Multiplatform 项目至关重要,因为:
- `OTHER_LDFLAGS` 包含链接 Kotlin/Native framework 所需的标志
- `FRAMEWORK_SEARCH_PATHS` 指定 framework 的位置

## 解决方案

✅ **在 Podfile 的 `post_install` hook 中自动配置这些参数**

这样每次 `pod install` 时,CocoaPods 会自动应用配置,无需手动修改。

## 实现

### 1. Podfile 配置

在 `mpp-ios/Podfile` 中添加:

```ruby
post_install do |installer|
  # 配置 Pods 项目的基本设置
  installer.pods_project.targets.each do |target|
    target.build_configurations.each do |config|
      config.build_settings['ENABLE_BITCODE'] = 'NO'
      config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '14.0'
      config.build_settings['SWIFT_OPTIMIZATION_LEVEL'] = '-Onone' if config.name == 'Debug'
    end
  end
  
  # 配置主应用 target 的链接器标志和框架搜索路径
  installer.pods_project.targets.each do |target|
    if target.name == 'Pods-AutoDevApp'
      target.build_configurations.each do |config|
        # 检测架构和配置
        arch = 'iosSimulatorArm64'  # 默认 Apple Silicon 模拟器
        build_config = config.name.downcase.include?('debug') ? 'debug' : 'release'
        
        # 添加框架搜索路径
        framework_paths = [
          "$(inherited)",
          "\"${PODS_ROOT}/../../mpp-core/build/bin/#{arch}/#{build_config}Framework\"",
          "\"${PODS_ROOT}/../../mpp-ui/build/bin/#{arch}/#{build_config}Framework\""
        ]
        
        config.build_settings['FRAMEWORK_SEARCH_PATHS'] = framework_paths
        
        # 添加链接器标志
        ldflags = [
          "$(inherited)",
          "-ObjC",
          "-lc++",
          "-framework AutoDevUI",
          "-lsqlite3"
        ]
        
        config.build_settings['OTHER_LDFLAGS'] = ldflags
      end
    end
  end
end
```

### 2. 自动配置的参数

#### FRAMEWORK_SEARCH_PATHS

告诉 Xcode 在哪里找到 Kotlin 编译的 framework:

```
$(inherited)
"${PODS_ROOT}/../../mpp-core/build/bin/iosSimulatorArm64/debugFramework"
"${PODS_ROOT}/../../mpp-ui/build/bin/iosSimulatorArm64/debugFramework"
```

#### OTHER_LDFLAGS

链接器标志,用于正确链接 Kotlin framework:

| 标志 | 说明 |
|------|------|
| `$(inherited)` | 继承现有设置 |
| `-ObjC` | 加载所有 Objective-C 类和分类 (Kotlin/Native 需要) |
| `-lc++` | 链接 C++ 标准库 (Kotlin/Native 依赖) |
| `-framework AutoDevUI` | 链接 AutoDevUI framework |
| `-lsqlite3` | 链接 SQLite 库 (SQLDelight 需要) |

#### ENABLE_BITCODE

```
ENABLE_BITCODE = NO
```

Kotlin/Native 不支持 Bitcode,必须禁用。

#### IPHONEOS_DEPLOYMENT_TARGET

```
IPHONEOS_DEPLOYMENT_TARGET = 14.0
```

设置 iOS 最低支持版本。

## 验证

### 1. 运行 pod install

```bash
cd mpp-ios
pod install
```

### 2. 检查生成的配置

```bash
cat Pods/Target\ Support\ Files/Pods-AutoDevApp/Pods-AutoDevApp.debug.xcconfig | grep -E "OTHER_LDFLAGS|FRAMEWORK_SEARCH_PATHS"
```

### 3. 预期输出

```
FRAMEWORK_SEARCH_PATHS = $(inherited) "${PODS_ROOT}/../../mpp-core/build/bin/iosSimulatorArm64/debugFramework" "${PODS_ROOT}/../../mpp-ui/build/bin/iosSimulatorArm64/debugFramework"
OTHER_LDFLAGS = $(inherited) -ObjC -l"c++" -framework "AutoDevCore" -framework "AutoDevUI"
```

## 优势

| 优势 | 说明 |
|------|------|
| ✅ **自动化** | 每次 `pod install` 自动应用配置 |
| ✅ **版本控制** | 配置在 Podfile 中,可以提交到 Git |
| ✅ **团队协作** | 团队成员运行 `pod install` 即可获得正确配置 |
| ✅ **可维护** | 集中管理,易于修改和调试 |
| ✅ **一致性** | 确保所有开发者使用相同的配置 |

## 扩展

### 支持多架构

如果需要支持不同架构,可以动态检测:

```ruby
# 检测当前架构
arch = case `uname -m`.strip
  when 'arm64'
    'iosSimulatorArm64'  # Apple Silicon 模拟器
  when 'x86_64'
    'iosX64'             # Intel 模拟器
  else
    'iosArm64'           # 真机
end
```

### 添加更多链接器标志

在 `ldflags` 数组中添加:

```ruby
ldflags = [
  "$(inherited)",
  "-ObjC",
  "-lc++",
  "-framework AutoDevUI",
  "-lsqlite3",
  "-framework YourFramework",  # 添加新的 framework
  "-lYourLib"                   # 添加新的库
]
```

### 配置不同的 Build Configuration

```ruby
target.build_configurations.each do |config|
  if config.name == 'Debug'
    # Debug 特定配置
    config.build_settings['SWIFT_OPTIMIZATION_LEVEL'] = '-Onone'
  elsif config.name == 'Release'
    # Release 特定配置
    config.build_settings['SWIFT_OPTIMIZATION_LEVEL'] = '-O'
  end
end
```

## 常见问题

### Q: 为什么需要 `-ObjC` 标志?

**A**: Kotlin/Native 生成的 framework 包含 Objective-C 分类 (categories)。默认情况下,链接器不会加载只包含分类的目标文件。`-ObjC` 标志强制链接器加载所有 Objective-C 类和分类。

### Q: 为什么需要 `-lc++`?

**A**: Kotlin/Native 编译器生成的代码依赖 C++ 标准库 (`libc++`)。如果不链接,会出现符号未定义的错误。

### Q: 为什么需要 `-lsqlite3`?

**A**: SQLDelight Native Driver 使用 iOS 系统的 SQLite 库。需要显式链接 `libsqlite3`。

### Q: 如何调试链接错误?

**A**: 
1. 检查 `FRAMEWORK_SEARCH_PATHS` 是否正确
2. 确认 framework 已编译并存在于指定路径
3. 检查 `OTHER_LDFLAGS` 是否包含所有必要的标志
4. 查看 Xcode 的 Build Log 获取详细错误信息

### Q: 每次 pod install 都需要重新编译 framework 吗?

**A**: 不需要。Podspec 中的 `prepare_command` 会在 pod install 时自动编译 framework。但如果修改了 Kotlin 代码,需要手动重新编译或重新运行 `pod install`。

## 相关文件

- `mpp-ios/Podfile` - CocoaPods 配置文件
- `mpp-ios/PODFILE-CONFIG.md` - 详细配置说明
- `mpp-core/AutoDevCore.podspec` - Core framework 的 podspec
- `mpp-ui/AutoDevUI.podspec` - UI framework 的 podspec

## 参考资料

- [CocoaPods Podfile Syntax Reference](https://guides.cocoapods.org/syntax/podfile.html)
- [Xcode Build Settings Reference](https://developer.apple.com/documentation/xcode/build-settings-reference)
- [Kotlin/Native iOS Integration](https://kotlinlang.org/docs/native-ios-integration.html)
- [CocoaPods post_install Hook](https://guides.cocoapods.org/syntax/podfile.html#post_install)

## 总结

通过在 Podfile 的 `post_install` hook 中配置编译参数,我们实现了:

1. ✅ **自动化配置管理** - 无需手动修改 Xcode 项目
2. ✅ **版本控制友好** - 配置在 Podfile 中,可以提交到 Git
3. ✅ **团队协作便利** - 所有开发者获得一致的配置
4. ✅ **易于维护** - 集中管理所有编译参数

这是 Kotlin Multiplatform iOS 项目的最佳实践! 🎉

