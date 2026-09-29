package com.ismartcoding.plain.ui.base

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun PFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected, onClick, label, modifier, enabled,
        colors = FilterChipDefaults.filterChipColors().copy(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor =  MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.cardBackgroundNormal,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
    )
}
