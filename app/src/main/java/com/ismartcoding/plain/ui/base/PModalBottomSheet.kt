package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PModalBottomSheet(
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = 28.0.dp,
            topEnd = 28.0.dp,
            bottomEnd = 0.0.dp,
            bottomStart = 0.0.dp
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}