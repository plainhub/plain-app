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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import rikka.shizuku.Shizuku

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

internal enum class ShizukuStep(val title: StringResource, val hint: StringResource, val action: StringResource? = null) {
    Install(Res.string.shizuku_install_title, Res.string.install_shizuku_hint, Res.string.download_shizuku),
    Start(Res.string.shizuku_start_title, Res.string.start_shizuku_hint, Res.string.open_shizuku),
    Authorize(Res.string.authorize_plainapp, Res.string.authorize_shizuku_hint, Res.string.authorize_plainapp),
    Manage(Res.string.shizuku_manage_title, Res.string.authorize_in_manager_hint, Res.string.open_shizuku),
    Ready(Res.string.shizuku_enabled, Res.string.shizuku_ready)
}

internal fun currentShizukuStep(
    granted: () -> Boolean = ShizukuHelper::isGranted,
    installed: () -> Boolean = { appContext.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null },
    running: () -> Boolean = ShizukuHelper::isAvailable,
    needsManager: () -> Boolean = Shizuku::shouldShowRequestPermissionRationale,
): ShizukuStep = when {
    granted() -> ShizukuStep.Ready
    !installed() -> ShizukuStep.Install
    !running() -> ShizukuStep.Start
    needsManager() -> ShizukuStep.Manage
    else -> ShizukuStep.Authorize
}

@Composable
internal fun rememberShizukuStep(): ShizukuStep {
    var step by remember { mutableStateOf(currentShizukuStep()) }
    LifecycleResumeEffect(Unit) {
        val result = Shizuku.OnRequestPermissionResultListener { _, _ -> step = currentShizukuStep() }
        val received = Shizuku.OnBinderReceivedListener { step = currentShizukuStep() }
        val died = Shizuku.OnBinderDeadListener { step = currentShizukuStep() }
        Shizuku.addRequestPermissionResultListener(result)
        Shizuku.addBinderReceivedListener(received)
        Shizuku.addBinderDeadListener(died)
        step = currentShizukuStep()
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
    else DialogHelper.showMessage(Res.string.install_shizuku_hint)
}

@Composable
internal actual fun ShizukuAccessSettings(onSetup: () -> Unit) {
    val granted = rememberShizukuStep() == ShizukuStep.Ready
    PListItem(
        modifier = Modifier.clickable(onClick = onSetup),
        title = stringResource(Res.string.adb_wireless_shizuku),
        subtitle = stringResource(if (granted) Res.string.shizuku_enabled else Res.string.shizuku_disabled),
        showMore = true,
    )
}
