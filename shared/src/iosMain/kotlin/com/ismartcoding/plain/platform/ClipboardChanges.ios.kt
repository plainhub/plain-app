package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.ClipboardReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.withContext
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardChangedNotification
import platform.UIKit.UIPasteboardRemovedNotification
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun observeClipboardChanges(report: suspend (ClipboardReportData) -> Unit): Unit =
    withContext(Dispatchers.Main.immediate) {
        val changes = Channel<Unit>(Channel.CONFLATED)
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(
            UIPasteboardChangedNotification,
            UIPasteboardRemovedNotification,
            UIApplicationDidBecomeActiveNotification,
        ).map { name ->
            center.addObserverForName(name, `object` = null, queue = null) { changes.trySend(Unit) }
        }
        try {
            changes.trySend(Unit)
            forwardClipboardChanges(
                changes.consumeAsFlow(),
                active = { UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive },
                revision = { UIPasteboard.generalPasteboard.changeCount },
                read = {
                    val pasteboard = UIPasteboard.generalPasteboard
                    if (pasteboard.containsPasteboardTypes(listOf(SENSITIVE_PASTEBOARD_TYPE))) null else pasteboard.string
                },
                report = report,
            )
        } finally {
            observers.forEach(center::removeObserver)
            changes.cancel()
        }
    }
