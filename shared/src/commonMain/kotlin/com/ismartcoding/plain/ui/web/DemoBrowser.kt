package com.ismartcoding.plain.ui.web

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
internal val DemoLoginBg = Color(0xFFFBF8FF)
private val DemoLoginCard = Color(0xFFE2E0F7)
private val DemoLoginPrimary = Color(0xFF4F5F9E)
private val DemoLoginOnSurface = Color(0xFF1A1B26)

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
                    .clip(MaterialTheme.shapes.medium)
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
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
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
                .background(DemoUrlPill, CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .width(4.dp)
                        .height(14.dp)
                        .background(DemoUrlText.copy(alpha = alpha)),
                )
            }
        }
    }
}

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
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(DemoLoginCard)
                .padding(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(DemoLoginPrimary)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Log in",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}
