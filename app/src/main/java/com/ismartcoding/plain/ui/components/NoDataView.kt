package com.ismartcoding.plain.ui.components

import org.jetbrains.compose.resources.DrawableResource
import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.VerticalSpace

@Composable
fun NoDataView(
    modifier: Modifier = Modifier,
    message: String = stringResource(Res.string.no_data),
    icon: DrawableResource = Res.drawable.files,
    showRefreshButton: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
        )

        VerticalSpace(20.dp)

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        
        if (showRefreshButton) {
            VerticalSpace(32.dp)

            FilledTonalButton(
                onClick = onRefresh
            ) {
                Text(text = stringResource(Res.string.refresh))
            }
        }
    }
} 