package com.ismartcoding.plain.ui.page.settings

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.DarkTheme
import com.ismartcoding.plain.data.preference.CustomPrimaryColorPreference
import com.ismartcoding.plain.data.preference.DarkThemePreference
import com.ismartcoding.plain.data.preference.LocalCustomPrimaryColor
import com.ismartcoding.plain.data.preference.LocalDarkTheme
import com.ismartcoding.plain.data.preference.LocalThemeIndex
import com.ismartcoding.plain.data.preference.ThemeIndexPreference
import com.ismartcoding.plain.ui.base.BlockRadioButton
import com.ismartcoding.plain.ui.base.BlockRadioGroupButtonItem
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.ClipboardTextField
import com.ismartcoding.plain.ui.base.DynamicSVGImage
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.colorpicker.ColorEnvelope
import com.ismartcoding.plain.ui.base.colorpicker.HsvColorPicker
import com.ismartcoding.plain.ui.base.colorpicker.rememberColorPickerController
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.svg.PALETTE
import com.ismartcoding.plain.ui.svg.SVGString
import com.ismartcoding.plain.ui.theme.palette.TonalPalettes
import com.ismartcoding.plain.ui.theme.palette.TonalPalettes.Companion.toTonalPalettes
import com.ismartcoding.plain.ui.theme.palette.checkColorHex
import com.ismartcoding.plain.ui.theme.palette.dynamic.extractTonalPalettesFromUserWallpaper
import com.ismartcoding.plain.ui.theme.palette.onDark
import com.ismartcoding.plain.ui.theme.palette.onLight
import com.ismartcoding.plain.ui.theme.palette.safeHexToColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        navController,
        topBarTitle = stringResource(R.string.color_and_style),
        content = {
            LazyColumn {
                item {
                    VerticalSpace(dp = 16.dp)
                }
                item {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .aspectRatio(1.38f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                MaterialTheme.colorScheme.inverseOnSurface
                                        onLight MaterialTheme.colorScheme.surface.copy(0.7f),
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
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    BlockRadioButton(
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
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    Subtitle(
                        text = stringResource(R.string.appearance),
                    )
                    PListItem(
                        title = stringResource(R.string.dark_theme),
                        desc = DarkTheme.values().find { it.value == darkTheme }?.getText(context) ?: "",
                        separatedActions = true,
                        onClick = {
                            navController.navigate(RouteName.DARK_THEME)
                        },
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
    val colorPickerController = rememberColorPickerController()
    var addDialogVisible by remember { mutableStateOf(false) }
    var customColorValue by remember { mutableStateOf(customPrimaryColor) }

    fun saveColor() {
        if (customColorValue.checkColorHex() != null) {
            scope.launch(Dispatchers.IO) {
                CustomPrimaryColorPreference.putAsync(context, customColorValue.checkColorHex()!!)
                ThemeIndexPreference.putAsync(context, 4)
                addDialogVisible = false
            }
        } else {
            scope.launch {
                Toast.makeText(context, "Invalid Value", Toast.LENGTH_LONG).show()
                addDialogVisible = false
            }
        }
    }

    if (palettes.isEmpty()) {
        Row(
            modifier =
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme.inverseOnSurface
                            onLight MaterialTheme.colorScheme.surface.copy(0.7f),
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
                .padding(horizontal = 16.dp),
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
        AlertDialog(
            onDismissRequest = {
                addDialogVisible = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveColor()
                    }
                ) {
                    Text(stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { addDialogVisible = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(R.string.primary_color))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                )
                {
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(300.dp),
                        controller = colorPickerController,
                        initialColor = customColorValue.safeHexToColor(),
                        onColorChanged = { colorEnvelope: ColorEnvelope ->
                                customColorValue = colorEnvelope.hexCode
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        ClipboardTextField(
                            value = customColorValue,
                            modifier = Modifier.weight(0.6f),
                            placeholder = stringResource(R.string.primary_color_hint),
                            onValueChange = {
                                customColorValue = it
                            },
                            onConfirm = {
                                it.checkColorHex()?.let { h ->
                                    scope.launch(Dispatchers.IO) {
                                        CustomPrimaryColorPreference.putAsync(context, h)
                                        ThemeIndexPreference.putAsync(context, 4)
                                        addDialogVisible = false
                                    }
                                }
                            }
                        )
                        Card(
                            modifier = Modifier
                                .height(50.dp)
                                .width(50.dp),
                            shape = RoundedCornerShape(CornerSize(5.dp)),
                            colors = CardColors(
                                containerColor = customColorValue.safeHexToColor(),
                                contentColor = contentColorFor(backgroundColor = customColorValue.safeHexToColor()),
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.LightGray
                            )
                        ) {}
                    }
                }
            }
        )
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
            MaterialTheme.colorScheme
                .inverseOnSurface onLight MaterialTheme.colorScheme.surface.copy(0.7f)
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
