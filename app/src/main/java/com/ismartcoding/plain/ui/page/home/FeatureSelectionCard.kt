package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.reorderable.ReorderableCollectionItemScope
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.circleBackground
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.secondaryTextColor

@Composable
internal fun ReorderableCollectionItemScope.EnabledFeatureCard(
    feature: FeatureItem,
    index: Int,
    onDisable: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardBackgroundNormal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.circleBackground)
                    .draggableHandle(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondaryTextColor,
                )
            }
            HorizontalSpace(12.dp)
            PIcon(
                icon = painterResource(feature.iconRes),
                contentDescription = stringResource(feature.titleRes),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            HorizontalSpace(12.dp)
            Text(
                text = stringResource(feature.titleRes),
                style = MaterialTheme.typography.listItemTitle(),
                modifier = Modifier.weight(1f),
            )
            HorizontalSpace(8.dp)
            PSwitch(activated = true, onClick = { onDisable() })
        }
    }
}

@Composable
internal fun DisabledFeatureCard(
    feature: FeatureItem,
    onEnable: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.cardBackgroundNormal),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.circleBackground.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            HorizontalSpace(12.dp)
            PIcon(
                icon = painterResource(feature.iconRes),
                contentDescription = stringResource(feature.titleRes),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            HorizontalSpace(12.dp)
            Text(
                text = stringResource(feature.titleRes),
                style = MaterialTheme.typography.listItemTitle(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            HorizontalSpace(8.dp)
            PSwitch(activated = false, onClick = { onEnable() })
        }
    }
}


