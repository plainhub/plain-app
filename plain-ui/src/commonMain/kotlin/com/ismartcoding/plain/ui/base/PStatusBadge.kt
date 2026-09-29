package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.BadgeType

@Composable
fun PStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    type: BadgeType = BadgeType.NEUTRAL,
) {
    Surface(modifier = modifier, shape = CircleShape, color = type.container()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = type.onContainer(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}