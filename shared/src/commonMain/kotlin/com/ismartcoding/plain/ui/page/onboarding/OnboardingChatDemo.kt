package com.ismartcoding.plain.ui.page.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Share-folder message card, mirroring ChatShareItem. */
@Composable
internal fun ShareCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.folders),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(7.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = "Documents",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.folder_card_items, 24) + " · 1.2 GB",
                    fontSize = 7.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Chat message row, mirroring ChatListItem: name + time, accent bar for me. */
@Composable
internal fun ChatMessageRow(
    fromMe: Boolean,
    name: String,
    time: String,
    appear: Float,
    content: @Composable () -> Unit,
) {
    val a = ease(appear)
    if (a <= 0f) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(a)
            .graphicsLayer { translationY = (1f - a) * 10.dp.toPx() },
    ) {
        if (fromMe) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        } else {
            HorizontalSpace(dp = 4.dp)
        }
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 5.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = time,
                    fontSize = 7.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}
