package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.Version
import com.ismartcoding.plain.data.toVersion
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.preferences.LocalNewVersion
import com.ismartcoding.plain.preferences.LocalSkipVersion
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBanner
import com.ismartcoding.plain.ui.base.PDonationBanner
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavHostController, updateViewModel: UpdateViewModel = viewModel()) {
    val currentVersion = Version(BuildConfig.VERSION_NAME)
    val newVersion = LocalNewVersion.current.toVersion()
    val skipVersion = LocalSkipVersion.current.toVersion()
    val context = LocalContext.current
    var developerMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        developerMode = DeveloperModePreference.getAsync(context)
    }

    UpdateDialog(updateViewModel)

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.settings))
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                }
                item {
                    PDonationBanner(
                        onClick = {
                            WebHelper.open(context, "https://ko-fi.com/ismartcoding")
                        },
                    )
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    if (AppFeatureType.CHECK_UPDATES.has() && newVersion.whetherNeedUpdate(currentVersion, skipVersion)) {
                        PBanner(
                            title = stringResource(R.string.get_new_updates, newVersion.toString()),
                            desc = stringResource(R.string.get_new_updates_desc),
                            icon = R.drawable.lightbulb,
                        ) {
                            updateViewModel.showDialog()
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                }
                item {
                    SettingsCardItems(navController)
                }
                if (developerMode) {
                    item {
                        VerticalSpace(dp = 16.dp)
                        DeveloperSettingsCard(navController)
                    }
                }
                debugItem()
                item {
                    BottomSpace(paddingValues)
                }
            }
        },
    )
}
