package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.green

/**
 * Shared browser-chrome mock and loaded-page frame for the demo "videos".
 */

/** Window dots + address pill; optional status icon and blinking caret while "typing". */
@Composable
internal fun DemoBrowserChrome(
    url: String,
    icon: DrawableResource? = null,
    iconTint: Color = Color.Unspecified,
    caret: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) {
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(8.dp)
                    .background(DemoDot, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .background(DemoUrlPill, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = iconTint,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = url,
                color = DemoUrlText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
            if (caret) {
                val transition = rememberInfiniteTransition(label = "demoCaret")
                val alpha by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.15f,
                    animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
                    label = "caretAlpha",
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(DemoUrlText.copy(alpha = alpha)),
                )
            }
        }
    }
}

/** Miniature switch in the ON position, shared by the OS-setting mockups. */
@Composable
internal fun DemoSwitchOn() {
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(18.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .size(14.dp)
                .background(Color.White, CircleShape),
        )
    }
}

/** Successful end frame shared by the demos: app logo, name and a green "connected" row. */
@Composable
internal fun DemoLoadedPage(minHeight: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            Icon(
                painter = painterResource(Res.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                tint = Color.Unspecified,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.green, CircleShape),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.faq_https_demo_connected),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
