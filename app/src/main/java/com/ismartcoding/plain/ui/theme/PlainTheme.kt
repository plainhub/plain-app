package com.ismartcoding.plain.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

object PlainTheme {
    val PAGE_HORIZONTAL_MARGIN = 16.dp
    val PAGE_TOP_MARGIN = 8.dp
    val CARD_CORNER = 16.dp

    @Composable
    fun getCardModifier(index: Int, size: Int): Modifier {
        val shape = if (index == 0) {
            if (size == 1) {
                RoundedCornerShape(CARD_CORNER)
            } else {
                RoundedCornerShape(topStart = CARD_CORNER, topEnd = CARD_CORNER)
            }
        } else if (index == size - 1) {
            RoundedCornerShape(bottomStart = CARD_CORNER, bottomEnd = CARD_CORNER)
        } else {
            RectangleShape
        }
        return Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_HORIZONTAL_MARGIN)
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.cardBack(),
                shape = shape,
            )
    }
}