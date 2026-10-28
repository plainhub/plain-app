package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.features.dlna.sender.RustDlnaSender
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.casting
import com.ismartcoding.plain.i18n.casting_to
import com.ismartcoding.plain.i18n.customize_home_features
import com.ismartcoding.plain.i18n.launcher_shortcuts
import com.ismartcoding.plain.i18n.tools
import com.ismartcoding.plain.ui.components.ActionButtonAddWithMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.StatusIndicator
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.notes.components.QuickNoteCard
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.navigation.MainNavScaffold
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.grid_3x3 as ui_drawable_grid_3x3
import com.ismartcoding.plain.ui.resources.layout_grid as ui_drawable_layout_grid
import com.ismartcoding.plain.ui.theme.greenText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsPage(
    navController: NavHostController,
    onTabSelected: (Int) -> Unit,
) {
    val currentUri by RustDlnaSender.currentUri.collectAsState()
    val featuresStr =
        Prefs.appHomeFeatures.collectAsStateValue()
    val notesEnabled =
        Prefs.parseFeatures(featuresStr)
            .contains(AppFeatureType.NOTES.name) && AppFeatureType.NOTES.has()

    MainNavScaffold(
        selectedIndex = 2,
        onTabSelected = onTabSelected,
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.tools),
                actions = {
                    ActionButtonAddWithMenu { dismiss ->
                        PDropdownMenuItem(
                            text = { Text(stringResource(Res.string.customize_home_features)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(UiRes.drawable.ui_drawable_layout_grid),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                dismiss()
                                navController.navigate(Routing.CustomFeatures)
                            },
                        )
                        if (Capability.LAUNCHER_SHORTCUTS in getDeviceCapabilities()) {
                            PDropdownMenuItem(
                                text = { Text(stringResource(Res.string.launcher_shortcuts)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(UiRes.drawable.ui_drawable_grid_3x3),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    dismiss()
                                    navController.navigate(Routing.LauncherShortcuts)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        HomeFeatureItemsGrid(
            navController = navController,
            modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp + paddingValues.calculateBottomPadding()),
            header = {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    if (currentUri.isNotEmpty() && RustDlnaSender.currentDevice != null) {
                        val deviceName = RustDlnaSender.currentDevice?.getDeviceName() ?: ""
                        StatusIndicator(
                            text = if (deviceName.isNotEmpty()) stringResource(Res.string.casting_to, deviceName) else stringResource(Res.string.casting),
                            onClick = { navController.navigate(Routing.CastSession) },
                            pillColor = Color.Transparent,
                            dotColor = MaterialTheme.colorScheme.greenText,
                            textColor = MaterialTheme.colorScheme.greenText,
                            contentPaddingHorizontal = 0.dp,
                            contentPaddingVertical = 8.dp,
                            textStartPadding = 8.dp,
                        )
                        VerticalSpace(8.dp)
                    }
                    if (notesEnabled) {
                        QuickNoteCard(
                            onOpenNote = { id -> navController.navigate(Routing.NoteDetail(id)) },
                            onEmptyExpand = { navController.navigate(Routing.NotesCreate("")) },
                        )
                    }
                }
            },
        )
    }
}
