package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.greenDot
import com.ismartcoding.plain.ui.theme.greenPill
import com.ismartcoding.plain.ui.theme.greenText
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** App home in the service-ON state: device-name top bar (TopBarHome) +
 *  service card (PlainAppServiceSection) + Desktop Access + Cast Receiver +
 *  bottom nav, mirroring the real home screen. Lays out natively at the
 *  104×200dp mockup; long single-line labels ellipsize. */
@Composable
internal fun AppHomeScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title group mirrors TopBarHome: pen rename icon follows the
            // device name; settings/scan are the far-right actions.
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pixel 9",
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HorizontalSpace(dp = 2.dp)
                Icon(
                    painter = painterResource(Res.drawable.pen),
                    contentDescription = null,
                    tint = mockupMutedColor(),
                    modifier = Modifier.size(6.dp),
                )
            }
            HorizontalSpace(dp = 5.dp)
            Icon(
                painter = painterResource(Res.drawable.settings),
                contentDescription = null,
                tint = mockupMutedColor(),
                modifier = Modifier.size(9.dp),
            )
            HorizontalSpace(dp = 5.dp)
            Icon(
                painter = painterResource(Res.drawable.scan_qr_code),
                contentDescription = null,
                tint = mockupMutedColor(),
                modifier = Modifier.size(9.dp),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(modifier = Modifier.padding(start = 7.dp, end = 7.dp, top = 5.dp, bottom = 6.dp)) {
                Text(
                    text = stringResource(Res.string.plainapp_service_on),
                    fontSize = 7.5.sp,
                    lineHeight = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                VerticalSpace(dp = 4.dp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniOutlinedButton(stringResource(Res.string.stay_online), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    MiniOutlinedButton(stringResource(Res.string.stop_service), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
            }
        }
        VerticalSpace(dp = 4.dp)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.cardBackgroundNormal,
        ) {
            Column(modifier = Modifier.padding(bottom = 1.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 7.dp, end = 7.dp, top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.devices),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp),
                    )
                    HorizontalSpace(dp = 4.dp)
                    Text(
                        text = stringResource(Res.string.desktop_access),
                        fontSize = 7.5.sp,
                        lineHeight = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    MiniSwitch(activated = true)
                }
                VerticalSpace(dp = 3.dp)
                Row(
                    modifier = Modifier
                        .padding(start = 7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.greenPill)
                        .padding(horizontal = 4.dp, vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.greenDot))
                    Text(
                        text = stringResource(Res.string.clients_online, 1),
                        fontSize = 5.5.sp,
                        lineHeight = 6.5.sp,
                        color = MaterialTheme.colorScheme.greenText,
                        maxLines = 1,
                    )
                }
                VerticalSpace(dp = 3.dp)
                Text(
                    text = "192.168.1.20:8080",
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp),
                )
                VerticalSpace(dp = 2.dp)
                Text(
                    text = stringResource(Res.string.try_more_addresses),
                    fontSize = 5.5.sp,
                    lineHeight = 6.5.sp,
                    color = mockupMutedColor(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                VerticalSpace(dp = 3.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.padding(horizontal = 2.dp).size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(Modifier.padding(horizontal = 2.dp).size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                }
                VerticalSpace(dp = 3.dp)
                HorizontalDivider(modifier = Modifier.padding(start = 7.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 7.dp, end = 5.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.access_settings),
                        fontSize = 7.5.sp,
                        lineHeight = 8.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(Res.drawable.chevron_right),
                        contentDescription = null,
                        tint = mockupMutedColor(),
                        modifier = Modifier.size(8.dp),
                    )
                }
            }
        }
        VerticalSpace(dp = 4.dp)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.cardBackgroundNormal,
        ) {
            Column(modifier = Modifier.padding(start = 7.dp, end = 7.dp, top = 5.dp, bottom = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.cast),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(8.dp),
                    )
                    HorizontalSpace(dp = 4.dp)
                    Text(
                        text = stringResource(Res.string.dlna_receiver),
                        fontSize = 7.5.sp,
                        lineHeight = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    MiniSwitch(activated = true)
                }
                VerticalSpace(dp = 2.dp)
                Text(
                    text = stringResource(Res.string.dlna_receiver_desc),
                    fontSize = 5.5.sp,
                    lineHeight = 6.5.sp,
                    color = mockupMutedColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavItem(Res.drawable.house, Res.string.home, selected = true)
            BottomNavItem(Res.drawable.message_circle, Res.string.chat, selected = false)
            BottomNavItem(Res.drawable.grid_3x3, Res.string.tools, selected = false)
        }
    }
}

/** Mini outlined button mirroring POutlinedButton (Stay Online / Stop Service). */
@Composable
private fun MiniOutlinedButton(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 6.sp, lineHeight = 7.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Mini switch mirroring PSwitch (checked: primary track, onPrimary knob). */
@Composable
private fun MiniSwitch(activated: Boolean) {
    Box(
        modifier = Modifier
            .width(14.dp)
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (activated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(if (activated) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 1.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(if (activated) MaterialTheme.colorScheme.onPrimary else Color.White),
        )
    }
}

@Composable
private fun BottomNavItem(icon: DrawableResource, label: StringResource, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else mockupMutedColor(),
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = stringResource(label),
            fontSize = 6.sp,
            lineHeight = 7.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else mockupMutedColor(),
            maxLines = 1,
        )
    }
}
