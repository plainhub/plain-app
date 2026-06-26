package com.ismartcoding.plain.lib.channel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ismartcoding.plain.lib.channel.Channel.internalScope
import com.ismartcoding.plain.lib.channel.Channel.sharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object Channel {
    var sharedFlow = MutableSharedFlow<ChannelEvent>()
    internal val internalScope = ChannelScope()
}

fun sendEvent(event: ChannelEvent) =
    Channel.internalScope.launch {
        Channel.sharedFlow.emit(event)
    }

inline fun <reified T> LifecycleOwner.receiveEvent(
    lifeEvent: Lifecycle.Event = Lifecycle.Event.ON_DESTROY,
    noinline block: suspend CoroutineScope.(event: T) -> Unit,
): Job {
    return ChannelScope(this, lifeEvent).launch {
        Channel.sharedFlow.collect {
            if (it is T) {
                block(it)
            }
        }
    }
}

inline fun <reified T> LifecycleOwner.receiveEvent(): Flow<T> {
    return Channel.sharedFlow.filter { it is T }
        .map { it as T }
}

inline fun <reified T> receiveEventHandler(noinline block: suspend CoroutineScope.(event: T) -> Unit): Job {
    return ChannelScope().launch {
        Channel.sharedFlow.collect {
            if (it is T) {
                block(it)
            }
        }
    }
}
