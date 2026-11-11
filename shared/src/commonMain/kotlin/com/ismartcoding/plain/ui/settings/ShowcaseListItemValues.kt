package com.ismartcoding.plain.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.StepNumber
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.settings as ui_drawable_settings
import org.jetbrains.compose.resources.painterResource

@Composable
fun ShowcaseListItemValues() {
    var rtl by remember { mutableStateOf(false) }
    var largeText by remember { mutableStateOf(false) }
    var narrow by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    PSectionTitle("PListItem cases")
    Column(Modifier.padding(horizontal = 16.dp)) {
        PCard {
            PListItem(title = "RTL", action = { Switch(rtl, onCheckedChange = { rtl = it }) })
            PListItem(title = "Large text (150%)", action = { Switch(largeText, onCheckedChange = { largeText = it }) })
            PListItem(title = "Narrow width (75%)", action = { Switch(narrow, onCheckedChange = { narrow = it }) })
        }
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalDensity provides Density(density.density, density.fontScale * if (largeText) 1.5f else 1f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth(if (narrow) 0.75f else 1f)) {
            ShowcaseListItemText()
            ShowcaseListItemLeading()
            ShowcaseListItemActions()
            ShowcaseListItemKeyValues()
            ShowcaseListItemSlots()
            ShowcaseListItemAppearance()
        }
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseListItemText() {
    PSectionTitle("Text and padding")
    PCard {
        PListItem(title = "Title only")
        PListItem(title = "Title and subtitle", subtitle = "One supporting line")
        PListItem(title = "Explicit empty subtitle", subtitle = "")
        PListItem(title = "A long title that wraps across several lines and keeps its full text visible")
        PListItem(title = "Multiline subtitle", subtitle = "Supporting text wraps across several lines to show the top and bottom padding around the complete text block.")
        PListItem(title = "Explicit line breaks", subtitle = "First supporting line\nSecond supporting line\nThird supporting line")
        PListItem(title = "多语言标题", subtitle = "中文副标题与 English supporting text 自动换行并保留完整内容。")
    }
}

@Composable
private fun ShowcaseListItemLeading() {
    PSectionTitle("Leading content")
    PCard {
        PListItem(title = "Icon and title", icon = UiRes.drawable.ui_drawable_settings)
        PListItem(title = "Icon and subtitle", subtitle = "Supporting text", icon = UiRes.drawable.ui_drawable_settings)
        PListItem(title = "Icon and multiline text", subtitle = "Long supporting text wraps across several lines beside the leading icon.", icon = UiRes.drawable.ui_drawable_settings, showMore = true)
        PListItem(title = "Custom leading slot", subtitle = "Slot supplies its own content without extra padding", start = { StepNumber(1, modifier = Modifier) })
        PListItem(title = "Start takes precedence over icon", icon = UiRes.drawable.ui_drawable_settings, start = { StepNumber(2, modifier = Modifier) })
    }
}

@Composable
private fun ShowcaseListItemActions() {
    var checked by remember { mutableStateOf(true) }
    var clicks by remember { mutableIntStateOf(0) }
    PSectionTitle("Trailing content and interaction")
    PCard {
        PListItem(title = "Chevron only", showMore = true)
        PListItem(title = "Subtitle and chevron", subtitle = "Supporting text", showMore = true)
        PListItem(title = "Switch", action = { Switch(checked, onCheckedChange = { checked = it }) })
        PListItem(title = "Subtitle and switch", subtitle = "Supporting text", action = { Switch(checked, onCheckedChange = { checked = it }) })
        PListItem(title = "Separated switch", separatedActions = true, action = { Switch(checked, onCheckedChange = { checked = it }) })
        PListItem(title = "Action and chevron", showMore = true, action = { PTextButton("Open", onClick = { clicks++ }) })
        PListItem(title = "Separated action and chevron", separatedActions = true, showMore = true, action = { PTextButton("Open", onClick = { clicks++ }) })
        PListItem(title = "Separated chevron", separatedActions = true, showMore = true)
        PListItem(title = "Title suffix", titleSuffix = { Text("NEW", style = MaterialTheme.typography.labelLarge) })
        PListItem(title = "A long title with an inline suffix that stays visible", titleSuffix = { Text("NEW", style = MaterialTheme.typography.labelLarge) })
        PListItem(title = "Title trailing slot", titleTrailing = { Text("ON") })
        PListItem(title = "Clickable row", subtitle = "Clicks: $clicks", modifier = Modifier.clickable { clicks++ }, showMore = true)
    }
}

@Composable
private fun ShowcaseListItemKeyValues() {
    PSectionTitle("Key and value")
    PCard {
        PListItem(title = "Setting title", value = "OK")
        PListItem(title = "ID", value = "Selected value")
        PListItem(title = "Longer setting title", value = "A value that exceeds the remaining width and wraps onto multiple lines")
        PListItem(title = "ID", value = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        PListItem(title = "A title that is longer than the entire list item width and needs to wrap", value = "Complete value", showMore = true)
        PListItem(title = "Multiline value", value = "First line\nSecond line\nThird line")
        PListItem(title = "多语言标题需要完整显示", value = "这是需要在剩余宽度内自动换行的详细内容")
        PListItem(title = "Title and subtitle", subtitle = "Supporting text remains below the title", value = "A long value with several words", showMore = true)
        PListItem(title = "Notifications", subtitle = "Alerts and banners", value = "Enabled", showMore = true, icon = UiRes.drawable.ui_drawable_settings)
        PListItem(title = "Custom leading", value = "Enabled", start = { StepNumber(3, modifier = Modifier) })
        PListItem(title = "Empty value", value = "", showMore = true)
        PListItem(title = "Null value", value = null, showMore = true)
        PListItem(title = "Empty value with action", value = "", action = { Switch(true, onCheckedChange = null) })
    }
}

@Composable
private fun ShowcaseListItemSlots() {
    PSectionTitle("Content slots")
    PCard {
        PListItem(title = { Text("Slot title only") })
        PListItem(title = { Text("Slot title and subtitle") }, subtitle = { Text("Supporting text") })
        PListItem(title = { Text("Title and description") }, description = { Text("Description without a subtitle") })
        PListItem(title = { Text("Three text lines") }, subtitle = { Text("Supporting text") }, description = { Text("Description after an 8dp gap") })
        PListItem(
            title = { Text("Long content slots") },
            subtitle = { Text("A supporting text slot that wraps across multiple lines at the current width and font scale") },
            description = { Text("A longer description that also wraps, retaining the spacing between visible text blocks.") },
            icon = UiRes.drawable.ui_drawable_settings,
            action = { Switch(true, onCheckedChange = null) },
        )
        PListItem(title = { Text("Custom leading and action") }, subtitle = { Text("Supporting text") }, start = { StepNumber(4, modifier = Modifier) }, action = { PTextButton("Open", onClick = {}) })
        PListItem(title = { Text("Compact row (48dp minimum)") }, minHeight = 48.dp)
        PListItem(title = { Text("Compact multiline row") }, subtitle = { Text("Text still determines the required height") }, minHeight = 48.dp)
    }
}

@Composable
private fun ShowcaseListItemAppearance() {
    PSectionTitle("Appearance")
    PCard {
        PListItem(title = "Dimmed text", subtitle = "Visual dimming", dimmed = true, icon = UiRes.drawable.ui_drawable_settings, showMore = true)
        PListItem(title = "Dimmed value", subtitle = "Supporting text", value = "Enabled", dimmed = true, showMore = true)
        PListItem(title = "Custom content color", subtitle = "Supporting text", contentColor = MaterialTheme.colorScheme.error, icon = UiRes.drawable.ui_drawable_settings, showMore = true)
        PListItem(title = "Custom value color", subtitle = "Supporting text", value = "Error", contentColor = MaterialTheme.colorScheme.error, showMore = true)
        PListItem(title = { Text("Status stripe") }, subtitle = { Text("Stripe follows the layout start in RTL") }, statusColor = MaterialTheme.colorScheme.primary)
        PListItem(title = { Text("Dimmed slots and status") }, subtitle = { Text("Supporting text") }, description = { Text("Description") }, dimmed = true, statusColor = MaterialTheme.colorScheme.error, start = {
            Icon(painterResource(UiRes.drawable.ui_drawable_settings), null, Modifier.size(24.dp))
        })
    }
}
