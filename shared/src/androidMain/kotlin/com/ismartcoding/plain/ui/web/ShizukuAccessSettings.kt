package com.ismartcoding.plain.ui.web

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.ShizukuHelper
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.PSwitch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import rikka.shizuku.Shizuku

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

internal enum class ShizukuStep(val hint: StringResource, val action: StringResource) {
    Install(Res.string.install_shizuku_hint, Res.string.download_shizuku),
    Start(Res.string.start_shizuku_hint, Res.string.open_shizuku),
    Authorize(Res.string.authorize_shizuku_hint, Res.string.authorize_plainapp),
    Manage(Res.string.authorize_in_manager_hint, Res.string.open_shizuku),
    Ready(Res.string.shizuku_ready, Res.string.done)
}

private fun currentStep(): ShizukuStep = when {
    ShizukuHelper.isGranted() -> ShizukuStep.Ready
    appContext.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) == null -> ShizukuStep.Install
    !ShizukuHelper.isAvailable() -> ShizukuStep.Start
    Shizuku.shouldShowRequestPermissionRationale() -> ShizukuStep.Manage
    else -> ShizukuStep.Authorize
}

@Composable
internal fun rememberShizukuStep(): ShizukuStep {
    var step by remember { mutableStateOf(currentStep()) }
    LifecycleResumeEffect(Unit) {
        val result = Shizuku.OnRequestPermissionResultListener { _, _ -> step = currentStep() }
        val received = Shizuku.OnBinderReceivedListener { step = currentStep() }
        val died = Shizuku.OnBinderDeadListener { step = currentStep() }
        Shizuku.addRequestPermissionResultListener(result)
        Shizuku.addBinderReceivedListener(received)
        Shizuku.addBinderDeadListener(died)
        step = currentStep()
        onPauseOrDispose {
            Shizuku.removeRequestPermissionResultListener(result)
            Shizuku.removeBinderReceivedListener(received)
            Shizuku.removeBinderDeadListener(died)
        }
    }
    return step
}

internal fun openShizuku() {
    val intent = appContext.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
    if (intent != null) appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    else DialogHelper.showMessage(Res.string.manage_shizuku)
}

@Composable
internal actual fun ShizukuAccessSettings(onSetup: () -> Unit) {
    val granted = rememberShizukuStep() == ShizukuStep.Ready
    val toggle = { if (granted) openShizuku() else onSetup() }
    PListItem(title = stringResource(Res.string.adb_wireless_shizuku), modifier = Modifier.clickable { toggle() }) {
        PSwitch(checked = granted, onCheckedChange = { toggle() })
    }
}
