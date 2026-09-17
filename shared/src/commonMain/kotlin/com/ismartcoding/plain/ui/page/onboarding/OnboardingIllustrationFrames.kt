package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.orange
import org.jetbrains.compose.resources.painterResource

@Composable
private fun DashedLine(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .height(2.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
            },
    )
}

@Composable
internal fun DeviceLink(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.wifi),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "Wi-Fi",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        VerticalSpace(dp = 8.dp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(56.dp)) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            DashedLine(Modifier.weight(1f))
            Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

// ---------- Device frames ----------

/** Phone mockup frame (bezel + screen), sized by the caller. Screen
 *  background matches the app. Dark lifts the screen to
 *  surfaceContainerLowest so the screen reads as a panel distinct from the
 *  page background, which shares the app background color. Keep the height
 *  ≥ ~185dp so the bottom nav row stays fully inside the clip. */
@Composable
internal fun PhoneFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(3.dp, bezelColor(), RoundedCornerShape(16.dp))
            .background(if (isDarkTheme()) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

/** Desktop browser window: traffic lights + address bar, then page content. */
@Composable
internal fun BrowserFrame(
    modifier: Modifier = Modifier,
    height: Dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, bezelColor(), RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Titlebar as an overlay: lights pinned start, URL pill absolutely
        // centered — nested Row/weight layouts let it drift downward.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(if (isDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.inverseSurface),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.orange))
                Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.green))
            }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDarkTheme()) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.lock),
                    contentDescription = null,
                    tint = mockupMutedColor(),
                    modifier = Modifier.size(8.dp),
                )
                Text(
                    text = "192.168.1.20",
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    color = mockupMutedColor(),
                    maxLines = 1,
                )
            }
        }
        content()
    }
}
