package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class GroupButton(
    val icon: Any,
    val text: String,
    val onClick: () -> Unit,
)

@Composable
fun GroupButtons(buttons: List<GroupButton>) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        buttons.forEach { button ->
            PIconTextActionButton(
                icon = button.icon,
                text = button.text,
                click = button.onClick,
            )
        }
    }
}