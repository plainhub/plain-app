package com.ismartcoding.plain.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import com.ismartcoding.plain.ui.components.rememberLifecycleEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun RemotePermissionEffects(
    source: ContentSource.Remote,
    permission: Permission,
    hasPermission: MutableState<Boolean>,
    reload: (suspend () -> Unit)? = null,
) {
    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(source.client, permission, lifecycleEvent) {
        if (lifecycleEvent != Lifecycle.Event.ON_RESUME) return@LaunchedEffect
        try {
            coroutineScope {
                launch {
                    source.client.permissions.map { permission in it }.distinctUntilChanged().collect { granted ->
                        hasPermission.value = granted
                        if (granted) reload?.invoke()
                    }
                }
                while (isActive) {
                    delay(2_000)
                    source.client.refreshPermissions()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DialogHelper.showMessage(error)
        }
    }
}
