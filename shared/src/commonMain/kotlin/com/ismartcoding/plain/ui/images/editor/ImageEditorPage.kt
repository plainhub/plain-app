package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.generateId
import com.ismartcoding.plain.lib.yjs.Point
import com.ismartcoding.plain.lib.yjs.RectLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.lib.yjs.StickerLayer
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.NoDataView
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
    val selectedLayer = layers.find { it.id == vm.selectedLayerId.value }

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
    var showStyle by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showOrder by remember { mutableStateOf(false) }
    var toolGroup by remember { mutableStateOf<EditorToolGroup?>(null) }
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
        enabled = vm.loadState.value == EditorLoadState.Ready && textInsertPoint == null && !showStyle && !editText && !showExport && !showOrder && toolGroup == null && !showCanvasSettings && previewImage == null && !vm.showLayerPanel.value,
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

    fun selectTool(tool: ImageEditorTool) {
        cropRect = null
        vm.currentTool.value = tool
        vm.selectLayer(null)
        toolGroup = null
    }

    if (editText && selectedLayer != null) {
        val value = when (selectedLayer) { is TextLayer -> selectedLayer.text; is StickerLayer -> selectedLayer.text; else -> error("Layer has no text") }
        TextFieldDialog(
            title = stringResource(Res.string.image_editor_text_content), value = value, singleLine = false,
            placeholder = stringResource(Res.string.image_editor_text_content), onDismissRequest = { editText = false },
            onConfirm = { text ->
                vm.updateLayer(when (selectedLayer) { is TextLayer -> selectedLayer.copy(text = text); is StickerLayer -> selectedLayer.copy(text = text); else -> error("Layer has no text") })
                editText = false
            },
        )
    }
    if (vm.showLayerPanel.value) ImageEditorLayerPanel(vm, onClear = {
        vm.showLayerPanel.value = false
        scope.launch {
            confirmActionAsync(Res.string.image_editor_clear_all, Res.string.confirm_to_delete, danger = true, callback = vm::clearLayers)
        }
    }, onDismiss = { vm.showLayerPanel.value = false })
    if (showStyle) {
        if (selectedLayer != null) ImageEditorProperties(selectedLayer, vm::updateLayer) { showStyle = false }
        else ImageEditorToolStyle(vm) { showStyle = false }
    }
    if (showOrder && selectedLayer != null) ImageEditorLayerOrder(selectedLayer, vm) { showOrder = false }
    if (showExport) ImageEditorExportSheet(
        preview = { previewImage = imageActions.render(textMeasurer) },
        copy = { scope.launchSafe { imageActions.copy(textMeasurer); DialogHelper.showSuccess(Res.string.copied) } },
        export = { imageActions.export(it, textMeasurer) }, dismiss = { showExport = false },
    )
    toolGroup?.let { group ->
        ImageEditorTools(group, onTool = ::selectTool,
            onAddImage = { toolGroup = null; imageActions.pick(EditorImageTarget.Layer) },
            onCanvas = { toolGroup = null; showCanvasSettings = true },
            onPreview = { toolGroup = null; previewImage = imageActions.render(textMeasurer) },
            onDismiss = { toolGroup = null },
        )
    }

    if (showCanvasSettings) ImageEditorCanvasSettings(document, vm, { imageActions.pick(EditorImageTarget.Background) }, { showCanvasSettings = false })
    previewImage?.let { ImageEditorPreview(it) { previewImage = null } }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = ::handleBack,
                navigationIcon = { NavigationBackIcon { handleBack() } },
                title = stringResource(Res.string.image_editor),
                actions = {
                    PFilledButton(stringResource(Res.string.image_editor_export_short), { showExport = true }, enabled = vm.loadState.value == EditorLoadState.Ready)
                },
            )
        },
        bottomBar = {
            if (vm.loadState.value == EditorLoadState.Ready) {
                ImageEditorBottomAppBar(
                    vm = vm, layerCount = layers.size, selectedLayer = selectedLayer, onToolSelected = ::selectTool,
                    onGroup = { toolGroup = it }, onStyle = { showStyle = true }, onEditText = { editText = true },
                    onReplaceImage = { imageActions.pick(EditorImageTarget.Replacement(checkNotNull(selectedLayer).id)) },
                    onOrder = { showOrder = true },
                    cropActions = {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            PTextButton(stringResource(Res.string.cancel), { selectTool(ImageEditorTool.SELECT) })
                            PTextButton(stringResource(Res.string.image_editor_apply_crop), {
                                cropRect?.let { crop ->
                                    val image = cropEditorBitmap(imageActions.render(textMeasurer), crop)
                                    vm.applyCrop(embeddedEditorPng(image), image.width, image.height)
                                    cropRect = null
                                }
                            }, enabled = cropRect?.let { it.w >= 2 && it.h >= 2 } == true)
                        }
                    },
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
