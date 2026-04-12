package com.ismartcoding.plain.ui.page.dlna

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.models.DlnaReceiverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DlnaReceiverPage(
    navController: NavHostController,
    vm: DlnaReceiverViewModel = viewModel(),
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        vm.startReceiver(context)
        onDispose { vm.stopReceiver(context) }
    }

    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val playbackState by DlnaRendererState.playbackState.collectAsState()
    val hasMedia = mediaUri.isNotEmpty() && playbackState != DlnaPlaybackState.NO_MEDIA_PRESENT

    if (hasMedia) {
        BackHandler {
            DlnaRendererState.mediaUri.value = ""
            DlnaRendererState.playbackState.value = DlnaPlaybackState.NO_MEDIA_PRESENT
        }
        DlnaReceiverVideoPlayer(vm = vm, onExit = {
            DlnaRendererState.mediaUri.value = ""
            DlnaRendererState.playbackState.value = DlnaPlaybackState.NO_MEDIA_PRESENT
        })
        return
    }

    PScaffold(
        topBar = { PTopAppBar(navController = navController, title = stringResource(R.string.dlna_receiver)) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { DlnaReceiverWaitingScreen() }
            item { BottomSpace() }
        }
    }
}
