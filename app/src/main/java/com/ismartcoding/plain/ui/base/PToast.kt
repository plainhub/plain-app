package com.ismartcoding.plain.ui.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.yellow

enum class ToastType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

@Composable
fun PToast(
    message: String,
    type: ToastType = ToastType.INFO,
    onDismiss: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onDismiss()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (type) {
                        ToastType.INFO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                        ToastType.SUCCESS -> MaterialTheme.colorScheme.green.copy(alpha = 0.95f)
                        ToastType.WARNING -> MaterialTheme.colorScheme.yellow.copy(alpha = 0.95f)
                        ToastType.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.95f)
                    }
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconRes = when (type) {
                        ToastType.INFO -> R.drawable.info
                        ToastType.SUCCESS -> R.drawable.circle_check
                        ToastType.WARNING -> R.drawable.circle_alert
                        ToastType.ERROR -> R.drawable.circle_x
                    }
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
} 