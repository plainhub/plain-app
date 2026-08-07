package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.auto_check_update
import com.ismartcoding.plain.i18n.auto_check_update_desc
import com.ismartcoding.plain.preferences.LocalAutoCheckUpdate
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.UpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCheckUpdatePage(navController: NavHostController, updateViewModel: UpdateViewModel) {
    val autoCheckUpdate = LocalAutoCheckUpdate.current
    val scope = rememberCoroutineScope()

    UpdateDialog(updateViewModel)

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.auto_check_update),
            )
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                }
                item {
                    PCard {
                        PListItem(title = stringResource(Res.string.auto_check_update), subtitle = stringResource(Res.string.auto_check_update_desc)) {
                            PSwitch(activated = autoCheckUpdate) { newValue -> scope.launch(Dispatchers.Default) {
                                UpdateInfoPreference.updateAsync { it.copy(autoCheckUpdate = newValue) } }
                            }
                            HorizontalSpace(8.dp)
                        }
                    }
                    BottomSpace(paddingValues)
                }
            }
        },
    )
}