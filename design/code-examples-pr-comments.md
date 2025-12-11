# PR Inline Comments 代码示例

本文档提供具体的代码示例，展示如何在现有的 mpp-ui 代码基础上实现 PR Inline Comments 功能。

## 1. 数据模型定义

### 文件: `mpp-core/src/commonMain/kotlin/cc/unitmesh/agent/github/PRReviewModels.kt`

```kotlin
package cc.unitmesh.agent.github

import kotlinx.serialization.Serializable

/**
 * Diff 侧边 (对应 GitHub API 的 side 字段)
 */
enum class DiffSide {
    LEFT,   // 旧版本 (删除的代码)
    RIGHT   // 新版本 (新增的代码)
}

/**
 * PR 评论位置
 */
@Serializable
data class PRCommentLocation(
    val filePath: String,
    val side: DiffSide,
    val lineNumber: Int,
    val isMultiLine: Boolean = false,
    val startLineNumber: Int? = null
)

/**
 * PR 评论
 */
@Serializable
data class PRComment(
    val id: String,
    val author: String,
    val avatarUrl: String? = null,
    val body: String,
    val createdAt: String,
    val location: PRCommentLocation,
    val isResolved: Boolean = false,
    val replies: List<PRComment> = emptyList()
)

/**
 * PR 评论线程
 */
@Serializable
data class PRCommentThread(
    val id: String,
    val location: PRCommentLocation,
    val comments: List<PRComment>,
    val isResolved: Boolean = false,
    val isCollapsed: Boolean = false
)
```

## 2. 修改现有的 DiffLineView

### 文件: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/diff/DiffCenterView.kt`

```kotlin
// 修改前的签名
@Composable
fun DiffLineView(line: DiffLine) {
    // ... 现有实现
}

// 修改后的签名 (向后兼容)
@Composable
fun DiffLineView(
    line: DiffLine,
    commentThreads: List<PRCommentThread> = emptyList(),
    onAddComment: ((Int) -> Unit)? = null,
    onReplyComment: ((String, String) -> Unit)? = null,
    onResolveThread: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 原有的行渲染逻辑
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            // Old line number
            Text(
                text = line.oldLineNumber?.toString() ?: "",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AutoDevColors.Diff.Dark.lineNumber,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.width(4.dp))

            // New line number
            Text(
                text = line.newLineNumber?.toString() ?: "",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = AutoDevColors.Diff.Dark.lineNumber,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Line prefix (+/-/ )
            Text(
                text = prefix,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = textColor,
                modifier = Modifier.width(12.dp)
            )

            // Line content
            Text(
                text = line.content,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // 新增: 评论指示器
            if (commentThreads.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = AutoDevComposeIcons.Comment,
                    contentDescription = "Has comments",
                    tint = AutoDevColors.Indigo.c600,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = commentThreads.size.toString(),
                    fontSize = 10.sp,
                    color = AutoDevColors.Indigo.c600,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
            
            // 新增: 添加评论按钮 (悬停时显示)
            if (onAddComment != null && line.newLineNumber != null) {
                IconButton(
                    onClick = { onAddComment(line.newLineNumber) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = AutoDevComposeIcons.Add,
                        contentDescription = "Add comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        // 新增: 渲染该行的所有评论线程
        if (commentThreads.isNotEmpty()) {
            commentThreads.forEach { thread ->
                InlinePRCommentThread(
                    thread = thread,
                    onReply = { body ->
                        onReplyComment?.invoke(thread.id, body)
                    },
                    onResolve = {
                        onResolveThread?.invoke(thread.id)
                    },
                    modifier = Modifier.padding(start = 80.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}
```

## 3. 创建内联评论组件

### 文件: `mpp-ui/src/commonMain/kotlin/cc/unitmesh/devins/ui/compose/agent/codereview/pr/InlinePRComment.kt`

```kotlin
package cc.unitmesh.devins.ui.compose.agent.codereview.pr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.unitmesh.agent.github.PRComment
import cc.unitmesh.agent.github.PRCommentThread
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons
import cc.unitmesh.devins.ui.compose.theme.AutoDevColors
import cc.unitmesh.devins.ui.compose.sketch.SketchRenderer

/**
 * 内联 PR 评论线程组件
 * 参考 InlineIssueChip 的设计模式
 */
@Composable
fun InlinePRCommentThread(
    thread: PRCommentThread,
    onReply: (String) -> Unit,
    onResolve: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCollapsed by remember { mutableStateOf(thread.isCollapsed) }
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (thread.isResolved)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 评论头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 作者头像 (使用首字母作为占位符)
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        color = AutoDevColors.Indigo.c600
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = thread.comments.first().author.first().uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = thread.comments.first().author,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = thread.comments.first().createdAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 已解决标记
                    if (thread.isResolved) {
                        Surface(
                            color = AutoDevColors.Green.c600.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AutoDevComposeIcons.CheckCircle,
                                    contentDescription = "Resolved",
                                    tint = AutoDevColors.Green.c600,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Resolved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AutoDevColors.Green.c600,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 折叠/展开按钮
                    IconButton(
                        onClick = { isCollapsed = !isCollapsed },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed)
                                AutoDevComposeIcons.ExpandMore
                            else
                                AutoDevComposeIcons.ExpandLess,
                            contentDescription = if (isCollapsed) "Expand" else "Collapse",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!isCollapsed) {
                Spacer(modifier = Modifier.height(8.dp))

                // 评论内容
                thread.comments.forEachIndexed { index, comment ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    PRCommentItem(comment)
                }

                // 回复输入框
                if (showReplyInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write a reply...") },
                        minLines = 2,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = {
                            showReplyInput = false
                            replyText = ""
                        }) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onReply(replyText)
                                    replyText = ""
                                    showReplyInput = false
                                }
                            },
                            enabled = replyText.isNotBlank()
                        ) {
                            Text("Reply")
                        }
                    }
                }

                // 操作按钮
                if (!thread.isResolved && !showReplyInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showReplyInput = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                AutoDevComposeIcons.Reply,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Reply")
                        }

                        Button(
                            onClick = onResolve,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                AutoDevComposeIcons.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Resolve")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个评论项
 */
@Composable
private fun PRCommentItem(comment: PRComment) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 如果是回复，显示作者和时间
        if (comment.replies.isNotEmpty() || comment != comment) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = AutoDevColors.Neutral.c600
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = comment.author.first().uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = comment.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 使用 SketchRenderer 渲染 Markdown 内容
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                SketchRenderer.RenderResponse(
                    content = comment.body,
                    isComplete = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
```

### 使用示例

```kotlin
// 在 DiffLineView 中使用
@Composable
fun DiffLineView(
    line: DiffLine,
    commentThreads: List<PRCommentThread> = emptyList(),
    onReplyComment: ((String, String) -> Unit)? = null,
    onResolveThread: ((String) -> Unit)? = null
) {
    Column {
        // 原有的行渲染
        Row { /* ... */ }

        // 渲染评论
        commentThreads.forEach { thread ->
            InlinePRCommentThread(
                thread = thread,
                onReply = { body ->
                    onReplyComment?.invoke(thread.id, body)
                },
                onResolve = {
                    onResolveThread?.invoke(thread.id)
                },
                modifier = Modifier.padding(start = 80.dp, end = 8.dp, top = 4.dp)
            )
        }
    }
}
```


