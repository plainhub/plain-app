package com.ismartcoding.plain.ui.settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.components.ButtonType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.settings
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
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.Tips
import com.ismartcoding.plain.ui.components.ToastManager
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.lightbulb as ui_drawable_lightbulb
import com.ismartcoding.plain.ui.resources.settings as ui_drawable_settings
import com.ismartcoding.plain.ui.theme.PlainTheme
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentShowcasePage(navController: NavHostController) {
    PScaffold(
        topBar = {
            PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = stringResource(Res.string.ui_components))
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            item { TopSpace() }
            item { ShowcaseAlerts() }
            item { ShowcaseBanners() }
            item { ShowcaseCards() }
            item { ShowcaseListItemValues() }
            item { ShowcaseButtons() }
            item { ShowcaseSegmentedButtons() }
            item { ShowcaseRangeDots() }
            item { ShowcaseComponentStates() }
            item { ShowcaseChipsAndSwitches() }
            item { ShowcaseTips() }
            item { ShowcaseToasts() }
            item { ShowcaseColors() }
            item { BottomSpace(paddingValues) }
        }
    }
}

@Composable
private fun ShowcaseAlerts() {
    PSectionTitle("PAlert")
    PAlert(description = "This is a warning alert.", type = AlertType.WARNING, modifier = Modifier.padding(horizontal = 16.dp))
    VerticalSpace(8.dp)
    PAlert(description = "This is an error alert.", type = AlertType.ERROR, modifier = Modifier.padding(horizontal = 16.dp))
    VerticalSpace(8.dp)
    PAlert(description = "This is an info alert.", type = AlertType.INFO, modifier = Modifier.padding(horizontal = 16.dp))
    VerticalSpace(8.dp)
    PAlert(description = "Alert with action.", type = AlertType.WARNING, modifier = Modifier.padding(horizontal = 16.dp)) {
        PTextButton(text = "Fix Now", buttonSize = ButtonSize.SMALL, onClick = {})
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseBanners() {
    PSectionTitle("PBanner")
    PBanner(title = "Banner Title", desc = "Banner description.", icon = UiRes.drawable.ui_drawable_lightbulb, onClick = {})
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseCards() {
    PSectionTitle("PCard + PListItem")
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(title = "List item with icon", subtitle = "PSectionTitle", icon = UiRes.drawable.ui_drawable_settings, showMore = true)
        PListItem(title = "List item with value", value = "Value")
        PListItem(title = "List item with switch", separatedActions = true) {
            val checked = remember { mutableStateOf(true) }
            Switch(checked = checked.value, onCheckedChange = { checked.value = it })
        }
        PListItem(
            title = { Text("List title") },
            subtitle = { Text("Supporting text") },
            description = { Text("Optional description") },
            icon = UiRes.drawable.ui_drawable_settings,
            statusColor = MaterialTheme.colorScheme.primary,
        )
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseButtons() {
    PSectionTitle("Buttons")
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PFilledButton(text = "Filled Button", onClick = {})
        PFilledButton(text = "Filled Danger", type = ButtonType.DANGER, onClick = {})
        POutlinedButton(modifier = Modifier.fillMaxWidth(), text = "Outlined Block", onClick = {})
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PFilledButton(text = "Small Filled", buttonSize = ButtonSize.SMALL, onClick = {})
            POutlinedButton(text = "Small Outlined", buttonSize = ButtonSize.SMALL, onClick = {})
        }
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseChipsAndSwitches() {
    PSectionTitle("Chips & Switches")
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
            Switch(checked = s1.value, onCheckedChange = { s1.value = it })
            Switch(checked = s2.value, onCheckedChange = { s2.value = it })
        }
    }
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseTips() {
    PSectionTitle("Tips")
    Tips(text = "This is a tips component for supplementary information.")
    VerticalSpace(16.dp)
}

@Composable
private fun ShowcaseToasts() {
    PSectionTitle("Toast")
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PFilledButton(text = "Info Toast", onClick = {
            ToastManager.showInfoToast("This is an info toast")
        })
        PFilledButton(text = "Success Toast", type = ButtonType.PRIMARY, onClick = {
            ToastManager.showSuccessToast("Operation completed successfully")
        })
        POutlinedButton(text = "Warning Toast", type = ButtonType.TERTIARY, onClick = {
            ToastManager.showWarningToast("Please review your input")
        })
        PFilledButton(text = "Error Toast", type = ButtonType.DANGER, onClick = {
            ToastManager.showErrorToast("Something went wrong")
        })
    }
    VerticalSpace(16.dp)
}
