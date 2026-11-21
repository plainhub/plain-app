package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.ui.components.PSwitch
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue

import com.ismartcoding.plain.ui.theme.PlainTheme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.auto_check_update
import com.ismartcoding.plain.i18n.auto_check_update_desc
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCheckUpdatePage(navController: NavHostController, updateViewModel: UpdateViewModel) {
    val autoCheckUpdate = Prefs.autoCheckUpdate.collectAsStateValue()
    val scope = rememberCoroutineScope()

    UpdateDialog(updateViewModel)

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                title = stringResource(Res.string.auto_check_update),
            )
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                }
                item {
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(title = stringResource(Res.string.auto_check_update), subtitle = stringResource(Res.string.auto_check_update_desc)) {
                            PSwitch(checked = autoCheckUpdate, onCheckedChange = { newValue -> scope.launch(Dispatchers.Default) {
                                Prefs.setAutoCheckUpdate(newValue) }
                            })
                        }
                    }
                    BottomSpace(paddingValues)
                }
            }
        },
    )
}
