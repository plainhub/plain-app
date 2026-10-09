package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.base.PSegmentedButtons
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace

@Composable
internal fun ShowcaseSegmentedButtons() {
    Subtitle("PSegmentedButtons")
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ButtonSize.entries.forEach { size ->
            var selectedIndex by remember { mutableIntStateOf(0) }
            PSegmentedButtons(
                options = listOf("First", "Second", "Third"),
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                label = { it },
                buttonSize = size,
            )
        }
        PSegmentedButtons(
            options = ButtonSize.entries,
            selectedIndex = -1,
            onSelect = {},
            label = { it.name },
            optionEnabled = { it != ButtonSize.LARGE },
        ) { option, _ -> Text(option.name) }
        PSegmentedButtons(
            options = listOf("First", "Second"),
            selectedIndex = 0,
            onSelect = {},
            label = { it },
            enabled = false,
        )
    }
    VerticalSpace(16.dp)
}
