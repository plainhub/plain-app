package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.VerticalSpace

@Composable
fun NotificationFilterModeCard(
    mode: String,
    onToggleMode: () -> Unit,
) {
    PCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.notification_filter_mode_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VerticalSpace(dp = 12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = mode == "allowlist",
                    onClick = { if (mode != "allowlist") onToggleMode() },
                    label = { Text(stringResource(R.string.allowlist_mode)) },
                    colors = chipColors
                )
                FilterChip(
                    selected = mode == "blacklist",
                    onClick = { if (mode != "blacklist") onToggleMode() },
                    label = { Text(stringResource(R.string.blacklist_mode)) },
                    colors = chipColors
                )
            }
        }
    }
}
