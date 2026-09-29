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
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.arrow_up_right as ui_drawable_arrow_up_right
import com.ismartcoding.plain.ui.resources.circle as ui_drawable_circle
import com.ismartcoding.plain.ui.resources.grid_3x3 as ui_drawable_grid_3x3
import com.ismartcoding.plain.ui.resources.highlighter as ui_drawable_highlighter
import com.ismartcoding.plain.ui.resources.layers as ui_drawable_layers
import com.ismartcoding.plain.ui.resources.looks_one as ui_drawable_looks_one
import com.ismartcoding.plain.ui.resources.looks_two as ui_drawable_looks_two
import com.ismartcoding.plain.ui.resources.mouse_pointer as ui_drawable_mouse_pointer
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.square as ui_drawable_square
import com.ismartcoding.plain.ui.resources.sticky_note as ui_drawable_sticky_note
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import com.ismartcoding.plain.ui.resources.type as ui_drawable_type
import com.ismartcoding.plain.i18n.type

private data class ToolItem(
    val tool: ImageEditorTool,
    val icon: DrawableResource,
    val labelRes: StringResource,
)

private val TOOL_LIST_1 = listOf(
    ToolItem(ImageEditorTool.SELECT, UiRes.drawable.ui_drawable_mouse_pointer, Res.string.image_editor_tool_select),
    ToolItem(ImageEditorTool.FREEHAND, UiRes.drawable.ui_drawable_pen, Res.string.image_editor_tool_freehand),
    ToolItem(ImageEditorTool.ARROW, UiRes.drawable.ui_drawable_arrow_up_right, Res.string.image_editor_tool_arrow),
    ToolItem(ImageEditorTool.RECT, UiRes.drawable.ui_drawable_square, Res.string.image_editor_tool_rect),
    ToolItem(ImageEditorTool.ELLIPSE, UiRes.drawable.ui_drawable_circle, Res.string.image_editor_tool_ellipse),
)

private val TOOL_LIST_2 = listOf(
    ToolItem(ImageEditorTool.HIGHLIGHT, UiRes.drawable.ui_drawable_highlighter, Res.string.image_editor_tool_highlight),
    ToolItem(ImageEditorTool.MOSAIC, UiRes.drawable.ui_drawable_grid_3x3, Res.string.image_editor_tool_mosaic),
    ToolItem(ImageEditorTool.TEXT, UiRes.drawable.ui_drawable_type, Res.string.image_editor_tool_text),
    ToolItem(ImageEditorTool.STICKER, UiRes.drawable.ui_drawable_sticky_note, Res.string.image_editor_tool_sticker),
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
                        icon = UiRes.drawable.ui_drawable_trash_2,
                        label = stringResource(Res.string.image_editor_delete_layer),
                        enabled = true,
                        onClick = onClear,
                    )
                    ActionButton(
                        icon = UiRes.drawable.ui_drawable_layers,
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
                            if (level == 0) UiRes.drawable.ui_drawable_looks_one else UiRes.drawable.ui_drawable_looks_two
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
