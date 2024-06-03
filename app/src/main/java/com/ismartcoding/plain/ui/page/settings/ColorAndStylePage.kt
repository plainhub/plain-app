package com.ismartcoding.plain.ui.page.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preference.CustomPrimaryColorPreference
import com.ismartcoding.plain.preference.DarkThemePreference
import com.ismartcoding.plain.preference.LocalCustomPrimaryColor
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.preference.LocalThemeIndex
import com.ismartcoding.plain.preference.ThemeIndexPreference
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.base.BlockRadioButtons
import com.ismartcoding.plain.ui.base.BlockRadioGroupButtonItem
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.DynamicSVGImage
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.components.ColorPickerDialog
import com.ismartcoding.plain.ui.nav.navigate
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.nav.RouteName
import com.ismartcoding.plain.ui.base.svg.PALETTE
import com.ismartcoding.plain.ui.base.svg.SVGString
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.cardContainer
import com.ismartcoding.plain.ui.theme.palette.TonalPalettes
import com.ismartcoding.plain.ui.theme.palette.TonalPalettes.Companion.toTonalPalettes
import com.ismartcoding.plain.ui.theme.palette.checkColorHex
import com.ismartcoding.plain.ui.theme.palette.dynamic.extractTonalPalettesFromUserWallpaper
import com.ismartcoding.plain.ui.theme.palette.onDark
import com.ismartcoding.plain.ui.theme.palette.safeHexToColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorAndStylePage(navController: NavHostController) {
    val context = LocalContext.current
    val darkTheme = LocalDarkTheme.current
    val themeIndex = LocalThemeIndex.current
    val customPrimaryColor = LocalCustomPrimaryColor.current
    val scope = rememberCoroutineScope()

    val wallpaperTonalPalettes = extractTonalPalettesFromUserWallpaper()
    var radioButtonSelected by remember { mutableIntStateOf(if (themeIndex > 4) 0 else 1) }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.color_and_style))
        },
        content = {
            LazyColumn {
                item {
                    TopSpace()
                }
                item {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)
                            .aspectRatio(1.38f)
                            .clip(RoundedCornerShape(PlainTheme.CARD_RADIUS))
                            .background(
                                MaterialTheme.colorScheme.cardContainer(),
                            )
                            .clickable { },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DynamicSVGImage(
                            modifier = Modifier.padding(60.dp),
                            svgImageString = SVGString.PALETTE,
                            contentDescription = stringResource(R.string.color_and_style),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    BlockRadioButtons(
                        selected = radioButtonSelected,
                        onSelected = { radioButtonSelected = it },
                        itemRadioGroups =
                        listOf(
                            BlockRadioGroupButtonItem(
                                text = stringResource(R.string.wallpaper_colors),
                                onClick = {},
                            ) {
                                Palettes(
                                    context = context,
                                    palettes =
                                    wallpaperTonalPalettes.run {
                                        if (this.size > 5) {
                                            this.subList(5, wallpaperTonalPalettes.size)
                                        } else {
                                            emptyList()
                                        }
                                    },
                                    themeIndex = themeIndex,
                                    themeIndexPrefix = 5,
                                    customPrimaryColor = customPrimaryColor,
                                )
                            },
                            BlockRadioGroupButtonItem(
                                text = stringResource(R.string.basic_colors),
                                onClick = {},
                            ) {
                                Palettes(
                                    context = context,
                                    themeIndex = themeIndex,
                                    palettes = wallpaperTonalPalettes.subList(0, 5),
                                    customPrimaryColor = customPrimaryColor,
                                )
                            },
                        ),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Subtitle(
                        text = stringResource(R.string.appearance),
                    )
                    PListItem(
                        modifier = PlainTheme.getCardModifier().clickable {
                            navController.navigate(RouteName.DARK_THEME)
                        },
                        title = stringResource(R.string.dark_theme),
                        desc = DarkTheme.entries.find { it.value == darkTheme }?.getText(context) ?: "",
                        separatedActions = true,
                    ) {
                        PSwitch(
                            activated = DarkTheme.isDarkTheme(darkTheme),
                        ) {
                            scope.launch {
                                withIO {
                                    DarkThemePreference.putAsync(context, if (it) DarkTheme.ON else DarkTheme.OFF)
                                }
                            }
                        }
                    }
                    BottomSpace()
                }
            }
        },
    )
}

@Composable
fun Palettes(
    context: Context,
    palettes: List<TonalPalettes>,
    themeIndex: Int = 0,
    themeIndexPrefix: Int = 0,
    customPrimaryColor: String = "",
) {
    val scope = rememberCoroutineScope()
    val tonalPalettes = customPrimaryColor.safeHexToColor().toTonalPalettes()
    var addDialogVisible by remember { mutableStateOf(false) }
    var customColorValue by remember { mutableStateOf(customPrimaryColor) }

    fun saveColor() {
        val hex = customColorValue.checkColorHex()
        if (hex != null) {
            scope.launch(Dispatchers.IO) {
                CustomPrimaryColorPreference.putAsync(context, hex)
                ThemeIndexPreference.putAsync(context, 4)
                addDialogVisible = false
            }
        } else {
            DialogHelper.showMessage(getString(R.string.invalid_value))
        }
    }

    if (palettes.isEmpty()) {
        Row(
            modifier =
            Modifier
                .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme.cardContainer(),
                )
                .clickable {},
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.no_palettes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseSurface,
            )
        }
    } else {
        Row(
            modifier =
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            palettes.forEachIndexed { index, palette ->
                val isCustom = index == palettes.lastIndex && themeIndexPrefix == 0
                val i = themeIndex - themeIndexPrefix
                SelectableMiniPalette(
                    selected = if (i >= palettes.size) i == 0 else i == index,
                    isCustom = isCustom,
                    onClick = {
                        if (isCustom) {
                            customColorValue = customPrimaryColor
                            addDialogVisible = true
                        } else {
                            scope.launch(Dispatchers.IO) {
                                ThemeIndexPreference.putAsync(context, themeIndexPrefix + index)
                            }
                        }
                    },
                    palette = if (isCustom) tonalPalettes else palette,
                )
            }
        }
    }

    if (addDialogVisible) {
        ColorPickerDialog(title = stringResource(R.string.primary_color),
            initValue = customColorValue, onDismiss = {
                addDialogVisible = false
            }, onConfirm = {
                customColorValue = it
                saveColor()
            })
    }
}

@Composable
fun SelectableMiniPalette(
    modifier: Modifier = Modifier,
    selected: Boolean,
    isCustom: Boolean = false,
    onClick: () -> Unit,
    palette: TonalPalettes,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color =
        if (isCustom) {
            MaterialTheme.colorScheme.primaryContainer
                .copy(0.5f) onDark MaterialTheme.colorScheme.onPrimaryContainer.copy(0.3f)
        } else {
            MaterialTheme.colorScheme.cardContainer()
        },
    ) {
        Surface(
            modifier =
            Modifier
                .clickable { onClick() }
                .padding(16.dp)
                .size(48.dp),
            shape = CircleShape,
            color = palette primary 90,
        ) {
            Box {
                Surface(
                    modifier =
                    Modifier
                        .size(48.dp)
                        .offset((-24).dp, 24.dp),
                    color = palette tertiary 90,
                ) {}
                Surface(
                    modifier =
                    Modifier
                        .size(48.dp)
                        .offset(24.dp, 24.dp),
                    color = palette secondary 60,
                ) {}
                AnimatedVisibility(
                    visible = selected,
                    modifier =
                    Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                    exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.checked),
                        modifier =
                        Modifier
                            .padding(8.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}
