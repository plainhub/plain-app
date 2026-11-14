package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.features.settings.AppUpdateDownloadStatus
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.platform.system.AppChannelType
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.features.settings.AppUpdateDownloads
import com.ismartcoding.plain.lib.UrlHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.system.getAppVersion
import com.ismartcoding.plain.platform.system.getCacheSize
import com.ismartcoding.plain.platform.system.getLogFileSize
import com.ismartcoding.plain.platform.system.getOSVersion
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.chat.PeerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.components.PDonationBanner
import com.ismartcoding.plain.ui.components.PExploreBanner
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.features.web.WebHelper
import com.ismartcoding.plain.ui.home.UpdateBanner
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsPage(navController: NavHostController, updateViewModel: UpdateViewModel, peerVM: PeerViewModel) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var cacheSize by remember { mutableLongStateOf(0L) }
    var fileSize by remember { mutableLongStateOf(0L) }
    val isDiscoverable = Prefs.nearbyDiscoverable.collectAsStateValue()
    val developerMode = Prefs.developerMode.collectAsStateValue()
    var developerModeRevealed by remember { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }
    var lastVersionTap by remember { mutableStateOf<TimeMark?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            try {
                cacheSize = getCacheSize()
                fileSize = getLogFileSize()
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (error: Exception) {
                DialogHelper.showErrorMessage(error.toString())
            }
        }
    }

    val download = AppUpdateDownloads.state.collectAsStateValue()
    LaunchedEffect(download.taskId, download.status) {
        if (download.status == AppUpdateDownloadStatus.DOWNLOADING) listState.animateScrollToItem(0)
    }

    UpdateDialog(updateViewModel)

    PScaffold(
        topBar = { PTopAppBar(onNavigateBack = { navController.navigateUp() }, title = stringResource(Res.string.settings)) },
        content = { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            ) {
                item { TopSpace() }
                if (buildChannel != AppChannelType.GOOGLE.name) {
                    item {
                        PDonationBanner(onClick = { WebHelper.open("https://ko-fi.com/ismartcoding") })
                        VerticalSpace(dp = 16.dp)
                    }
                } else {
                    item {
                        PExploreBanner(onClick = { WebHelper.open("https://plainapp.app") })
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
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(
                            title = stringResource(Res.string.system_version),
                            value = getOSVersion(),
                        )
                        PListItem(
                            modifier = Modifier.clickable(enabled = !developerMode && !developerModeRevealed) {
                                versionTaps = if (lastVersionTap?.elapsedNow()?.let { it <= 1.seconds } == true) {
                                    versionTaps + 1
                                } else {
                                    1
                                }
                                lastVersionTap = TimeSource.Monotonic.markNow()
                                if (versionTaps == 5) developerModeRevealed = true
                            },
                            title = stringResource(Res.string.app_version),
                            value = getAppVersion(),
                        )
                        if (developerMode || developerModeRevealed) {
                            PListItem(title = stringResource(Res.string.developer_mode)) {
                                Switch(checked = developerMode, onCheckedChange = {
                                    scope.launch(Dispatchers.Default) {
                                        Prefs.set(PrefKeys.DEVELOPER_MODE, it)
                                    }
                                })
                            }
                        }
                        if (AppFeatureType.CHECK_UPDATES.has()) {
                            PListItem(modifier = Modifier.clickable {
                                navController.navigate(Routing.AutoCheckUpdate)
                            }, title = stringResource(Res.string.auto_check_update), separatedActions = true, showMore = true, action = {
                                PFilledButton(text = stringResource(Res.string.check_update), buttonSize = ButtonSize.SMALL, onClick = {
                                    scope.launch {
                                        DialogHelper.showMessage(Res.string.checking_updates)
                                        Prefs.setSkipVersion("")
                                        val r = withIO { checkAppUpdate(true) }
                                        if (r != null) {
                                            if (r) updateViewModel.showDialog()
                                            else DialogHelper.showMessage(Res.string.is_latest_version)
                                        }
                                    }
                                })
                            })
                        }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(title = stringResource(Res.string.events), showMore = true,
                            modifier = Modifier.clickable { navController.navigate(Routing.Events) })
                    }
                    VerticalSpace(dp = 16.dp)
                    AboutLogsAndCacheCard(
                        navController = navController, scope = scope,
                        fileSize = fileSize, onFileSizeCleared = { fileSize = 0 },
                        cacheSize = cacheSize, onCacheCleared = { cacheSize = it },
                    )
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(
                            modifier = Modifier.clickable { WebHelper.open(UrlHelper.getTermsUrl()) },
                            title = stringResource(Res.string.terms_of_use), showMore = true
                        )
                        PListItem(
                            modifier = Modifier.clickable { WebHelper.open(UrlHelper.getPolicyUrl()) },
                            title = stringResource(Res.string.privacy_policy), showMore = true
                        )
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    DeveloperSettingsCard(
                        navController = navController,
                        developerMode = developerMode,
                    )
                }
                item { BottomSpace(paddingValues) }
            }
        },
    )
}
