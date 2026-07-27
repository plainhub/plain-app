package com.ismartcoding.plain.ui.page.imageeditor

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.models.ImageEditorTool

private data class ToolItem(
    val tool: ImageEditorTool,
    val icon: DrawableResource,
    val labelRes: StringResource,
)

private val TOOL_LIST_1 = listOf(
    ToolItem(ImageEditorTool.SELECT, Res.drawable.mouse_pointer, Res.string.image_editor_tool_select),
    ToolItem(ImageEditorTool.FREEHAND, Res.drawable.pen, Res.string.image_editor_tool_freehand),
    ToolItem(ImageEditorTool.ARROW, Res.drawable.arrow_up_right, Res.string.image_editor_tool_arrow),
    ToolItem(ImageEditorTool.RECT, Res.drawable.square, Res.string.image_editor_tool_rect),
    ToolItem(ImageEditorTool.ELLIPSE, Res.drawable.circle, Res.string.image_editor_tool_ellipse),
)

private val TOOL_LIST_2 = listOf(
    ToolItem(ImageEditorTool.HIGHLIGHT, Res.drawable.highlighter, Res.string.image_editor_tool_highlight),
    ToolItem(ImageEditorTool.MOSAIC, Res.drawable.grid_3x3, Res.string.image_editor_tool_mosaic),
    ToolItem(ImageEditorTool.TEXT, Res.drawable.type, Res.string.image_editor_tool_text),
    ToolItem(ImageEditorTool.STICKER, Res.drawable.sticky_note, Res.string.image_editor_tool_sticker),
)

@Composable
fun ImageEditorBottomAppBar(
    level: Int,
    currentTool: ImageEditorTool,
    onToolSelected: (ImageEditorTool) -> Unit,
    onToggleLevel: () -> Unit,
    onClear: () -> Unit,
    onLayerPanel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val toolList = if (level == 0) TOOL_LIST_1 else TOOL_LIST_2
                toolList.forEach { item ->
                    ToolButton(
                        item = item,
                        selected = currentTool == item.tool,
                        onClick = { onToolSelected(item.tool) },
                    )
                }
                if (level == 1) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    ActionButton(
                        icon = Res.drawable.trash_2,
                        label = stringResource(Res.string.image_editor_delete_layer),
                        enabled = true,
                        onClick = onClear,
                    )
                    ActionButton(
                        icon = Res.drawable.layers,
                        label = stringResource(Res.string.image_editor_layers),
                        enabled = true,
                        onClick = onLayerPanel,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.IconButton(onClick = onToggleLevel) {
                    Icon(
                        painter = painterResource(
                            if (level == 0) Res.drawable.looks_one else Res.drawable.looks_two
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    item: ToolItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .width(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = stringResource(item.labelRes),
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionButton(
    icon: DrawableResource,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Column(
        modifier = Modifier
            .width(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}
