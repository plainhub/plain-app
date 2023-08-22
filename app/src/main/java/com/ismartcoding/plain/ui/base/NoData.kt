package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun NoData() {
    Text(
        text = stringResource(id = R.string.no_data),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
