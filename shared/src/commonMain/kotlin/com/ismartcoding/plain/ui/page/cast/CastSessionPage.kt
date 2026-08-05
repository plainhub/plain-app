package com.ismartcoding.plain.ui.page.cast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.casting
import com.ismartcoding.plain.i18n.casting_to
import com.ismartcoding.plain.i18n.no_active_cast
import com.ismartcoding.plain.i18n.playlist_title
import com.ismartcoding.plain.i18n.stop_casting
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastSessionPage(
    navController: NavHostController,
    castVM: CastViewModel = viewModel(key = "castSessionVM") { CastViewModel() },
) {
    val currentUri by CastPlayer.currentUri.collectAsState()
    val isPlaying by CastPlayer.isPlaying.collectAsState()
    val progress by CastPlayer.progress.collectAsState()
    val duration by CastPlayer.duration.collectAsState()
    val supportsCallback by CastPlayer.supportsCallback.collectAsState()
    val castItems by CastPlayer.items.collectAsState()
    val deviceName = CastPlayer.currentDevice?.getDeviceName() ?: ""

    LaunchedEffect(Unit) {
        if (currentUri.isNotEmpty()) {
            castVM.trySubscribeEvent()
            castVM.startPositionUpdater()
        }
    }

    val titleText = if (deviceName.isNotEmpty()) {
        stringResource(Res.string.casting_to, deviceName)
    } else {
        stringResource(Res.string.casting)
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = titleText,
            )
        },
    ) { paddingValues ->
        if (currentUri.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.no_active_cast),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondaryTextColor,
                )
            }
            return@PScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = paddingValues.calculateTopPadding()),
        ) {
            TopSpace()
            CastNowPlayingSection(
                currentUri = currentUri,
                isPlaying = isPlaying,
                progress = progress,
                duration = duration,
                supportsCallback = supportsCallback,
                isLoading = castVM.isLoading.value,
                onPlay = { castVM.playCast() },
                onPause = { castVM.pauseCast() },
                onSeek = { castVM.seekCast(it) },
            )

            if (castItems.size > 1) {
                VerticalSpace(16.dp)
                Text(
                    text = LocaleHelper.getStringF(Res.string.playlist_title, "total", castItems.size),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                )
                castItems.forEachIndexed { index, item ->
                    if (item.path == currentUri) return@forEachIndexed
                    CastSessionPlaylistItem(
                        item = item,
                        onClick = { castVM.cast(item) },
                        onRemove = { castVM.removeCastItemAt(index) },
                    )
                }
            }

            VerticalSpace(24.dp)
            PFilledButton(
                text = stringResource(Res.string.stop_casting),
                onClick = {
                    castVM.stopCast()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                type = ButtonType.DANGER,
                buttonSize = ButtonSize.LARGE,
            )
            BottomSpace(paddingValues)
        }
    }
}
