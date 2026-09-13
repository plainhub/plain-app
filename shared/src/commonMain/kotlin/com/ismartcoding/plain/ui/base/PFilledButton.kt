package com.ismartcoding.plain.ui.base

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.theme.filledButtonContent


@Composable
fun PFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    type: ButtonType = ButtonType.PRIMARY,
    buttonSize: ButtonSize = ButtonSize.LARGE,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val containerColor = when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonType.DANGER -> MaterialTheme.colorScheme.error
        ButtonType.TERTIARY -> MaterialTheme.colorScheme.tertiary
    }
    // Fills use their on-pair content: primary is the pastel accent in dark so
    // its content is the dark onPrimary; danger is pale salmon in dark so its
    // content is the dark onError. Disabled (non-loading) content sits on a
    // translucent fill over the dark surface, so it stays soft white.
    val contentColor = when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        ButtonType.DANGER -> MaterialTheme.colorScheme.onError
        ButtonType.TERTIARY -> MaterialTheme.colorScheme.onTertiary
    }
    val disabledContentColor = when (type) {
        ButtonType.TERTIARY -> contentColor.copy(alpha = 0.38f)
        else -> if (isLoading) contentColor else MaterialTheme.colorScheme.filledButtonContent.copy(alpha = 0.38f)
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(buttonSize.height),
        shape = RoundedCornerShape(buttonSize.cornerRadius),
        elevation = buttonSize.elevation(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = if (isLoading) containerColor.copy(alpha = 0.8f) else containerColor.copy(alpha = 0.12f),
            disabledContentColor = disabledContentColor,
        ),
        contentPadding = buttonSize.getPaddingValues(),
        enabled = enabled && !isLoading,
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "filled_btn_${type.name.lowercase()}",
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (buttonSize == ButtonSize.SMALL) 18.dp else 24.dp),
                    strokeWidth = if (buttonSize == ButtonSize.SMALL) 2.dp else 3.dp,
                    color = contentColor,
                )
            } else {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            modifier = Modifier.size(if (buttonSize == ButtonSize.SMALL) 16.dp else 20.dp),
                            tint = contentColor,
                        )
                        HorizontalSpace(8.dp)
                    }
                    Text(
                        text = text,
                        style = buttonSize.textStyle(),
                        fontWeight = buttonSize.fontWeight()
                    )
                }
            }
        }
    }
}
