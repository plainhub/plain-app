package com.ismartcoding.plain.ui.nav

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/**
 * Suspends a page's initial data load until the navigation enter transition
 * settles. Media queries typically land mid-animation, and applying the
 * results then triggers grid recompositions and thumbnail decode bursts on
 * the main thread, dropping frames during the slide-up present animation.
 * The page skeleton (top bar, permission gate, loading state) still composes
 * on the first frame; only the query start is deferred.
 */
class NavLoadGate private constructor(initiallyOpen: Boolean) {
    private val opened = MutableStateFlow(initiallyOpen)

    val isOpen: Boolean
        get() = opened.value

    fun open() {
        opened.value = true
    }

    suspend fun await() {
        opened.first { it }
    }

    companion object {
        /** A gate that never blocks — default for callers outside navigation. */
        fun opened(): NavLoadGate = NavLoadGate(initiallyOpen = true)

        fun closed(): NavLoadGate = NavLoadGate(initiallyOpen = false)
    }
}

/**
 * Gate tied to this destination's enter transition: opens once the
 * AnimatedContent transition reaches its target state (animation finished
 * or skipped). Restored destinations (config change / process death)
 * compose with the transition already settled, so the gate opens on the
 * first check.
 */
@Composable
fun AnimatedContentScope.rememberNavLoadGate(): NavLoadGate {
    val gate = remember { NavLoadGate.closed() }
    LaunchedEffect(gate) {
        snapshotFlow { transition.currentState == transition.targetState }
            .first { it }
        gate.open()
    }
    return gate
}
