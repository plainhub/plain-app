package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.preference.LocalLatestRelease
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlertDialog
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
import com.ismartcoding.plain.ui.page.RouteName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    var demoMode by remember { mutableStateOf(TempData.demoMode) }
    val scope = rememberCoroutineScope()
    var updateDialogVisible by remember {
        mutableStateOf(false)
    }
    val latest = LocalLatestRelease.current
    PAlertDialog(
        modifier = Modifier.heightIn(max = 400.dp),
        visible = updateDialogVisible,
        onDismissRequest = { updateDialogVisible = false },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Update,
                contentDescription = stringResource(R.string.change_log),
            )
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = latest?.name ?: "")
                VerticalSpace(dp = 16.dp)
                Text(
                    text = "${latest?.publishedAt?.ifEmpty { latest.createdAt ?: "" }} ${FormatHelper.formatBytes(latest?.getDownloadSize() ?: 0L)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                VerticalSpace(dp = 16.dp)
            }
        },
        text = {
            SelectionContainer {
                Text(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    text = latest?.body ?: "",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    WebHelper.open(context, latest?.htmlUrl ?: "")
                }
            ) {
                Text(
                    text = stringResource(R.string.update)
                )
            }
        },
        dismissButton = {
        },
    )

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
                        PListItem(
                            title = stringResource(R.string.app_version),
                            desc = MainApp.getAppVersion(),
                            action = {
                                PMiniOutlineButton(text = stringResource(R.string.check_update)) {
                                    scope.launch {
                                        DialogHelper.showMessage(getString(R.string.checking_updates))
                                        val r = withIO { AppHelper.checkUpdateAsync(context, true) }
                                        if (r != null) {
                                            if (r == true) {
                                                updateDialogVisible = true
                                            } else {
                                                DialogHelper.showMessage(getString(R.string.is_latest_version))
                                            }
                                        }
                                    }
                                }
                            },
                        )
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
