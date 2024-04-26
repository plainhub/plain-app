package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDialogRadioRow(selected: Boolean, onClick: () -> Unit, text: String) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                baselineShift = BaselineShift.None,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}