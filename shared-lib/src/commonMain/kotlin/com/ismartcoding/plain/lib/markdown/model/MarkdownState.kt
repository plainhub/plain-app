package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalInspectionMode
import com.ismartcoding.plain.lib.markdown.utils.lookupLinkDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * A [MarkdownState] that executes the parsing of the markdown content with the [MarkdownParser] asynchronously.
 *
 * @param content The markdown content to parse.
 * @param lookupLinks Whether to lookup links in the parsed tree or not.
 * @param flavour The [MarkdownFlavourDescriptor] to use for parsing.
 * @param parser The [MarkdownParser] to use for parsing.
 * @param referenceLinkHandler The [ReferenceLinkHandler] to use for storing links.
 * @param immediate Whether to parse the content immediately or not. (WARNING: This is not advised, as it will block the composition!)
 * @return A [MarkdownState] instance that will parse the content and emit the result to its [MarkdownState.state] flow.
 */
@Composable
fun rememberMarkdownState(
    content: String,
    lookupLinks: Boolean = true,
    retainState: Boolean = false,
    flavour: MarkdownFlavourDescriptor = remember { GFMFlavourDescriptor() },
    parser: MarkdownParser = remember(flavour) { MarkdownParser(flavour) },
    referenceLinkHandler: ReferenceLinkHandler = remember { ReferenceLinkHandlerImpl() },
    immediate: Boolean = LocalInspectionMode.current,
): MarkdownState {
    val input = remember(content, lookupLinks, flavour, parser, referenceLinkHandler, retainState) {
        Input(
            content = content,
            lookupLinks = lookupLinks,
            flavour = flavour,
            parser = parser,
            referenceLinkHandler = referenceLinkHandler,
            retainState = retainState,
        )
    }

    val state = remember {
        MarkdownStateImpl(input).apply {
            if (immediate) {
                // In immediate mode, parse synchronously but be aware this blocks the UI thread
                parseBlocking()
            }
        }
    }

    // Capture the latest input without restarting the effect
    val currentInput by rememberUpdatedState(input)

    // Use snapshotFlow with conflate to prevent parse thrashing during rapid updates.
    // Without conflate, LaunchedEffect(input) cancels and restarts on every change,
    // causing nothing to render if updates arrive faster than parsing completes.
    // With conflate, parsing always completes and then picks up the latest value.
    LaunchedEffect(Unit) {
        snapshotFlow { currentInput }
            .conflate()
            .collect { newInput ->
                state.updateInput(newInput)
                state.parse()
            }
    }

    return state
}

/**
 * A [MarkdownState] that executes the parsing of the markdown content with the [MarkdownParser] asynchronously.
 * This version accepts a suspend function block that returns the markdown content, allowing for dynamic loading.
 *
 * @param keys Optional keys that determine when the block should be re-executed. When any key changes, the block will be called again.
 * @param block A suspend function that returns the markdown content to parse.
 * @param lookupLinks Whether to lookup links in the parsed tree or not.
 * @param retainState Whether to retain the state of the [MarkdownState] or not, when the input changes
 * @param flavour The [MarkdownFlavourDescriptor] to use for parsing.
 * @param parser The [MarkdownParser] to use for parsing.
 * @param referenceLinkHandler The [ReferenceLinkHandler] to use for storing links.
 * @return A [MarkdownState] instance that will parse the content and emit the result to its [MarkdownState.state] flow.
 */
@Composable
fun rememberMarkdownState(
    vararg keys: Any?,
    lookupLinks: Boolean = true,
    retainState: Boolean = false,
    flavour: MarkdownFlavourDescriptor = remember { GFMFlavourDescriptor() },
    parser: MarkdownParser = remember(flavour) { MarkdownParser(flavour) },
    referenceLinkHandler: ReferenceLinkHandler = remember { ReferenceLinkHandlerImpl() },
    block: suspend () -> String,
): MarkdownState {
    val input = remember(lookupLinks, flavour, parser, referenceLinkHandler, retainState) {
        Input(
            content = "",
            lookupLinks = lookupLinks,
            flavour = flavour,
            parser = parser,
            referenceLinkHandler = referenceLinkHandler,
            retainState = retainState,
        )
    }

    val state = remember {
        MarkdownStateImpl(input).apply {
            parseBlocking()
        }
    }

    val keysList = remember(*keys) { keys.toList() }
    val currentKeys by rememberUpdatedState(keysList)
    val currentBlock by rememberUpdatedState(block)

    LaunchedEffect(Unit) {
        snapshotFlow { currentKeys }
            .conflate()
            .collect {
                state.updateInput(
                    Input(
                        content = currentBlock(),
                        lookupLinks = lookupLinks,
                        flavour = flavour,
                        parser = parser,
                        referenceLinkHandler = referenceLinkHandler,
                        retainState = retainState,
                    )
                )
                state.parse()
            }
    }

    return state
}

/**
 * Interface for a state that handles the parsing of markdown content with the [MarkdownParser].
 */
@Stable
interface MarkdownState {
    /** The current state of the markdown parsing */
    val state: StateFlow<State>

    /** The links found in the markdown content */
    val links: StateFlow<Map<String, String?>>

    /**
     * Parses the markdown content asynchronously using the Default dispatcher.
     * When a result is available it will be emitted to the [state] flow.
     */
    suspend fun parse(): State
}

/**
 * Implementation of [MarkdownState] that executes the parsing of the markdown content with the [MarkdownParser] asynchronously.
 */
@Stable
internal class MarkdownStateImpl(
    private var input: Input,
) : MarkdownState {
    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(State.Loading(input.referenceLinkHandler))
    override val state: StateFlow<State> = stateFlow.asStateFlow()

    private val linkStateFlow: MutableStateFlow<Map<String, String?>> = MutableStateFlow(emptyMap())
    override val links: StateFlow<Map<String, String?>> = linkStateFlow.asStateFlow()

    /**
     * Updates the input for this markdown state.
     * This is used when loading content dynamically.
     *
     * @param newInput The new input to use for parsing.
     */
    internal fun updateInput(newInput: Input) {
        if (input == newInput) return // don't do anything for the same input
        input = newInput
        // if we already have content don't go back to loading if retain state is set to true
        if (!newInput.retainState) stateFlow.value = State.Loading(input.referenceLinkHandler)
    }

    /**
     * Sets an error state for this markdown state.
     * This is used when an error occurs during dynamic content loading.
     *
     * @param error The error that occurred.
     */
    internal fun setError(error: Throwable) {
        stateFlow.value = State.Error(error, input.referenceLinkHandler)
    }

    /**
     * Parses the markdown content asynchronously using the Default dispatcher.
     * When a result is available it will be emitted to the [state] flow.
     */
    override suspend fun parse(): State = withContext(Dispatchers.Default) {
        parseBlocking()
    }

    /**
     * Parses the markdown content synchronously.
     */
    internal fun parseBlocking(): State {
        return try {
            val parsedResult = input.parser.buildMarkdownTreeFromString(input.content)
            if (input.lookupLinks) {
                val links = mutableMapOf<String, String?>()
                lookupLinkDefinition(links, parsedResult, input.content, recursive = true)
                links.onEach { (key, value) -> input.referenceLinkHandler.store(key, value) }
                linkStateFlow.value = links
            }
            State.Success(parsedResult, input.content, input.lookupLinks, input.referenceLinkHandler)
        } catch (error: Throwable) {
            State.Error(error, input.referenceLinkHandler)
        }.also { result ->
            stateFlow.value = result
        }
    }
}

/**
 * Creates a [kotlinx.coroutines.flow.Flow] of [State] for use in non-composable contexts like view models.
 * As soon as the flow is collected, it will start parsing the content, and emit the state once ready.
 *
 * @param content The markdown content to parse.
 * @param lookupLinks Whether to lookup links in the parsed tree or not.
 * @param flavour The [MarkdownFlavourDescriptor] to use for parsing.
 * @param parser The [MarkdownParser] to use for parsing.
 * @param referenceLinkHandler The [ReferenceLinkHandler] to use for storing links.
 *
 * @return A [kotlinx.coroutines.flow.Flow] of [State] that will emit the parsing state.
 */
fun parseMarkdownFlow(
    content: String,
    lookupLinks: Boolean = true,
    flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    parser: MarkdownParser = MarkdownParser(flavour),
    referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
) = flow {
    emit(State.Loading(referenceLinkHandler))
    val markdownState = MarkdownStateImpl(
        Input(
            content = content,
            lookupLinks = lookupLinks,
            flavour = flavour,
            parser = parser,
            referenceLinkHandler = referenceLinkHandler,
        )
    )
    markdownState.parse()
    emitAll(markdownState.state)
}

/**
 * Parses the given markdown [content] synchronously and returns the resulting [State] directly.
 *
 * Unlike [parseMarkdownFlow], this does not expose any mutable Flow/StateFlow machinery. It performs the
 * parse on the calling thread and returns the final, immutable [State] (a [State.Success] on success, or a
 * [State.Error] if the underlying parse fails). This is useful to pre-parse content before the UI is shown
 * and hand the already-parsed [State.Success] to the `Markdown` composable.
 *
 * Note: parsing happens on the calling thread. For large documents consider invoking this off the main thread.
 *
 * @param content The markdown content to parse.
 * @param lookupLinks Whether to lookup links in the parsed tree or not.
 * @param flavour The [MarkdownFlavourDescriptor] to use for parsing.
 * @param parser The [MarkdownParser] to use for parsing.
 * @param referenceLinkHandler The [ReferenceLinkHandler] to use for storing links.
 *
 * @return The resulting [State]; [State.Success] on success or [State.Error] on failure.
 */
fun parseMarkdown(
    content: String,
    lookupLinks: Boolean = true,
    flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    parser: MarkdownParser = MarkdownParser(flavour),
    referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
): State {
    return MarkdownStateImpl(
        Input(
            content = content,
            lookupLinks = lookupLinks,
            flavour = flavour,
            parser = parser,
            referenceLinkHandler = referenceLinkHandler,
        )
    ).parseBlocking()
}

/**
 * Transforms a [Flow] of markdown content strings into a [Flow] of parsed [State] for use in non-composable contexts like view models.
 * As soon as the flow is collected, it will start parsing the content, and emit the state once ready.
 *
 * @param lookupLinks Whether to lookup links in the parsed tree or not
 * @param retainState Whether to retain the state of the [MarkdownState] or not, when the input changes
 * @param flavour The [MarkdownFlavourDescriptor] to use for parsing.
 * @param parser The [MarkdownParser] to use for parsing.
 * @param referenceLinkHandler The [ReferenceLinkHandler] to use for storing links.
 *
 * @return A [Flow] of [State] that represents the parsed markdown state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<String>.asMarkdownState(
    lookupLinks: Boolean = true,
    retainState: Boolean = false,
    flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    parser: MarkdownParser = MarkdownParser(flavour),
    referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
): Flow<State> {
    val markdownState = MarkdownStateImpl(
        Input(
            content = "",
            lookupLinks = lookupLinks,
            flavour = flavour,
            parser = parser,
            referenceLinkHandler = referenceLinkHandler,
            retainState = retainState,
        )
    )
    return transformLatest { content ->
        markdownState.updateInput(
            Input(
                content = content,
                lookupLinks = lookupLinks,
                flavour = flavour,
                parser = parser,
                referenceLinkHandler = referenceLinkHandler,
                retainState = retainState,
            )
        )

        // Emit the Loading state only when updateInput transitioned to it (i.e. not retaining).
        // A late subscriber whose content already parsed sees Success directly, avoiding a flash.
        (markdownState.state.value as? State.Loading)?.let { emit(it) }

        // parse() returns the resulting State (Success/Error) deterministically.
        emit(markdownState.parse())
    }
}

/**
 * The input for the [MarkdownState].
 *
 * @param content The markdown content to parse.
 * @param lookupLinks Whether to lookup links in the parsed tree or not.
 * @param flavour The [MarkdownFlavourDescriptor] to use for parsing.
 * @param parser The [MarkdownParser] to use for parsing.
 * @param referenceLinkHandler The [ReferenceLinkHandler] to use for storing links.
 * @param retainState Whether to retain the state of the [MarkdownState] or not, when the input changes
 */
@Immutable
data class Input(
    val content: String,
    val lookupLinks: Boolean,
    val flavour: MarkdownFlavourDescriptor,
    val parser: MarkdownParser,
    val referenceLinkHandler: ReferenceLinkHandler,
    val retainState: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Input

        if (lookupLinks != other.lookupLinks) return false
        if (content != other.content) return false
        if (flavour != other.flavour) return false
        if (parser != other.parser) return false
        if (referenceLinkHandler != other.referenceLinkHandler) return false
        if (retainState != other.retainState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lookupLinks.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + flavour.hashCode()
        result = 31 * result + parser.hashCode()
        result = 31 * result + referenceLinkHandler.hashCode()
        result = 31 * result + retainState.hashCode()
        return result
    }
}

/**
 * The current state of the [MarkdownState].
 */
@Immutable
sealed interface State {

    /** The [ReferenceLinkHandler] to store links in. */
    val referenceLinkHandler: ReferenceLinkHandler

    /** The parsing is in-progress. */
    @Immutable
    data class Loading(
        override val referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
    ) : State

    /** The parsing was successful. */
    @Immutable
    data class Success(
        val node: ASTNode,
        val content: String,
        val linksLookedUp: Boolean,
        override val referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
    ) : State

    /** The parsing failed due to [Throwable]. */
    @Immutable
    data class Error(
        val result: Throwable,
        override val referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
    ) : State
}
