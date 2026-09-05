package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

// Top-aligned step row: PListItem centers its start slot vertically, which looks
// wrong once the description wraps to multiple lines. When stacked in a PCard,
// wrap them in a Column with padding(vertical = 8.dp) so card edges are 16dp.
@Composable
fun StepItem(index: Int, title: String, desc: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        StepNumber(index)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.listItemTitle())
            VerticalSpace(dp = 8.dp)
            Text(text = desc, style = MaterialTheme.typography.listItemSubtitle())
        }
    }
}
