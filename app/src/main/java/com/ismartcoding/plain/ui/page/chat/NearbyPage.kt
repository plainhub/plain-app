package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R

import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon

import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.ui.page.chat.components.NearbyDeviceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPage(
    navController: NavHostController,
    nearbyVM: NearbyViewModel = viewModel()
) {
    val nearbyDevices = nearbyVM.nearbyDevices
    val isDiscovering by nearbyVM.isDiscovering

    // Auto start discovering when entering the page
    LaunchedEffect(Unit) {
        if (!isDiscovering) {
            nearbyVM.toggleDiscovering()
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
                title = stringResource(R.string.nearby_devices)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalSpace(8.dp)
                        Text(
                            text = stringResource(R.string.searching_nearby_devices),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            if (nearbyDevices.isNotEmpty()) {
                item {
                    VerticalSpace(16.dp)
                    Subtitle(stringResource(R.string.nearby_devices))
                }
                nearbyDevices.forEach { item ->
                    item {
                        val isPaired = nearbyVM.isPaired(item.id)
                        val isPairing = nearbyVM.isPairing(item.id)
                        NearbyDeviceItem(
                            item = item,
                            isPaired = isPaired,
                            isPairing = isPairing,
                            onPairClick = {
                                nearbyVM.startPairing(item)
                            },
                            onUnpairClick = {
                                nearbyVM.unpairDevice(item.id)
                            },
                            onCancelClick = {
                                nearbyVM.cancelPairing(item.id)
                            }
                        )
                        VerticalSpace(8.dp)
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.make_sure_devices_same_network),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                BottomSpace(paddingValues)
            }
        }
    }
}
