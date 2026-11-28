package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.ShizukuHelper
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.PSwitch
import org.jetbrains.compose.resources.stringResource
import rikka.shizuku.Shizuku

@Composable
internal actual fun ShizukuAccessSettings() {
    var granted by remember { mutableStateOf(ShizukuHelper.isGranted()) }
    val refresh = { granted = ShizukuHelper.isGranted() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }
    DisposableEffect(Unit) {
        val result = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }
        val received = Shizuku.OnBinderReceivedListener { refresh() }
        val died = Shizuku.OnBinderDeadListener { granted = false }
        Shizuku.addRequestPermissionResultListener(result)
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(died)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(result)
            Shizuku.removeBinderReceivedListener(received)
            Shizuku.removeBinderDeadListener(died)
        }
    }
    val toggle = {
        try {
            when {
                !ShizukuHelper.isAvailable() -> DialogHelper.showMessage(Res.string.start_shizuku)
                ShizukuHelper.isGranted() || Shizuku.shouldShowRequestPermissionRationale() -> {
                    val intent = appContext.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) appContext.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    else DialogHelper.showMessage(Res.string.manage_shizuku)
                }
                else -> ShizukuHelper.requestPermission()
            }
        } catch (error: Exception) {
            DialogHelper.showMessage(error)
        }
    }
    PListItem(title = stringResource(Res.string.adb_wireless_shizuku), modifier = Modifier.clickable { toggle() }) {
        PSwitch(checked = granted, onCheckedChange = { toggle() })
    }
}
