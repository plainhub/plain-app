package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun PCard(
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
        shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
        colors = CardDefaults.cardColors().copy(
            containerColor = MaterialTheme.colorScheme.cardBackgroundNormal
        )
    ) {
        content()
    }
}