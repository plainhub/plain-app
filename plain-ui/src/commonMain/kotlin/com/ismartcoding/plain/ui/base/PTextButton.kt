package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize

@Composable
fun PTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    buttonSize: ButtonSize = ButtonSize.MEDIUM,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(buttonSize.height),
        shape = RoundedCornerShape(buttonSize.cornerRadius),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        contentPadding = buttonSize.getPaddingValues(),
        enabled = enabled && !isLoading,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(if (buttonSize == ButtonSize.SMALL) 14.dp else 16.dp), strokeWidth = 2.dp, color = contentColor)
                HorizontalSpace(8.dp)
            } else if (icon != null) {
                Icon(painter = icon, contentDescription = null, modifier = Modifier.size(if (buttonSize == ButtonSize.SMALL) 16.dp else 20.dp), tint = contentColor)
                HorizontalSpace(8.dp)
            }
            Text(
                text = text,
                style = buttonSize.textStyle(),
                fontWeight = buttonSize.fontWeight(),
            )
        }
    }
}
