package com.ismartcoding.plain.lib

import com.ismartcoding.plain.lib.Channel.internalScope
import com.ismartcoding.plain.lib.Channel.sharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class ChannelEvent
class ChannelScope() : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main.immediate + SupervisorJob()
}

object Channel {
    var sharedFlow = MutableSharedFlow<ChannelEvent>()
    internal val internalScope = ChannelScope()
}

fun sendEvent(event: ChannelEvent) =
    internalScope.launch {
        sharedFlow.emit(event)
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
