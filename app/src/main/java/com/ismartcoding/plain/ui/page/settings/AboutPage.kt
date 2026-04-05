package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.AppLogHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.preferences.AutoCheckUpdatePreference
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.preferences.LocalAutoCheckUpdate
import com.ismartcoding.plain.preferences.SkipVersionPreference
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.UpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AboutPage(navController: NavHostController, updateViewModel: UpdateViewModel = viewModel()) {
    val context = LocalContext.current
    var cacheSize by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    var fileSize by remember { mutableLongStateOf(AppLogHelper.getFileSize(context)) }
    var developerMode by remember { mutableStateOf(false) }
    val autoCheckUpdate = LocalAutoCheckUpdate.current

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            cacheSize = AppHelper.getCacheSize(context)
            developerMode = DeveloperModePreference.getAsync(context)
        }
    }

    UpdateDialog(updateViewModel)

    PScaffold(
        topBar = { PTopAppBar(navController = navController, title = stringResource(R.string.about)) },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item { TopSpace() }
                item {
                    PCard {
                        if (developerMode) PListItem(title = stringResource(R.string.client_id), value = TempData.clientId)
                        PListItem(
                            modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = {
                                developerMode = true
                                scope.launch(Dispatchers.IO) { DeveloperModePreference.putAsync(context, true) }
                            }),
                            title = stringResource(R.string.android_version), value = MainApp.getAndroidVersion())
                        if (AppFeatureType.CHECK_UPDATES.has()) {
                            PListItem(title = stringResource(R.string.app_version), subtitle = MainApp.getAppVersion(), action = {
                                POutlinedButton(text = stringResource(R.string.check_update), small = true, onClick = {
                                    scope.launch {
                                        DialogHelper.showMessage(getString(R.string.checking_updates))
                                        val r = withIO { SkipVersionPreference.putAsync(context, ""); AppHelper.checkUpdateAsync(context, true) }
                                        if (r != null) { if (r == true) updateViewModel.showDialog() else DialogHelper.showMessage(getString(R.string.is_latest_version)) }
                                    }
                                })
                            })
                            PListItem(title = stringResource(R.string.auto_check_update), subtitle = stringResource(R.string.auto_check_update_desc)) {
                                PSwitch(activated = autoCheckUpdate) { scope.launch(Dispatchers.IO) { AutoCheckUpdatePreference.putAsync(context, it) } }
                            }
                        } else {
                            PListItem(title = stringResource(R.string.app_version), value = MainApp.getAppVersion())
                        }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    AboutLogsAndCacheCard(
                        navController = navController, context = context, scope = scope,
                        fileSize = fileSize, onFileSizeCleared = { fileSize = 0 },
                        cacheSize = cacheSize, onCacheCleared = { cacheSize = it },
                        developerMode = developerMode, onDeveloperModeChanged = { developerMode = it })
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(modifier = Modifier.clickable { WebHelper.open(context, UrlHelper.getTermsUrl()) },
                            title = stringResource(R.string.terms_of_use), showMore = true)
                        PListItem(modifier = Modifier.clickable { WebHelper.open(context, UrlHelper.getPolicyUrl()) },
                            title = stringResource(R.string.privacy_policy), showMore = true)
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        },
    )
}
