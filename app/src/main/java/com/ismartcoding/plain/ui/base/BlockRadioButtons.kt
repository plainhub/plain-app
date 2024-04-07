package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.cardContainer

@Composable
fun BlockRadioButtons(
    modifier: Modifier = Modifier,
    selected: Int = 0,
    onSelected: (Int) -> Unit,
    itemRadioGroups: List<BlockRadioGroupButtonItem> = listOf(),
) {
    Column {
        Row(
            modifier = modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemRadioGroups.forEachIndexed { index, item ->
                BlockRadioButton(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(end = if (item == itemRadioGroups.last()) 0.dp else 8.dp),
                    text = item.text,
                    selected = selected == index,
                ) {
                    onSelected(index)
                    item.onClick()
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        itemRadioGroups[selected].content()
    }
}

@Composable
fun BlockRadioButton(
    modifier: Modifier = Modifier,
    text: String = "",
    selected: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.cardContainer(),
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.inverseSurface,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit = {},
) {
    Column(
        modifier =
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) selectedContainerColor else containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style =
            MaterialTheme.typography.labelLarge.copy(
                textAlign = TextAlign.Center,
            ),
            color = if (selected) selectedContentColor else contentColor,
        )
    }
}

data class BlockRadioGroupButtonItem(
    val text: String,
    val onClick: () -> Unit = {},
    val content: @Composable () -> Unit,
)
