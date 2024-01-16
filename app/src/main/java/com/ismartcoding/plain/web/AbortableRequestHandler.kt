package com.ismartcoding.plain.web

import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.concurrent.atomic.AtomicReference

fun ApplicationCall.invokeOnAbort(block: () -> Unit) {
    attributes.put(AbortableRequestHandler.ABORT_HANDLER_KEY, block)
}

class AbortableRequestHandler : ChannelInboundHandlerAdapter() {
    companion object {
        val ABORT_HANDLER_KEY = AttributeKey<() -> Unit>("abortFuture")
    }

    private val ref = AtomicReference<ApplicationCall?>()

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ApplicationCall) {
            ref.set(msg)
        }
        super.channelRead(ctx, msg)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        val call = ref.getAndSet(null)
        call?.attributes?.get(ABORT_HANDLER_KEY)?.invoke()
        super.channelInactive(ctx)
    }
}

