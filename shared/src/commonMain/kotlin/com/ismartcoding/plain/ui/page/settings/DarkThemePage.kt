package com.ismartcoding.plain.ui.page.settings

import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkThemePage(navController: NavHostController) {
    val darkTheme = UserPrefs.darkTheme.collectAsStateValue()
    val amoledDarkTheme = UserPrefs.amoledDarkTheme.collectAsStateValue()
    val pdfFollowDarkTheme = UserPrefs.pdfFollowDarkTheme.collectAsStateValue()
    val scope = rememberCoroutineScope()

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
                title = stringResource(Res.string.dark_theme),
            )
        },
        content = { paddingValues ->
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                item {
                    TopSpace()
                }
                item {
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        DarkTheme.entries.forEach {
                            PListItem(
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        UserPrefs.setDarkThemeValue(it.value)
                                    }
                                },
                                title = it.getText(),
                            ) {
                                RadioButton(selected = it.value == darkTheme, onClick = {
                                    scope.launch {
                                        UserPrefs.setDarkThemeValue(it.value)
                                    }
                                })
                            }
                        }
                    }
                }
                item {
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(
                            modifier = Modifier.clickable {
                                scope.launch {
                                    UserPrefs.amoledDarkTheme.value = !amoledDarkTheme
                                }
                            },
                            title = stringResource(Res.string.amoled_dark_theme),
                        ) {
                            PSwitch(activated = amoledDarkTheme) {
                                scope.launch {
                                    UserPrefs.amoledDarkTheme.value = !amoledDarkTheme
                                }
                            }
                            HorizontalSpace(8.dp)
                        }
                    }
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(
                            modifier = Modifier.clickable {
                                scope.launch {
                                    UserPrefs.pdfFollowDarkTheme.value = !pdfFollowDarkTheme
                                }
                            },
                            title = stringResource(Res.string.pdf_follow_dark_theme),
                            subtitle = stringResource(Res.string.pdf_follow_dark_theme_desc),
                        ) {
                            PSwitch(activated = pdfFollowDarkTheme) {
                                scope.launch {
                                    UserPrefs.pdfFollowDarkTheme.value = !pdfFollowDarkTheme
                                }
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
