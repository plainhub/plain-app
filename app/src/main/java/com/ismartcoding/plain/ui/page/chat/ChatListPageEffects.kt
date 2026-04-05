package com.ismartcoding.plain.ui.page.chat

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.PeerGraphQLClient
import com.ismartcoding.plain.chat.discover.NearbyDiscoverManager
import com.ismartcoding.plain.ui.models.PeerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
internal fun ChatListPageEffects(
    peerVM: PeerViewModel, scope: CoroutineScope,
    isAppInForeground: MutableState<Boolean>, isPageVisible: MutableState<Boolean>,
    isScreenOn: MutableState<Boolean>,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { isAppInForeground.value = true; isPageVisible.value = true }
                Lifecycle.Event.ON_PAUSE -> { isAppInForeground.value = false; isPageVisible.value = false }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context) {
        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> isScreenOn.value = true
                    Intent.ACTION_SCREEN_OFF -> isScreenOn.value = false
                }
            }
        }
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) }
        context.registerReceiver(screenReceiver, filter)
        onDispose { context.unregisterReceiver(screenReceiver) }
    }

    LaunchedEffect(Unit) { peerVM.loadPeers() }

    LaunchedEffect(Unit) {
        while (isActive) {
            val peersSnapshot = peerVM.pairedPeers.toList()
            scope.launch(Dispatchers.IO) {
                peersSnapshot.forEach { peer ->
                    val key = ChatCacheManager.peerKeyCache[peer.id]
                    if (key != null) {
                        try {
                            if (peer.ip.isNotEmpty() && peer.port > 0) {
                                try {
                                    if (PeerGraphQLClient.ping(peer, peer.id)) peerVM.updatePeerLastActive(peer.id)
                                    else NearbyDiscoverManager.discoverSpecificDevice(peer.id, key)
                                } catch (e: Exception) { NearbyDiscoverManager.discoverSpecificDevice(peer.id, key) }
                            } else {
                                NearbyDiscoverManager.discoverSpecificDevice(peer.id, key)
                            }
                        } catch (e: Exception) { e.printStackTrace(); LogCat.e("Error discovering device ${peer.id}: ${e.message}") }
                    }
                }
            }
            val interval = if (isPageVisible.value && isAppInForeground.value && isScreenOn.value) 5000L else 15000L
            LogCat.d("Discovery interval: ${interval}ms (visible: ${isPageVisible.value}, foreground: ${isAppInForeground.value}, screenOn: ${isScreenOn.value})")
            delay(interval)
        }
    }
}
