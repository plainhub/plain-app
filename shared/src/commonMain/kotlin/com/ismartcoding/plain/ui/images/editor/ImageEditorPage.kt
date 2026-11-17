package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ismartcoding.plain.ui.state.launchSafe
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.images.editor.ImageEditorEditViewModel
import com.ismartcoding.plain.ui.images.editor.ImageEditorTool
import com.ismartcoding.plain.ui.images.editor.scene.parseScopeBgColor
import com.ismartcoding.plain.lib.yjs.Point
import com.ismartcoding.plain.lib.yjs.RectLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.redo as ui_drawable_redo
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import com.ismartcoding.plain.ui.resources.undo as ui_drawable_undo
import com.ismartcoding.plain.i18n.undo
import com.ismartcoding.plain.i18n.redo

private val PRESET_COLORS = listOf(
    "#ff0000", "#00cc00", "#0066ff", "#ffcc00",
    "#ff8800", "#ff00ff", "#00cccc", "#000000",
)
private val PRESET_WIDTHS = listOf(2.0, 4.0, 8.0, 16.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorPage(
    navController: NavHostController,
    projectId: String,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    vm: ImageEditorEditViewModel = viewModel { ImageEditorEditViewModel(com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient(source.api)) },
) {
    val document by vm.document.collectAsState()
    val layers = document.layers

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val geometry = remember(textMeasurer) { EditorGeometry { text ->
        val layout = com.ismartcoding.plain.ui.images.editor.scene.measureSceneText(text, textMeasurer)
        (layout.lines.size * layout.lineHeight).toDouble()
    } }
    val imageActions = vm.imageActions
    ImageEditorFileEffects(imageActions)
    val scope = rememberCoroutineScope()
    var showCanvasSettings by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var showProperties by remember { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf<RectLayer?>(null) }
    var textInsertPoint by remember { mutableStateOf<Point?>(null) }

    LaunchedEffect(projectId) {
        vm.loadAsync(projectId)
    }

    fun handleBack() {
        if (vm.dirty.value) {
            vm.saveAsync(onDone = { navController.navigateUp() })
        } else {
            navController.navigateUp()
        }
    }

    com.ismartcoding.plain.ui.navigation.PBackHandler(
        enabled = vm.loadState.value == EditorLoadState.Ready && textInsertPoint == null && !showProperties && !showCanvasSettings && previewImage == null && !vm.showLayerPanel.value,
        onBack = ::handleBack,
    )

    textInsertPoint?.let { point ->
        TextFieldDialog(
            title = stringResource(Res.string.image_editor_tool_text),
            value = "",
            singleLine = false,
            placeholder = stringResource(Res.string.image_editor_tool_text),
            onDismissRequest = { textInsertPoint = null },
            onConfirm = { text ->
                vm.addLayer(
                    TextLayer(
                        id = generateId(),
                        x = point.x, y = point.y,
                        text = text,
                        fontSize = 48.0,
                        color = vm.color.value,
                        maxWidth = 200.0,
                    ),
                )
                textInsertPoint = null
            },
        )
    }

    if (vm.showLayerPanel.value) {
        ImageEditorLayerPanel(vm = vm, onDismiss = { vm.showLayerPanel.value = false })
    }

    val selectedLayer = layers.find { it.id == vm.selectedLayerId.value }
    if (showProperties && selectedLayer != null) ImageEditorProperties(selectedLayer, vm::updateLayer, replaceImage = { imageActions.pick(EditorImageTarget.Replacement(selectedLayer.id)) }, dismiss = { showProperties = false })

    if (showCanvasSettings) ImageEditorCanvasSettings(document, vm, { imageActions.pick(EditorImageTarget.Background) }, { showCanvasSettings = false })
    previewImage?.let { ImageEditorPreview(it) { previewImage = null } }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = ::handleBack,
                navigationIcon = { NavigationBackIcon { handleBack() } },
                title = stringResource(Res.string.image_editor),
                actions = {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_undo,
                        contentDescription = stringResource(Res.string.undo),
                        tint = MaterialTheme.colorScheme.onSurface,
                        enabled = vm.canUndo.value,
                    ) { vm.undo() }
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_redo,
                        contentDescription = stringResource(Res.string.redo),
                        tint = MaterialTheme.colorScheme.onSurface,
                        enabled = vm.canRedo.value,
                    ) { vm.redo() }
                    if (selectedLayer != null) com.ismartcoding.plain.ui.base.PTextButton(stringResource(Res.string.image_editor_properties), { showProperties = true })
                    ImageEditorMenu(
                        enabled = vm.loadState.value == EditorLoadState.Ready,
                        canvasSettings = { showCanvasSettings = true },
                        preview = { previewImage = imageActions.render(textMeasurer) },
                        copy = { scope.launchSafe { imageActions.copy(textMeasurer); com.ismartcoding.plain.ui.components.DialogHelper.showSuccess(Res.string.copied) } },
                        deleteProject = { scope.launchSafe {
                            com.ismartcoding.plain.ui.components.confirmActionAsync(
                                title = Res.string.confirm_to_delete,
                                message = Res.string.image_editor_delete_project_confirm,
                                callback = { vm.deleteProject { navController.navigateUp() } },
                                danger = true,
                            )
                        } },
                        export = { imageActions.export(it, textMeasurer) },
                    )
                    val sel = vm.selectedLayerId.value
                    if (sel != null) {
                        PIconButton(
                            icon = UiRes.drawable.ui_drawable_trash_2,
                            contentDescription = stringResource(Res.string.image_editor_delete_layer),
                            tint = MaterialTheme.colorScheme.onSurface,
                        ) { vm.deleteLayer(sel) }
                    }
                },
            )
        },
        bottomBar = {
            if (vm.loadState.value == EditorLoadState.Ready) Column {
                if (vm.currentTool.value == ImageEditorTool.CROP) Row {
                    com.ismartcoding.plain.ui.base.PTextButton(stringResource(Res.string.image_editor_apply_crop), {
                        cropRect?.let { crop ->
                            val image = cropEditorBitmap(imageActions.render(textMeasurer), crop)
                            vm.applyCrop(embeddedEditorPng(image), image.width, image.height)
                            cropRect = null
                        }
                    }, enabled = cropRect?.let { it.w >= 2 && it.h >= 2 } == true)
                    com.ismartcoding.plain.ui.base.PTextButton(stringResource(Res.string.cancel), { cropRect = null; vm.currentTool.value = ImageEditorTool.SELECT })
                }
                if (vm.currentTool.value in listOf(ImageEditorTool.FREEHAND, ImageEditorTool.ARROW, ImageEditorTool.RECT, ImageEditorTool.ELLIPSE, ImageEditorTool.HIGHLIGHT)) {
                    StylePickerRow(
                        currentColor = vm.color.value,
                        currentWidth = vm.lineWidth.value,
                        onColorSelected = { vm.color.value = it },
                        onWidthSelected = { vm.lineWidth.value = it },
                        showWidth = vm.currentTool.value != ImageEditorTool.HIGHLIGHT,
                    )
                }
                ImageEditorBottomAppBar(
                    currentTool = vm.currentTool.value,
                    onToolSelected = {
                        cropRect = null
                        vm.currentTool.value = it
                        vm.selectLayer(null)
                    },
                    onAddImage = { imageActions.pick(EditorImageTarget.Layer) },
                    onClear = { vm.clearLayers() },
                    onLayerPanel = { vm.showLayerPanel.value = true },
                )
            }
        },
    ) { paddingValues ->
        when (val loading = vm.loadState.value) {
            EditorLoadState.Loading -> { NoDataColumn(loading = true, modifier = Modifier.padding(paddingValues)); return@PScaffold }
            is EditorLoadState.Failed -> { NoDataView(modifier = Modifier.padding(paddingValues), message = loading.message, icon = null); return@PScaffold }
            EditorLoadState.Ready -> Unit
        }
        ImageEditorCanvas(vm, document, textMeasurer, geometry, cropRect, { cropRect = it }, { textInsertPoint = it }, Modifier.padding(paddingValues))
    }
}

@Composable
private fun StylePickerRow(
    currentColor: String,
    currentWidth: Double,
    onColorSelected: (String) -> Unit,
    onWidthSelected: (Double) -> Unit,
    showWidth: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        val styleLabel = stringResource(Res.string.image_editor_style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .semantics { contentDescription = styleLabel }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val colorLabel = stringResource(Res.string.image_editor_color)
            PRESET_COLORS.forEach { color ->
                val active = color.equals(currentColor, ignoreCase = true)
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .semantics { contentDescription = "$colorLabel $color"; selected = active }
                        .clickable { onColorSelected(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(parseScopeBgColor(color)))
                }
            }
            EditorColorField(stringResource(Res.string.image_editor_custom_color), currentColor, onColorSelected)
            if (showWidth) PRESET_WIDTHS.forEach { w ->
                val selected = w == currentWidth
                val containerColor = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                }
                val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .size(40.dp, 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(containerColor)
                        .clickable { onWidthSelected(w) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = w.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                    )
                }
            }
        }
    }
}
