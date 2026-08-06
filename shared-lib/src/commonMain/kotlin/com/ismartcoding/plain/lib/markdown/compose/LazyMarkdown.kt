package com.ismartcoding.plain.lib.markdown.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.markdown.compose.components.MarkdownComponents
import com.ismartcoding.plain.lib.markdown.model.State
import com.ismartcoding.plain.lib.markdown.model.StreamingMarkdownState

/**
 * Renders the parsed markdown content in a [LazyColumn].
 *
 * This function uses Compose's [LazyColumn] to implement virtualization, which means
 * only the visible portions of the document are rendered, improving performance for
 * large documents.
 *
 * @param state The success markdown state.
 * @param components The MarkdownComponents instance containing the components to use.
 * @param modifier The modifier to be applied to the container.
 * @param contentPadding The padding to be applied to the scrolling container.
 */
@Composable
fun LazyMarkdownSuccess(
    state: State.Success,
    components: MarkdownComponents,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    // Extract nodes for rendering
    val nodes = remember(state.node) { state.node.children }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        items(
            items = nodes,
            // Use the node's start offset as a key for stable item identity
            key = { node -> node.startOffset },
            // contentType lets the lazy layout reuse compositions across items
            // of the same markdown element type (e.g. paragraph after paragraph).
            contentType = { node -> node.type }
        ) { node ->
            MarkdownElement(node, components, state.content)
        }
    }
}

/**
 * Renders the parsed streaming markdown content in a [LazyColumn].
 */
@Composable
fun LazyMarkdownSuccess(
    streamingMarkdownState: StreamingMarkdownState,
    snapshot: StreamingMarkdownState.Snapshot,
    components: MarkdownComponents,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val content = streamingMarkdownState.content
    val nodes = remember(snapshot) { snapshot.stableAst + snapshot.unstableAstTail }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        items(
            items = nodes,
            key = { node -> node.startOffset },
            contentType = { node -> node.type }
        ) { node ->
            MarkdownElementInternal(node, components, content)
        }
    }
}
