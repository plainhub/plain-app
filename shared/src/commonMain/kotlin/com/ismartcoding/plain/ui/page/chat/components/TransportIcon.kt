package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.ismartcoding.plain.chat.peer.transport.PeerTransportType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.theme.greenText
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.bluetooth as ui_drawable_bluetooth
import com.ismartcoding.plain.ui.resources.wifi as ui_drawable_wifi
import com.ismartcoding.plain.ui.resources.wifi_tethering as ui_drawable_wifi_tethering
import com.ismartcoding.plain.i18n.bluetooth

enum class TransportStyle {
    LAN {
        override val icon = UiRes.drawable.ui_drawable_wifi
    },
    AWARE {
        override val icon = UiRes.drawable.ui_drawable_wifi_tethering
    },
    BLE {
        override val icon = UiRes.drawable.ui_drawable_bluetooth
    };

    abstract val icon: DrawableResource

    companion object {
        fun from(type: PeerTransportType): TransportStyle = when (type) {
            PeerTransportType.LAN -> LAN
            PeerTransportType.AWARE -> AWARE
            PeerTransportType.BLE -> BLE
        }
    }
}

@Composable
fun TransportIcon(transportType: PeerTransportType, modifier: Modifier = Modifier) {
    val style = TransportStyle.from(transportType)

    val transition = rememberInfiniteTransition(label = "transport")
    val scale = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "transport_scale",
    )
    val alpha = transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "transport_alpha",
    )

    Icon(
        painter = painterResource(style.icon),
        contentDescription = transportType.name,
        tint = MaterialTheme.colorScheme.greenText,
        modifier = modifier
            .size(18.dp)
            .scale(scale.value)
            .alpha(alpha.value),
    )
}
