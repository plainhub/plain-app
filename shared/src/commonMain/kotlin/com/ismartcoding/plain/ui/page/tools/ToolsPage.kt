package com.ismartcoding.plain.ui.page.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.casting
import com.ismartcoding.plain.i18n.casting_to
import com.ismartcoding.plain.i18n.tools
import com.ismartcoding.plain.ui.base.ActionButtonAdd
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.StatusIndicator
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.preferences.HomeFeaturesPreference
import com.ismartcoding.plain.preferences.appDataStore
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.ui.components.QuickNoteCard
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.primaryPill
import com.ismartcoding.plain.ui.theme.primaryText
import com.ismartcoding.plain.ui.page.MainBottomBar
import com.ismartcoding.plain.ui.page.home.HomeFeatureItemsGrid
import com.ismartcoding.plain.ui.theme.greenPill
import com.ismartcoding.plain.ui.theme.greenText
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsPage(
    navController: NavHostController,
    onTabSelected: (Int) -> Unit,
) {
    val currentUri by CastPlayer.currentUri.collectAsState()
    val featuresStr =
        remember {
            appDataStore.dataFlow.map { HomeFeaturesPreference.get(it) }
        }.collectAsStateValue(initial = HomeFeaturesPreference.default)
    val notesEnabled =
        HomeFeaturesPreference.parseList(featuresStr.ifEmpty { HomeFeaturesPreference.default })
            .contains(AppFeatureType.NOTES.name)

    PScaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.tools),
                actions = {
                    ActionButtonAdd {
                        navController.navigate(Routing.CustomFeatures)
                    }
                },
            )
        },
        bottomBar = { MainBottomBar(selectedIndex = 2, onTabSelected = onTabSelected) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            if (currentUri.isNotEmpty() && CastPlayer.currentDevice != null) {
                val deviceName = CastPlayer.currentDevice?.getDeviceName() ?: ""
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    StatusIndicator(
                        text = if (deviceName.isNotEmpty()) stringResource(Res.string.casting_to, deviceName) else stringResource(Res.string.casting),
                        onClick = { navController.navigate(Routing.CastSession) },
                        pillColor = MaterialTheme.colorScheme.greenPill,
                        dotColor = MaterialTheme.colorScheme.greenText,
                        textColor = MaterialTheme.colorScheme.greenText,
                        cornerRadius = 24.dp,
                        contentPaddingHorizontal = 16.dp,
                        contentPaddingVertical = 8.dp,
                        textStartPadding = 8.dp,
                    )
                }
                VerticalSpace(8.dp)
            }
            TopSpace()
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (notesEnabled) {
                    QuickNoteCard(
                        onOpenNote = { id -> navController.navigate(Routing.NoteDetail(id)) },
                        onEmptyExpand = { navController.navigate(Routing.NotesCreate("")) },
                    )
                }
                HomeFeatureItemsGrid(navController)
            }
            VerticalSpace(dp = 16.dp)
            BottomSpace(paddingValues)
        }
    }
}
