package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.grey
import com.ismartcoding.plain.ui.theme.orange
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Illustration "photo" palette: index-cycled pairs of (soft background, dot)
// built from theme colors so both light and dark themes stay readable.
@Composable
private fun DemoPhotoTile(
    index: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 7.dp,
    dotSize: Dp = 18.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val (bg, dot) = when (index % 6) {
        0 -> scheme.primary.copy(alpha = 0.15f) to scheme.primary
        1 -> scheme.orange.copy(alpha = 0.15f) to scheme.orange
        2 -> scheme.green.copy(alpha = 0.15f) to scheme.green
        3 -> scheme.tertiaryContainer to scheme.tertiary
        4 -> scheme.error.copy(alpha = 0.15f) to scheme.error
        else -> scheme.grey.copy(alpha = 0.15f) to scheme.grey
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dot.copy(alpha = 0.75f)),
        )
    }
}

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
private fun DeviceLink(modifier: Modifier = Modifier) {
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

@Composable
fun WelcomeIllustration() {
    val outline = MaterialTheme.colorScheme.onSurface
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .width(72.dp)
                .height(136.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(3.dp, outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 7.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            DemoPhotoTile(0, Modifier.fillMaxWidth().weight(1f), cornerRadius = 6.dp, dotSize = 12.dp)
            DemoPhotoTile(2, Modifier.fillMaxWidth().weight(1f), cornerRadius = 6.dp, dotSize = 12.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.music2),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        DeviceLink(Modifier.padding(horizontal = 8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                modifier = Modifier
                    .width(148.dp)
                    .height(104.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(3.dp, outline, RoundedCornerShape(10.dp))
                    .padding(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "PlainApp",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.green))
                        Text(
                            text = stringResource(Res.string.faq_https_demo_connected),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                VerticalSpace(dp = 4.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DemoPhotoTile(0, Modifier.weight(1f).height(26.dp), cornerRadius = 5.dp, dotSize = 10.dp)
                        DemoPhotoTile(1, Modifier.weight(1f).height(26.dp), cornerRadius = 5.dp, dotSize = 10.dp)
                        DemoPhotoTile(2, Modifier.weight(1f).height(26.dp), cornerRadius = 5.dp, dotSize = 10.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DemoPhotoTile(3, Modifier.weight(1f).height(26.dp), cornerRadius = 5.dp, dotSize = 10.dp)
                        DemoPhotoTile(4, Modifier.weight(1f).height(26.dp), cornerRadius = 5.dp, dotSize = 10.dp)
                        DemoPhotoTile(5, Modifier.weight(1f).height(26.dp), cornerRadius = 5.dp, dotSize = 10.dp)
                    }
                }
                VerticalSpace(dp = 4.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .width(160.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(outline),
            )
        }
    }
}

@Composable
fun DesktopAccessIllustration() {
    Column(
        modifier = Modifier
            .width(304.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.orange))
                Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.green))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    text = "192.168.1.20",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "PlainApp",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.green))
                Text(
                    text = stringResource(Res.string.faq_https_demo_connected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DemoPhotoTile(0, Modifier.weight(1f).aspectRatio(1f))
                DemoPhotoTile(1, Modifier.weight(1f).aspectRatio(1f))
                DemoPhotoTile(2, Modifier.weight(1f).aspectRatio(1f))
                DemoPhotoTile(3, Modifier.weight(1f).aspectRatio(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DemoPhotoTile(4, Modifier.weight(1f).aspectRatio(1f))
                DemoPhotoTile(5, Modifier.weight(1f).aspectRatio(1f))
                DemoPhotoTile(1, Modifier.weight(1f).aspectRatio(1f))
                DemoPhotoTile(0, Modifier.weight(1f).aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun BubbleIn(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BubbleOut(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun ShareHint(text: String, modifier: Modifier = Modifier) {
    val dashColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(50.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.share_2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ChatIllustration() {
    Column(
        modifier = Modifier
            .width(304.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BubbleIn(stringResource(Res.string.onboarding_3_bubble_in))
        Row(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.image),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "IMG_0913.jpg",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                VerticalSpace(dp = 2.dp)
                Text(
                    text = "3.2 MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.green),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        BubbleOut(stringResource(Res.string.onboarding_3_bubble_out), Modifier.align(Alignment.End))
        ShareHint(stringResource(Res.string.onboarding_3_share_hint), Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun ToolCard(
    modifier: Modifier,
    icon: DrawableResource,
    title: String,
    subtitle: String,
    hero: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (hero) scheme.primaryContainer else scheme.surface)
            .border(
                if (hero) 2.dp else 1.dp,
                if (hero) scheme.primary else scheme.outlineVariant,
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (hero) scheme.surface else scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        VerticalSpace(dp = 6.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        VerticalSpace(dp = 4.dp)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = if (hero) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            fontWeight = if (hero) FontWeight.SemiBold else null,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MediaToolsIllustration() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.image,
                title = stringResource(Res.string.onboarding_4_photo_title),
                subtitle = stringResource(Res.string.onboarding_4_photo_desc),
                hero = false,
            )
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.music2,
                title = stringResource(Res.string.onboarding_4_music_title),
                subtitle = stringResource(Res.string.onboarding_4_music_desc),
                hero = true,
            )
        }
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.notebook_pen,
                title = stringResource(Res.string.notes),
                subtitle = stringResource(Res.string.onboarding_4_notes_desc),
                hero = false,
            )
            ToolCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Res.drawable.scan_qr_code,
                title = stringResource(Res.string.tools),
                subtitle = stringResource(Res.string.onboarding_4_tools_desc),
                hero = false,
            )
        }
    }
}
