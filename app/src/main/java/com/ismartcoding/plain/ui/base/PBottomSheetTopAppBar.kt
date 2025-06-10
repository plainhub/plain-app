@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PBottomSheetTopAppBar(
    title: String = "",
    titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
        )
    },
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            titleContent()
        }
        HorizontalSpace(16.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
        }
    }
}