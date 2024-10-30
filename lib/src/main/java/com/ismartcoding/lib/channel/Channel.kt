package com.ismartcoding.lib.channel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.Channel.internalScope
import com.ismartcoding.lib.channel.Channel.sharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object Channel {
    var sharedFlow = MutableSharedFlow<ChannelEvent>()
    internal val internalScope = ChannelScope()
}

fun sendEvent(event: ChannelEvent) =
    internalScope.launch {
        sharedFlow.emit(event)
    }

inline fun <reified T> LifecycleOwner.receiveEvent(
    lifeEvent: Lifecycle.Event = Lifecycle.Event.ON_DESTROY,
    noinline block: suspend CoroutineScope.(event: T) -> Unit,
): Job {
    return ChannelScope(this, lifeEvent).launch {
        sharedFlow.collect {
            if (it is T) {
                block(it)
            }
        }
    }
}

inline fun <reified T> LifecycleOwner.receiveEvent(): Flow<T> {
    return sharedFlow.filter { it is T }
        .map { it as T }
}

inline fun <reified T> LifecycleOwner.receiveEventLive(
    lifeEvent: Lifecycle.Event = Lifecycle.Event.ON_START,
    noinline block: suspend CoroutineScope.(event: T) -> Unit,
): Job {
    return lifecycleScope.launch {
        sharedFlow.flowWithLifecycle(lifecycle, lifeEvent.targetState).collect {
            if (it is T) {
                block(it)
            }
        }
    }
}

inline fun <reified T> receiveEventHandler(noinline block: suspend CoroutineScope.(event: T) -> Unit): Job {
    return ChannelScope().launch {
        sharedFlow.collect {
            if (it is T) {
                block(it)
            }
        }
    }
}
