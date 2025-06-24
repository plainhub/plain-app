package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.ui.page.chat.components.NearbyDeviceItem
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPage(
    navController: NavHostController,
    nearbyVM: NearbyViewModel = viewModel()
) {
    val context = LocalContext.current
    val nearbyDevices = nearbyVM.nearbyDevices
    val isDiscovering by nearbyVM.isDiscovering

    val isDiscoverable = remember {
        context.dataStore.dataFlow.map {
            NearbyDiscoverablePreference.get(it)
        }
    }.collectAsStateValue(initial = NearbyDiscoverablePreference.default)


    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                navigationIcon = {
                    NavigationBackIcon {
                        navController.popBackStack()
                    }
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
                PCard {
                    PListItem(
                        title = stringResource(R.string.make_discoverable),
                        subtitle = stringResource(R.string.make_discoverable_desc),
                        icon = R.drawable.wifi,
                        action = {
                            Switch(
                                checked = isDiscoverable,
                                onCheckedChange = { enabled ->
                                    nearbyVM.updateDiscoverable(context, enabled)
                                }
                            )
                        }
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    ) {
                        Button(
                            onClick = {
                                nearbyVM.toggleDiscovering()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isDiscovering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    HorizontalSpace(8.dp)
                                    Text(stringResource(R.string.stop_discovering))
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.search),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    HorizontalSpace(8.dp)
                                    Text(stringResource(R.string.discover_nearby_devices))
                                }
                            }
                        }
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
            } else if (isDiscovering) {
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
                                text = stringResource(R.string.searching_nearby_devices),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            VerticalSpace(8.dp)
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
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            VerticalSpace(16.dp)
                            Text(
                                text = stringResource(R.string.tap_discover_to_start),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                BottomSpace()
            }
        }
    }
}
