package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.models.NearbyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPage(
    navController: NavHostController,
    pairDeviceJson: String = "",
    nearbyVM: NearbyViewModel = viewModel()
) {
    val nearbyDevices = nearbyVM.nearbyDevices
    val isDiscovering by nearbyVM.isDiscovering

    var showQrSheet by remember { mutableStateOf(false) }
    var qrData by remember { mutableStateOf<DQrPairData?>(null) }

    // Auto start discovering when entering the page
    LaunchedEffect(Unit) {
        if (!isDiscovering) {
            nearbyVM.toggleDiscovering()
        }

        // Handle incoming pair request from QR scan (navigated from ScanPage)
        if (pairDeviceJson.isNotEmpty()) {
            try {
                val device = JsonHelper.jsonDecode<DQrPairData>(pairDeviceJson).toDNearbyDevice()
                nearbyVM.startPairingFromDevice(device)
            } catch (e: Exception) {
                // ignore parse errors
            }
        }

    }

    // Load QR data when the sheet is about to be shown
    LaunchedEffect(showQrSheet) {
        if (showQrSheet && qrData == null) {
            withIO {
                qrData = nearbyVM.getQrDataAsync()
            }
        }
    }

    // Stop discovering when leaving the page
    DisposableEffect(Unit) {
        onDispose {
            if (isDiscovering) {
                nearbyVM.toggleDiscovering()
            }
        }
    }


    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                title = stringResource(R.string.nearby_devices),
                actions = {
                    PIconButton(
                        icon = R.drawable.qr_code,
                        contentDescription = stringResource(R.string.show_qr_code),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        qrData = null
                        showQrSheet = true
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            nearbySearchingItem()
            nearbyDeviceListItems(nearbyDevices, nearbyVM)
            item {
                BottomSpace(paddingValues)
            }
        }
    }

    if (showQrSheet) {
        NearbyQrBottomSheet(
            qrData = qrData,
            onDismiss = { showQrSheet = false },
        )
    }
}

