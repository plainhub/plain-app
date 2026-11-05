package com.ismartcoding.plain.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.platform.system.AppFeatureType
import androidx.lifecycle.Lifecycle
import com.ismartcoding.plain.ui.components.rememberLifecycleEvent
import com.ismartcoding.plain.features.permissions.allGranted
import com.ismartcoding.plain.platform.permissions.FeaturePermission
import com.ismartcoding.plain.ui.components.refreshStoragePermission
import com.ismartcoding.plain.ui.navigation.NavLoadGate

@Composable
internal fun MediaPermissionEffects(
    hasPermission: MutableState<Boolean>,
    initialLoadGate: NavLoadGate?,
    reload: () -> Unit,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    feature: com.ismartcoding.plain.features.web.AccessFeature = com.ismartcoding.plain.features.web.AccessFeature.FILES,
    permission: FeaturePermission? = AppFeatureType.FILES.getPermission(),
) {
    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        com.ismartcoding.plain.ui.permissions.RemotePermissionEffects(source, feature, hasPermission) {
            initialLoadGate?.await()
            reload()
        }
        return
    }
    val granted = { permission?.let { allGranted(it.permissions) } ?: true }
    LaunchedEffect(permission) {
        hasPermission.value = granted()
        if (hasPermission.value) {
            initialLoadGate?.await()
            reload()
        }
    }
    LaunchedEffect(Channel.sharedFlow, permission) {
        Channel.sharedFlow.collect { event ->
            if (event is PermissionsResultEvent) refreshStoragePermission(hasPermission, granted, reload)
        }
    }
    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent, permission) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME) refreshStoragePermission(hasPermission, granted, reload)
    }
}
