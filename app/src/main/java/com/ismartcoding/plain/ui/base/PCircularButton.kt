package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun PCircularButton(
    text: String,
    icon: Int,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.cardBackgroundNormal,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = description,
                modifier = Modifier.size(48.dp),
            )
        }

        VerticalSpace(12.dp)

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
} 