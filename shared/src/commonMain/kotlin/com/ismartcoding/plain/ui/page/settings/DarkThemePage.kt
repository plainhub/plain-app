package com.ismartcoding.plain.ui.page.settings
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
import com.ismartcoding.plain.preferences.AmoledDarkThemePreference
import com.ismartcoding.plain.preferences.DarkThemePreference
import com.ismartcoding.plain.preferences.LocalAmoledDarkTheme
import com.ismartcoding.plain.preferences.LocalDarkTheme
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
    val darkTheme = LocalDarkTheme.current
    val amoledDarkTheme = LocalAmoledDarkTheme.current
    val pdfFollowDarkTheme = LocalPdfFollowDarkTheme.current
    val scope = rememberCoroutineScope()

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
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
                                        DarkThemePreference.putAsync(it.value)
                                    }
                                },
                                title = it.getText(),
                            ) {
                                RadioButton(selected = it.value == darkTheme, onClick = {
                                    scope.launch {
                                        DarkThemePreference.putAsync(it.value)
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
                                    AmoledDarkThemePreference.putAsync(!amoledDarkTheme)
                                }
                            },
                            title = stringResource(Res.string.amoled_dark_theme),
                        ) {
                            PSwitch(activated = amoledDarkTheme) {
                                scope.launch {
                                    AmoledDarkThemePreference.putAsync(!amoledDarkTheme)
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
                                    PdfFollowDarkThemePreference.putAsync(!pdfFollowDarkTheme)
                                }
                            },
                            title = stringResource(Res.string.pdf_follow_dark_theme),
                            subtitle = stringResource(Res.string.pdf_follow_dark_theme_desc),
                        ) {
                            PSwitch(activated = pdfFollowDarkTheme) {
                                scope.launch {
                                    PdfFollowDarkThemePreference.putAsync(!pdfFollowDarkTheme)
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
