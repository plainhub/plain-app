package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.ismartcoding.plain.lib.markdown.utils.lookupLinkDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.EmptyStreamingMarkdownFile
import org.intellij.markdown.parser.StreamingMarkdownFile

@Composable
fun rememberStreamingMarkdownState(
    lookupLinks: Boolean = true,
    flavour: MarkdownFlavourDescriptor = remember { GFMFlavourDescriptor() },
    referenceLinkHandler: ReferenceLinkHandler = remember { ReferenceLinkHandlerImpl() },
): StreamingMarkdownState = remember(lookupLinks, flavour, referenceLinkHandler) {
    StreamingMarkdownStateImpl(
        lookupLinks = lookupLinks,
        flavour = flavour,
        referenceLinkHandler = referenceLinkHandler,
    )
}

/**
 * Collects append-only markdown chunks from this [Flow] into a remembered [StreamingMarkdownState].
 */
@Composable
fun Flow<String>.collectAsStreamingMarkdownState(
    lookupLinks: Boolean = true,
    flavour: MarkdownFlavourDescriptor = remember { GFMFlavourDescriptor() },
    referenceLinkHandler: ReferenceLinkHandler = remember { ReferenceLinkHandlerImpl() },
): StreamingMarkdownState {
    val markdownState = rememberStreamingMarkdownState(
        lookupLinks = lookupLinks,
        flavour = flavour,
        referenceLinkHandler = referenceLinkHandler,
    )

    LaunchedEffect(this, markdownState) {
        collect { chunk ->
            markdownState.append(chunk)
        }
    }

    return markdownState
}

@Stable
interface StreamingMarkdownState {
    /**
     * The accumulated markdown content.
     */
    val content: CharSequence

    /** The current state of the markdown parsing */
    val snapshot: StateFlow<Snapshot>

    /** The links found in the markdown content */
    val links: StateFlow<Map<String, String?>>

    /** The [ReferenceLinkHandler] to store links in. */
    val referenceLinkHandler: ReferenceLinkHandler

    /**
     * The only way to update this [StreamingMarkdownState].
     *
     * This method is append-only. Callers should append chunks in source order.
     */
    suspend fun append(chunk: String): Snapshot

    @Immutable
    data class Snapshot(
        val stableAst: List<ASTNode>,
        val unstableAstTail: List<ASTNode>,
    )
}

@Stable
internal class StreamingMarkdownStateImpl(
    private val lookupLinks: Boolean,
    private val flavour: MarkdownFlavourDescriptor,
    override val referenceLinkHandler: ReferenceLinkHandler,
) : StreamingMarkdownState {
    internal constructor(input: Input) : this(
        lookupLinks = input.lookupLinks,
        flavour = input.flavour,
        referenceLinkHandler = input.referenceLinkHandler,
    )

    override val content: StringBuilder = StringBuilder()
    private val snapshotStateFlow: MutableStateFlow<StreamingMarkdownState.Snapshot> = MutableStateFlow(
        StreamingMarkdownState.Snapshot(emptyList(), emptyList()),
    )
    private val linkStateFlow: MutableStateFlow<Map<String, String?>> = MutableStateFlow(emptyMap())
    private val stableLinks = mutableMapOf<String, String?>()
    private var processedStableChildren = 0
    private val appendMutex = Mutex()
    private val streamingFile: StreamingMarkdownFile = EmptyStreamingMarkdownFile(flavour)

    // FIXME: use explicit backing fields with Kotlin 2.4
    override val snapshot: StateFlow<StreamingMarkdownState.Snapshot>
        get() = snapshotStateFlow.asStateFlow()
    // FIXME: use explicit backing fields with Kotlin 2.4
    override val links: StateFlow<Map<String, String?>>
        get() = linkStateFlow.asStateFlow()

    override suspend fun append(chunk: String): StreamingMarkdownState.Snapshot = appendMutex.withLock {
        content.append(chunk)
        streamingFile.append(chunk)

        val stableChildren = streamingFile.stableChildren
        val unstableTail = streamingFile.unstableTail
        if (lookupLinks) {
            val links = lookupLinks(stableChildren, unstableTail)
            links.forEach { (key, value) -> referenceLinkHandler.store(key, value) }
            linkStateFlow.value = links
        }
        val nextSnapshot = StreamingMarkdownState.Snapshot(
            stableAst = stableChildren.toList(),
            unstableAstTail = unstableTail.toList(),
        )
        snapshotStateFlow.value = nextSnapshot
        snapshotStateFlow.value
    }

    private fun lookupLinks(
        stableChildren: List<ASTNode>,
        unstableTail: List<ASTNode>,
    ): Map<String, String?> {
        while (processedStableChildren < stableChildren.size) {
            lookupLinkDefinition(stableLinks, stableChildren[processedStableChildren], content, recursive = true)
            processedStableChildren += 1
        }
        return stableLinks.toMutableMap().also { links ->
            unstableTail.forEach { node ->
                lookupLinkDefinition(links, node, content, recursive = true)
            }
        }
    }
}
