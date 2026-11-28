package com.ismartcoding.plain.services.screenmirror

import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Point
import android.os.IBinder
import android.os.Parcel
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.system.ShizukuHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

internal class ShizukuMirrorConnection {
    private val args = Shizuku.UserServiceArgs(ComponentName(appContext, ShizukuMirrorInput::class.java))
        .processNameSuffix("mirror_input").daemon(false)
    private var pending: CompletableDeferred<IBinder>? = null
    @Volatile private var binder: IBinder? = null
    private var closed = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (closed) return
            binder = service
            pending?.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binder = null
            pending?.completeExceptionally(IllegalStateException("Shizuku input service disconnected"))
            pending = null
        }
    }

    suspend fun dispatch(controls: String, size: Point) {
        val service = binder ?: withContext(Dispatchers.Main.immediate) {
            check(!closed)
            val bound = pending ?: CompletableDeferred<IBinder>().also {
                Shizuku.bindUserService(args, connection)
                pending = it
            }
            withTimeoutOrNull(10_000) { bound.await() } ?: error("Shizuku input service did not connect")
        }
        transact(service, IBinder.FIRST_CALL_TRANSACTION) {
            writeString(controls)
            writeInt(size.x)
            writeInt(size.y)
        }
    }

    fun reset() {
        binder?.let { transact(it, IBinder.FIRST_CALL_TRANSACTION + 1) {} }
    }

    fun close() {
        closed = true
        if (pending == null && binder == null) return
        pending?.cancel()
        pending = null
        binder = null
        if (ShizukuHelper.isGranted()) {
            try {
                Shizuku.unbindUserService(args, connection, true)
            } finally {
                Shizuku.unbindUserService(args, connection, false)
            }
        }
    }

    private inline fun transact(service: IBinder, code: Int, write: Parcel.() -> Unit) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.write()
            check(service.transact(code, data, reply, 0)) { "Shizuku rejected input transaction" }
            reply.readException()
        } finally {
            reply.recycle()
            data.recycle()
        }
    }
}
