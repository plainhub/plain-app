package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PModalBottomSheet(
    modifier: Modifier = Modifier,
    topBarTitle: String = "",
    actions: (@Composable RowScope.() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ModalBottomSheet(
        windowInsets = WindowInsets(0, 0, 0, 0),
        dragHandle = null,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        TopAppBar(
            modifier = Modifier.height(56.dp),
            title = { Text(topBarTitle) },
            actions = { actions?.invoke(this) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            )
        )
        VerticalSpace(dp = 24.dp)
        content()
    }
}