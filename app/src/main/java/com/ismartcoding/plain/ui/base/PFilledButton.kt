package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.theme.red

@Composable
fun PFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    type: ButtonType = ButtonType.PRIMARY,
    enabled: Boolean = true
) {
    val containerColor = when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonType.SECONDARY -> MaterialTheme.colorScheme.secondary
        ButtonType.DANGER -> MaterialTheme.colorScheme.red
    }

    val contentColor = when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        ButtonType.SECONDARY -> MaterialTheme.colorScheme.onSecondary
        ButtonType.DANGER -> MaterialTheme.colorScheme.onError
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(27.dp), 
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.12f),
            disabledContentColor = contentColor.copy(alpha = 0.38f)
        ),
        enabled = enabled
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
} 