package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PInputChip
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.settings as ui_drawable_settings

@Composable
internal fun ShowcaseComponentStates() {
    var enabled by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var rtl by remember { mutableStateOf(false) }
    var largeText by remember { mutableStateOf(false) }
    var clicks by remember { mutableIntStateOf(0) }
    var value by remember { mutableStateOf("") }
    val density = LocalDensity.current
    Subtitle("States and accessibility")
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Switch(enabled, onCheckedChange = { enabled = it })
            Text("Enabled")
            Switch(loading, onCheckedChange = { loading = it })
            Text("Loading")
        }
        Row {
            Switch(rtl, onCheckedChange = { rtl = it })
            Text("RTL")
            Switch(largeText, onCheckedChange = { largeText = it })
            Text("Large text")
        }
        Text("Actions: $clicks")
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalDensity provides Density(density.density, density.fontScale * if (largeText) 1.5f else 1f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PFilledButton("A longer button label", onClick = { clicks++ }, enabled = enabled, isLoading = loading)
            POutlinedButton("Outlined action", onClick = { clicks++ }, enabled = enabled, isLoading = loading)
            PTextButton("Text action", onClick = { clicks++ }, enabled = enabled, isLoading = loading)
            PTextField(readOnly = false, value = value, onValueChange = { value = it },
                label = "Input", errorMessage = if (value.isBlank()) "Enter a value" else "", enabled = enabled)
            PTextField(readOnly = true, value = "Selectable read-only text", onValueChange = {})
            PInputChip("Removable chip", onClick = { clicks++ }, onClose = { clicks++ }, enabled = enabled)
            PBanner(title = "Static banner", desc = "A longer description that follows the available width")
            PSheetPrimaryActionsCard {
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_settings, "Settings", enabled = enabled, onClick = { clicks++ })
            }
        }
    }
}
