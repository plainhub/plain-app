package com.ismartcoding.plain.ui.page.settings
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.events.DownloadUpdateEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.AppLogHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.preferences.UpdateInfoPreference
import com.ismartcoding.plain.preferences.DeveloperModePreference
import com.ismartcoding.plain.preferences.LocalAutoCheckUpdate
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PDonationBanner
import com.ismartcoding.plain.ui.base.PExploreBanner
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.models.consumeUpdateDownloadEvent
import com.ismartcoding.plain.ui.page.home.UpdateBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsPage(navController: NavHostController, updateViewModel: UpdateViewModel) {
    val autoCheckUpdate = LocalAutoCheckUpdate.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var developerMode by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableLongStateOf(0L) }
    var fileSize by remember { mutableLongStateOf(AppLogHelper.getFileSize(context)) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            cacheSize = AppHelper.getCacheSize(context)
            developerMode = DeveloperModePreference.getAsync()
        }
    }

    LaunchedEffect(Unit) {
        Channel.sharedFlow.collect { event ->
            if (event is DownloadUpdateEvent) {
                listState.animateScrollToItem(0)
            }
            updateViewModel.consumeUpdateDownloadEvent(event)
        }
    }

    UpdateDialog(updateViewModel)

    PScaffold(
        topBar = { PTopAppBar(navController = navController, title = stringResource(Res.string.settings)) },
        content = { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            ) {
                item { TopSpace() }
                if (AppFeatureType.DONATION.has()) {
                    item {
                        PDonationBanner(onClick = { WebHelper.open(context, "https://ko-fi.com/ismartcoding") })
                        VerticalSpace(dp = 16.dp)
                    }
                } else {
                    item {
                        PExploreBanner(onClick = { WebHelper.open(context, "https://plainapp.app") })
                        VerticalSpace(dp = 16.dp)
                    }
                }
                item {
                    if (AppFeatureType.CHECK_UPDATES.has()) {
                        UpdateBanner(updateViewModel)
                        VerticalSpace(dp = 16.dp)
                    }
                }
                item { SettingsCardItems(navController) }

                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(
                            modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = {
                                developerMode = true
                                scope.launch(Dispatchers.IO) { DeveloperModePreference.putAsync(true) }
                            }),
                            title = stringResource(Res.string.android_version),
                            value = MainApp.getAndroidVersion(),
                        )
                        if (AppFeatureType.CHECK_UPDATES.has()) {
                            PListItem(title = stringResource(Res.string.app_version), subtitle = MainApp.getAppVersion(), action = {
                                PFilledButton(text = stringResource(Res.string.check_update), buttonSize = ButtonSize.SMALL, onClick = {
                                    scope.launch {
                                        DialogHelper.showMessage(Res.string.checking_updates)
                                        val r = withIO {
                                            UpdateInfoPreference.updateAsync { it.copy(skipVersion = "") }
                                            AppHelper.checkUpdateAsync(context, true)
                                        }
                                        if (r != null) {
                                            if (r) updateViewModel.showDialog()
                                            else DialogHelper.showMessage(Res.string.is_latest_version)
                                        }
                                    }
                                })
                            })
                            PListItem(title = stringResource(Res.string.auto_check_update), subtitle = stringResource(Res.string.auto_check_update_desc)) {
                                PSwitch(activated = autoCheckUpdate) { newValue -> scope.launch(Dispatchers.IO) { UpdateInfoPreference.updateAsync { it.copy(autoCheckUpdate = newValue) } } }
                            }
                        } else {
                            PListItem(title = stringResource(Res.string.app_version), value = MainApp.getAppVersion())
                        }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    AboutLogsAndCacheCard(
                        navController = navController, context = context, scope = scope,
                        fileSize = fileSize, onFileSizeCleared = { fileSize = 0 },
                        cacheSize = cacheSize, onCacheCleared = { cacheSize = it },
                        developerMode = developerMode, onDeveloperModeChanged = { developerMode = it },
                    )
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(
                            modifier = Modifier.clickable { WebHelper.open(context, UrlHelper.getTermsUrl()) },
                            title = stringResource(Res.string.terms_of_use), showMore = true
                        )
                        PListItem(
                            modifier = Modifier.clickable { WebHelper.open(context, UrlHelper.getPolicyUrl()) },
                            title = stringResource(Res.string.privacy_policy), showMore = true
                        )
                    }
                }
                if (developerMode) {
                    item {
                        VerticalSpace(dp = 16.dp)
                        DeveloperSettingsCard(navController)
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        },
    )
}
