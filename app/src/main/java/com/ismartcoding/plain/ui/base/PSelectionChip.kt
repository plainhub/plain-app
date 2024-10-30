package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.cardContainer

@Composable
fun PSelectionChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected, onClick,
        label = {
            Text(text = text)
        },
        modifier, enabled,
        leadingIcon = if (selected) {
            {
                Icon(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp),
                    painter = painterResource(R.drawable.check),
                    contentDescription = stringResource(R.string.select),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors().copy(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.cardContainer(),
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
    )
}
