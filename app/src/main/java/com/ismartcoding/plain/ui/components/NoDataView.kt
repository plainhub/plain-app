package com.ismartcoding.plain.ui.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.VerticalSpace

@Composable
fun NoDataView(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.no_data),
    iconResId: Int = R.drawable.files,
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
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
        )

        VerticalSpace(20.dp)

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        if (showRefreshButton) {
            VerticalSpace(32.dp)

            FilledTonalButton(
                onClick = onRefresh
            ) {
                Text(text = stringResource(R.string.refresh))
            }
        }
    }
} 