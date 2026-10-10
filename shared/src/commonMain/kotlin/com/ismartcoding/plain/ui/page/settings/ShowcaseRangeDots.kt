package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.base.PRangeDots
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace

@Composable
internal fun ShowcaseRangeDots() {
    Subtitle("PRangeDots")
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        var selectedIndex by remember { mutableIntStateOf(0) }
        val options = ButtonSize.entries
        PRangeDots(
            items = options,
            selected = options[selectedIndex],
            onSelect = { options.indexOf(it).also { i -> if (i >= 0) selectedIndex = i } },
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    VerticalSpace(16.dp)
}