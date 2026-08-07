package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.MediaPreviewer
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState

private val SAMPLE_MARKDOWN = """
# Heading 1

## Heading 2

### Heading 3

#### Heading 4

This is a regular paragraph with some content. It demonstrates the unified markdown reading theme. The quick brown fox jumps over the lazy dog. Lorem ipsum dolor sit amet, consectetur adipiscing elit.

---

## Text Formatting

You can use **bold text**, *italic text*, and ***both***. There's also ~~strikethrough~~ and `inline code` formatting. superscript and subscript are also supported.

## Lists

### Unordered List
- First item
- Second item
  - Nested item
  - Another nested item
- Third item

### Ordered List
1. First step
2. Second step
3. Third step

### Task List
- [x] Completed task
- [x] Another completed task
- [ ] Pending task

## Code Blocks

```kotlin
fun main() {
    val greeting = "Hello, Markdown!"
    println(greeting)
}
```

```javascript
function fibonacci(n) {
    if (n <= 1) return n;
    return fibonacci(n - 1) + fibonacci(n - 2);
}
```

## Blockquotes

> "This is a blockquote. It's often used to highlight important information or quotes from other sources."
> - Source attribution

> Nested blockquote with multiple lines.
> Each line starts with a > character.

## Tables

| Feature | Description |
|---------|-------------|
| Headings | Clear hierarchy with proper spacing |
| Code | Dark background with monospace font |
| Quote | Muted text with left accent border |
| Table | Clean borders, comfortable rows |

| Feature | Description | Feature2 | Description2 |
|---------|-------------|---------|-------------|
| Headings | Clear hierarchy with proper spacing | Headings | Clear hierarchy with proper spacing |
| Code | Dark background with monospace font | Code | Dark background with monospace font |
| Quote | Muted text with left accent border | Quote | Muted text with left accent border |
| Table | Clean borders, comfortable rows | Table | Clean borders, comfortable rows |

## Links

[Visit PlainApp](https://plainapp.app) for more information.

## Images

![PlainApp Logo](https://plainapp.app/plainapp.svg)

## Math

Inline math: ${'$'}E = mc^2${'$'}

Block math:
${'$'}${'$'}
\int_0^1 x^2 dx = \frac{1}{3}
${'$'}${'$'}
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownThemePreviewPage(
    navController: NavHostController,
) {
    val previewerState = rememberPreviewerState()

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = "Markdown Preview",
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
        ) {
            item {
                VerticalSpace(dp = 8.dp)
            }
            item {
                MarkdownText(
                    text = SAMPLE_MARKDOWN,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    previewerState = previewerState,
                )
            }
        }
    }
    MediaPreviewer(state = previewerState)
}
