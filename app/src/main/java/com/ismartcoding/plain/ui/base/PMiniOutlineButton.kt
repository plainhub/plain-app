package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PMiniOutlineButton(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    click: () -> Unit,
) {
    Button(
        click,
        modifier =
        modifier
            .height(32.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = color,
        ),
        border = BorderStroke(1.dp, color),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
