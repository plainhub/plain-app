package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.animateContentSize
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

// plain-desktop light theme (src/styles/_base.scss)
internal val DemoLoginBg = Color(0xFFFBF8FF) // --md-sys-color-background
private val DemoLoginCard = Color(0xFFE2E0F7) // --md-sys-color-surface-variant
private val DemoLoginPrimary = Color(0xFF3F51B5) // --md-sys-color-primary
private val DemoLoginOnSurface = Color(0xFF1A1B26)

// DemoChromeBg/DemoTrack/DemoDot/DemoUrlPill/DemoUrlText live in DemoPlayer.kt (internal).

/**
 * Unified browser mock: dark chrome (window dots + address pill) above an
 * inset rounded page area. All demo "videos" that show a browser use this.
 */
@Composable
internal fun DemoBrowserFrame(
    url: String,
    icon: DrawableResource? = null,
    iconTint: Color = Color.Unspecified,
    caret: Boolean = false,
    pageBackground: Color = MaterialTheme.colorScheme.background,
    pageContent: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DemoBrowserChrome(url = url, icon = icon, iconTint = iconTint, caret = caret)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pageBackground)
                    .animateContentSize()
                    .heightIn(min = 208.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                pageContent()
            }
        }
    }
}

/** Window dots + address pill; optional status icon and blinking caret while "typing". */
@Composable
private fun DemoBrowserChrome(
    url: String,
    icon: DrawableResource?,
    iconTint: Color,
    caret: Boolean,
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

/**
 * plain-desktop LoginView end frame: PlainApp title + card with the Log in
 * button (colors from src/styles/_base.scss light theme).
 */
@Composable
internal fun DemoLoginPage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 208.dp)
            .background(DemoLoginBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "PlainApp",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DemoLoginOnSurface,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DemoLoginCard)
                .padding(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(DemoLoginPrimary)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Log in",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
        }
    }
}
