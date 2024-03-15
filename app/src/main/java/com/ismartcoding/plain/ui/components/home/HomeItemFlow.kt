package com.ismartcoding.plain.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemFlow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        maxItemsInEachRow = 3,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}
