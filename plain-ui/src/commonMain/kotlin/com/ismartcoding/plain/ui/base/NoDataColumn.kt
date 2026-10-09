package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.loading as ui_string_loading
import com.ismartcoding.plain.ui.resources.no_data as ui_string_no_data
import com.ismartcoding.plain.ui.resources.no_results_found as ui_string_no_results_found
import com.ismartcoding.plain.ui.resources.searching as ui_string_searching
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoDataColumn(loading: Boolean = false, search: Boolean = false, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val text = if (search) {
            if (loading) UiRes.string.ui_string_searching else UiRes.string.ui_string_no_results_found
        } else {
            if (loading) UiRes.string.ui_string_loading else UiRes.string.ui_string_no_data
        }
        item {
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
