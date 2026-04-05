package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.ui.page.chat.components.NearbyDeviceItem

internal fun LazyListScope.nearbySearchingItem() {
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
}

internal fun LazyListScope.nearbyDeviceListItems(
    nearbyDevices: List<DNearbyDevice>,
    nearbyVM: NearbyViewModel,
) {
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
                    onPairClick = { nearbyVM.startPairing(item) },
                    onUnpairClick = { nearbyVM.unpairDevice(item.id) },
                    onCancelClick = { nearbyVM.cancelPairing(item.id) }
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}
