@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSNumber
import platform.Foundation.valueForKey
import platform.Foundation.setValue
import platform.darwin.NSObject
import platform.objc.sel_registerName
import platform.posix.memcpy

/**
 * Helpers for calling AVPlayer / AVPlayerItem methods that the cinterop
 * binding does not expose directly. Uses `performSelector`, KVC
 * (`valueForKey`/`setValue`), and `NSInvocation` (for struct-returning
 * methods like `currentTime` / `duration` / `seekToTime:`).
 *
 * Shared by [AVPlayerAudioPlayer] and [AVPlayerVideoController].
 */

internal fun avPlayerRate(player: NSObject): Float =
    (player.valueForKey("rate") as? NSNumber)?.floatValue() ?: 0f

internal fun avPlayerSetRate(player: NSObject, rate: Float) {
    try {
        player.setValue(NSNumber(float = rate), forKey = "rate")
    } catch (e: Exception) {
        LogCat.e("avPlayerSetRate: ${e.message}")
    }
}

internal fun avPlayerSetMuted(player: NSObject, muted: Boolean) {
    try {
        player.setValue(NSNumber(bool = muted), forKey = "muted")
    } catch (e: Exception) {
        LogCat.e("avPlayerSetMuted: ${e.message}")
    }
}

internal fun avPlayerSeekToMs(target: NSObject, ms: Long) {
    try {
        val time = CMTimeMake(ms, 1000)
        val sel = sel_registerName("seekToTime:")
        val sigRaw = target.methodSignatureForSelector(sel) ?: return
        val sig = sigRaw as Any as NSMethodSignature
        val inv = NSInvocation.invocationWithMethodSignature(sig)
        inv.setTarget(target)
        inv.setSelector(sel)
        memScoped {
            val t = alloc<CMTime>()
            time.useContents {
                t.value = value
                t.timescale = timescale
                t.flags = flags
                t.epoch = epoch
            }
            inv.setArgument(t.ptr, atIndex = 2L)
            inv.invoke()
        }
    } catch (e: Exception) {
        LogCat.e("avPlayerSeekToMs: ${e.message}")
    }
}

internal fun avPlayerTimeMs(target: NSObject, selName: String): Long {
    return try {
        val sel = sel_registerName(selName)
        val sigRaw = target.methodSignatureForSelector(sel) ?: return 0L
        val sig = sigRaw as Any as NSMethodSignature
        val inv = NSInvocation.invocationWithMethodSignature(sig)
        inv.setTarget(target)
        inv.setSelector(sel)
        inv.invoke()
        val size = sig.methodReturnLength.toInt().coerceAtLeast(24)
        val buf = ByteArray(size)
        buf.usePinned { pinned ->
            inv.getReturnValue(pinned.addressOf(0))
        }
        memScoped {
            val t = alloc<CMTime>()
            buf.usePinned { pinned ->
                memcpy(t.ptr, pinned.addressOf(0), size.toULong())
            }
            if (t.timescale == 0) return@memScoped 0L
            val seconds = t.value.toDouble() / t.timescale.toDouble()
            if (seconds.isNaN() || seconds.isInfinite() || seconds < 0.0) 0L else (seconds * 1000.0).toLong()
        }
    } catch (e: Exception) {
        LogCat.e("avPlayerTimeMs($selName) failed: ${e.message}")
        0L
    }
}

internal fun avPlayerPerform(target: NSObject, selectorName: String) {
    target.performSelector(sel_registerName(selectorName))
}

internal fun avPlayerPerformWithArg(target: NSObject, selectorName: String, arg: Any?) {
    if (arg == null) {
        target.performSelector(sel_registerName(selectorName))
    } else {
        target.performSelector(sel_registerName(selectorName), arg)
    }
}
