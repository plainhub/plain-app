package com.ismartcoding.plain.ui.page.root.home

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeItemFlow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth(),
        maxItemsInEachRow = 3,
        content = content,
    )
}
