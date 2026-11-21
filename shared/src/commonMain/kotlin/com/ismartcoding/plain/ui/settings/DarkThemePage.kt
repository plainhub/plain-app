package com.ismartcoding.plain.ui.settings

import com.ismartcoding.plain.ui.components.PSwitch
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.theme.DarkTheme
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkThemePage(navController: NavHostController) {
    val darkTheme = Prefs.darkTheme.collectAsStateValue()
    val amoledDarkTheme = Prefs.amoledDarkTheme.collectAsStateValue()
    val pdfFollowDarkTheme = Prefs.pdfFollowDarkTheme.collectAsStateValue()
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
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN).selectableGroup()) {
                        DarkTheme.entries.forEach {
                            PListItem(
                                modifier = Modifier.selectable(
                                    selected = it == darkTheme,
                                    role = Role.RadioButton,
                                    onClick = { scope.launch { Prefs.set(PrefKeys.DARK_THEME, it.name) } },
                                ),
                                title = it.getText(),
                            ) {
                                RadioButton(selected = it == darkTheme, onClick = null)
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
                                    Prefs.set(PrefKeys.AMOLED_DARK_THEME, !amoledDarkTheme)
                                }
                            },
                            title = stringResource(Res.string.amoled_dark_theme),
                        ) {
                            PSwitch(checked = amoledDarkTheme, onCheckedChange = {
                                scope.launch {
                                    Prefs.set(PrefKeys.AMOLED_DARK_THEME, !amoledDarkTheme)
                                }
                            })
                        }
                    }
                    VerticalSpace(dp = 16.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        PListItem(
                            modifier = Modifier.clickable {
                                scope.launch {
                                    Prefs.set(PrefKeys.PDF_FOLLOW_DARK_THEME, !pdfFollowDarkTheme)
                                }
                            },
                            title = stringResource(Res.string.pdf_follow_dark_theme),
                            subtitle = stringResource(Res.string.pdf_follow_dark_theme_desc),
                        ) {
                            PSwitch(checked = pdfFollowDarkTheme, onCheckedChange = {
                                scope.launch {
                                    Prefs.set(PrefKeys.PDF_FOLLOW_DARK_THEME, !pdfFollowDarkTheme)
                                }
                            })
                        }
                    }
                    BottomSpace(paddingValues)
                }
            }
        },
    )
}
