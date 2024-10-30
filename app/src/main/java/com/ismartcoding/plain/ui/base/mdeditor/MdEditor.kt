package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.measureTextWidth
import com.ismartcoding.plain.ui.helpers.MdEditorLineHelper
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun MdEditor(
    modifier: Modifier,
    mdEditorVM: MdEditorViewModel,
    scrollState: ScrollState,
    focusRequester: FocusRequester,
) {
    val lineNumberState = rememberScrollState()
    var lineCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.value }
            .debounce(10)
            .collectLatest { value ->
                lineNumberState.scrollTo(value)
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        val lineNumberWidth = if (mdEditorVM.showLineNumbers) measureTextWidth(" ${lineCount + 1} ", MaterialTheme.typography.bodyLarge) else 0.dp
        if (mdEditorVM.showLineNumbers) {
            Column(
                modifier = Modifier
                    .width(lineNumberWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(lineNumberState, enabled = false),
            ) {
                Text(
                    text = mdEditorVM.linesText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxHeight(),
                    textAlign = TextAlign.End,
                )
            }
        }
        Box(
            modifier = if (mdEditorVM.wrapContent) {
                Modifier
                    .padding(start = if (mdEditorVM.showLineNumbers) lineNumberWidth + 8.dp else PlainTheme.PAGE_HORIZONTAL_MARGIN, end = PlainTheme.PAGE_HORIZONTAL_MARGIN)
                    .fillMaxSize()
            } else {
                Modifier
                    .padding(if (mdEditorVM.showLineNumbers) lineNumberWidth + 8.dp else PlainTheme.PAGE_HORIZONTAL_MARGIN, end = PlainTheme.PAGE_HORIZONTAL_MARGIN)
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            }
        ) {
            BasicTextField(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                state = mdEditorVM.textFieldState,
                scrollState = scrollState,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { result ->
                    val r = result()
                    if (r != null) {
                        if (lineCount != r.lineCount) {
                            lineCount = r.lineCount
                            mdEditorVM.linesText = MdEditorLineHelper.getLinesText(
                                lineCount,
                                r,
                                mdEditorVM.textFieldState.text.toString(),
                            )
                        }
                    }
                })
        }
    }
}
