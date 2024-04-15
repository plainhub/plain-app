package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.theme.buttonTextLarge

@Composable
fun PBlockButton(
    text: String,
    type: ButtonType = ButtonType.PRIMARY,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp),
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        colors = type.getColors(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.buttonTextLarge(),
        )
    }
}
