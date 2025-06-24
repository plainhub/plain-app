package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.theme.red

@Composable
fun POutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    type: ButtonType = ButtonType.PRIMARY,
    isLoading: Boolean = false
) {
    val contentColor = when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonType.SECONDARY -> MaterialTheme.colorScheme.secondary
        ButtonType.DANGER -> MaterialTheme.colorScheme.red
    }

    val borderColor = when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ButtonType.SECONDARY -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        ButtonType.DANGER -> MaterialTheme.colorScheme.red.copy(alpha = 0.5f)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(32.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
        ),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                HorizontalSpace(8.dp)
            } else if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                HorizontalSpace(8.dp)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
} 