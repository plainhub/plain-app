package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.cardContainer

@Composable
fun PIconTextButton(
    icon: Any,
    text: String,
    modifier: Modifier = Modifier,
    click: () -> Unit,
) {
    Column(
        modifier =
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                click()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerticalSpace(dp = 16.dp)
        PIcon(
            icon = icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
fun PIconTextActionButton(
    icon: Any,
    text: String,
    click: () -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors()
                .copy(
                    containerColor = MaterialTheme.colorScheme.cardContainer(),
                ),
            onClick = click
        ) {
            PIcon(
                icon = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp),
        )
    }
}