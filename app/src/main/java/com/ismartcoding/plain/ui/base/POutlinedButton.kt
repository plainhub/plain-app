package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
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
    isLoading: Boolean = false,
    enabled: Boolean = true,
    small: Boolean = false,
    block: Boolean = false,
    contentColor: Color? = null,
) {
    val resolvedColor = contentColor ?: when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonType.SECONDARY -> MaterialTheme.colorScheme.secondary
        ButtonType.DANGER -> MaterialTheme.colorScheme.red
    }
    val borderColor = resolvedColor.copy(alpha = 0.5f)
    val height = if (small) 32.dp else 40.dp
    val shape = if (small) RoundedCornerShape(32.dp) else RoundedCornerShape(40.dp)
    val padding = PaddingValues(horizontal = 12.dp)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.then(if (block) Modifier.fillMaxWidth() else Modifier).height(height),
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = resolvedColor),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = padding,
        enabled = enabled,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = resolvedColor)
                HorizontalSpace(8.dp)
            } else if (icon != null) {
                Icon(painter = icon, contentDescription = null, modifier = Modifier.size(if (small) 16.dp else 20.dp), tint = resolvedColor)
                HorizontalSpace(8.dp)
            }
            Text(
                text = text,
                style = if (small) MaterialTheme.typography.labelSmall
                else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
} 