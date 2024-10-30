package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.ui.theme.yellow

enum class AlertType {
    WARNING,
    ERROR,
}

@Composable
fun PAlert(
    title: String,
    description: String,
    type: AlertType,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    PCard {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.circle_alert),
                contentDescription = "",
                tint = if (type == AlertType.WARNING) MaterialTheme.colorScheme.yellow else MaterialTheme.colorScheme.red,
            )
            HorizontalSpace(dp = 8.dp)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Start,
            )
        }
        Text(
            modifier = Modifier
                .padding(16.dp, 0.dp, 16.dp, 16.dp)
                .fillMaxWidth(),
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        )
        if (actions != null) {
            Row(
                modifier = Modifier
                    .padding(16.dp, 0.dp, 16.dp, 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                actions.invoke(this)
            }
        }
    }
    VerticalSpace(dp = 16.dp)
}
