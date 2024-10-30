package com.ismartcoding.plain.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PlainTheme {
    val PAGE_HORIZONTAL_MARGIN = 16.dp
    val PAGE_TOP_MARGIN = 8.dp
    val CARD_RADIUS = 12.dp

    @Composable
    fun getCardModifier(index: Int = 0, size: Int = 1, selected: Boolean = false): Modifier {
        val shape = if (index == 0) {
            if (size == 1) {
                RoundedCornerShape(CARD_RADIUS)
            } else {
                RoundedCornerShape(topStart = CARD_RADIUS, topEnd = CARD_RADIUS)
            }
        } else if (index == size - 1) {
            RoundedCornerShape(bottomStart = CARD_RADIUS, bottomEnd = CARD_RADIUS)
        } else {
            RectangleShape
        }
        return Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_HORIZONTAL_MARGIN)
            .clip(shape)
            .background(
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.cardContainer(),
                shape = shape,
            )
    }
}

fun Modifier.largeBlockButton() = this
    .fillMaxWidth()
    .height(48.dp)
    .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)


fun Typography.buttonTextLarge() = bodyMedium.copy(fontSize = 16.sp)

@Composable
fun Typography.tipsText() = bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun Typography.listItemTitle() = titleMedium.copy(
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurface
)

@Composable
fun Typography.listItemValue() = bodyMedium.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)

@Composable
fun Typography.listItemDescription() = titleSmall.copy(color = MaterialTheme.colorScheme.onSurface)

@Composable
fun Typography.listItemSubtitle() = labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
fun Typography.listItemTag() = labelLarge.copy(color = MaterialTheme.colorScheme.primary)
