package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun PMiniButton(
    label: String,
    modifier: Modifier = Modifier,
    click: () -> Unit,
) {
    Button(
        click,
        modifier =
        modifier
            .height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
