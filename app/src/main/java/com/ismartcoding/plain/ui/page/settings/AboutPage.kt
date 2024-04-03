package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.enums.AppFeatureType
import com.ismartcoding.plain.data.preference.SkipVersionPreference
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.WebHelper
import com.ismartcoding.plain.ui.models.UpdateViewModel
import com.ismartcoding.plain.ui.page.RouteName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    navController: NavHostController,
    updateViewModel: UpdateViewModel = viewModel()
) {
    val context = LocalContext.current
    var demoMode by remember { mutableStateOf(TempData.demoMode) }
    val scope = rememberCoroutineScope()

    UpdateDialog(updateViewModel)

    PScaffold(
        navController,
        topBarTitle = stringResource(R.string.about),
        content = {
            LazyColumn {
                item {
                    TopSpace()
                }
                item {
                    PCard {
                        PListItem(
                            title = stringResource(R.string.client_id),
                            value = TempData.clientId,
                        )
                        if (AppFeatureType.CHECK_UPDATES.has()) {
                            PListItem(
                                title = stringResource(R.string.app_version),
                                desc = MainApp.getAppVersion(),
                                action = {
                                    PMiniOutlineButton(text = stringResource(R.string.check_update)) {
                                        scope.launch {
                                            DialogHelper.showMessage(getString(R.string.checking_updates))
                                            val r = withIO {
                                                SkipVersionPreference.putAsync(context, "")
                                                AppHelper.checkUpdateAsync(context, true)
                                            }
                                            if (r != null) {
                                                if (r == true) {
                                                    updateViewModel.showDialog()
                                                } else {
                                                    DialogHelper.showMessage(getString(R.string.is_latest_version))
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                        } else {
                            PListItem(
                                title = stringResource(R.string.app_version),
                                value = MainApp.getAppVersion(),
                            )
                        }
                        PListItem(
                            title = stringResource(R.string.android_version),
                            value = MainApp.getAndroidVersion(),
                        )
                    }
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(
                            title = stringResource(R.string.logs),
                            showMore = true,
                            onClick = {
                                navController.navigate(RouteName.LOGS)
                            },
                        )
                        PListItem(
                            title = stringResource(R.string.donation),
                            showMore = true,
                            onClick = {
                                WebHelper.open(context, "https://ko-fi.com/ismartcoding")
                            },
                        )
                        if (BuildConfig.DEBUG) {
                            PListItem(
                                title = stringResource(R.string.demo_mode),
                            ) {
                                PSwitch(
                                    activated = demoMode,
                                ) {
                                    demoMode = it
                                    TempData.demoMode = it
                                }
                            }
                        }
                    }
                    VerticalSpace(dp = 16.dp)
                    PCard {
                        PListItem(
                            title = stringResource(R.string.terms_of_use),
                            showMore = true,
                            onClick = {
                                WebHelper.open(context, UrlHelper.getTermsUrl())
                            },
                        )
                        PListItem(
                            title = stringResource(R.string.privacy_policy),
                            showMore = true,
                            onClick = {
                                WebHelper.open(context, UrlHelper.getPolicyUrl())
                            },
                        )
                    }
                    BottomSpace()
                }
            }
        },
    )
}
