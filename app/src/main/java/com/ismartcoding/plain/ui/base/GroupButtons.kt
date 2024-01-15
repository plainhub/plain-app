package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.palette.onDark

data class GroupButton(
    val icon: ImageVector,
    val text: String,
    val onClick: () -> Unit,
)

@Composable
fun GroupButtons(buttons: List<GroupButton>) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        HorizontalSpace(dp = 16.dp)
        buttons.forEachIndexed { index, button ->
            Column(
                modifier =
                Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (index == 0) 24.dp else 0.dp,
                            bottomStart = if (index == 0) 24.dp else 0.dp,
                            topEnd = if (index == buttons.size - 1) 24.dp else 0.dp,
                            bottomEnd = if (index == buttons.size - 1) 24.dp else 0.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(0.7f) onDark MaterialTheme.colorScheme.inverseOnSurface)
                    .clickable(onClick = button.onClick),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 32.dp, end = 32.dp),
                ) {
                    Icon(
                        imageVector = button.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp, bottom = 16.dp),
                    text = button.text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalSpace(dp = 16.dp)
    }
}