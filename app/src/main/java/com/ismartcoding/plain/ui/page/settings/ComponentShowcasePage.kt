package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcasePage(navController: NavHostController) {
    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.ui_components))
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            item { TopSpace() }
            item { ShowcaseAlerts() }
            item { ShowcaseBanners() }
            item { ShowcaseCards() }
            item { ShowcaseButtons() }
            item { ShowcaseChipsAndSwitches() }
            item { ShowcaseTips() }
            item { ShowcaseColors() }
            item { BottomSpace(paddingValues) }
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ShowcaseAlerts() {
    SectionTitle("PAlert")
    PAlert(description = "This is a warning alert.", type = AlertType.WARNING)
    VerticalSpace(8.dp)
    PAlert(description = "This is an error alert.", type = AlertType.ERROR)
    VerticalSpace(8.dp)
    PAlert(description = "Alert with action.", type = AlertType.WARNING) {
        POutlinedButton(text = "Fix Now", small = true, onClick = {})
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseBanners() {
    SectionTitle("PBanner")
    PBanner(title = "Banner Title", desc = "Banner description.", icon = R.drawable.lightbulb, onClick = {})
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseCards() {
    SectionTitle("PCard + PListItem")
    PCard {
        PListItem(title = "List item with icon", subtitle = "Subtitle", icon = R.drawable.settings, showMore = true)
        PListItem(title = "List item with value", value = "Value")
        PListItem(title = "List item with switch", separatedActions = true) {
            val checked = remember { mutableStateOf(true) }
            PSwitch(activated = checked.value) { checked.value = it }
        }
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseButtons() {
    SectionTitle("Buttons")
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PFilledButton(text = "Filled Button", onClick = {})
        PFilledButton(text = "Filled Danger", type = ButtonType.DANGER, onClick = {})
        POutlinedButton(text = "Outlined Block", block = true, onClick = {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PFilledButton(text = "Small Filled", buttonSize = ButtonSize.SMALL, onClick = {})
            POutlinedButton(text = "Small Outlined", small = true, onClick = {})
        }
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseChipsAndSwitches() {
    SectionTitle("Chips & Switches")
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val selected = remember { mutableStateOf(false) }
            PFilterChip(selected = selected.value, onClick = { selected.value = !selected.value }, label = { Text("Filter Chip") })
            PFilterChip(selected = true, onClick = {}, label = { Text("Selected") })
        }
        VerticalSpace(12.dp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            val s1 = remember { mutableStateOf(false) }
            val s2 = remember { mutableStateOf(true) }
            PSwitch(activated = s1.value) { s1.value = it }
            PSwitch(activated = s2.value) { s2.value = it }
        }
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseTips() {
    SectionTitle("Tips")
    Tips(text = "This is a tips component for supplementary information.")
    VerticalSpace(16.dp)
}
