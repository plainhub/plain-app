package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.lifecycle.Lifecycle
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.hasPermission

/**
 * Single source of the storage-permission transition rule shared by every
 * media/files page.
 *
 * All-files-access is granted on the system settings screen, which the app
 * launches with NEW_TASK: on some Android versions (verified on 12) the
 * activity result is delivered immediately — before the user grants — while
 * on others it arrives only on return. No single signal is trustworthy, so
 * pages call [refreshStoragePermission] from every one they get
 * (PermissionsResultEvent collector, lifecycle ON_RESUME) and this rule
 * guarantees the page-visible state tracks the freshest reading while
 * [onGranted] runs exactly once per false → true transition.
 *
 * The [isGranted] overload exists so unit tests can script platform signal
 * orderings; production code uses the single-argument overload. Note that
 * page-entry loads (LaunchedEffect(Unit)) intentionally bypass this rule:
 * they must run whenever the permission is granted, even for shared view
 * models whose state is already true.
 */
fun refreshStoragePermission(
    hasPermission: MutableState<Boolean>,
    isGranted: () -> Boolean,
    onGranted: () -> Unit,
) {
    val granted = isGranted()
    val wasGranted = hasPermission.value
    hasPermission.value = granted
    if (granted && !wasGranted) {
        onGranted()
    }
}

fun refreshStoragePermission(
    hasPermission: MutableState<Boolean>,
    onGranted: () -> Unit,
) = refreshStoragePermission(hasPermission, { AppFeatureType.FILES.hasPermission() }, onGranted)

/** ON_RESUME re-check: the only reliable signal on versions that deliver the activity result early. */
@Composable
fun StoragePermissionResumeEffect(hasPermission: MutableState<Boolean>, onGranted: () -> Unit) {
    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
            refreshStoragePermission(hasPermission, onGranted)
        }
    }
}
