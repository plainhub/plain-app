package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.requiredHeightIn
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
        selected, onClick, label,
        // requiredHeightIn, not heightIn: containers like PScrollableTabRow size tabs from the
        // intrinsic height and then re-measure every tab at that fixed height. FilterChip enforces
        // its own minimum height through defaultMinSize, which is invisible to intrinsic queries,
        // so inside such a container the chip collapses to the label height and its corners look
        // rounder than in a plain Row.
        modifier.requiredHeightIn(min = FilterChipDefaults.Height),
        enabled,
        colors = FilterChipDefaults.filterChipColors().copy(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.cardBackgroundNormal,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
    )
}
