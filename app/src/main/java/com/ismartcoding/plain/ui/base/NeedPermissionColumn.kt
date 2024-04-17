package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DFeaturePermission
import com.ismartcoding.plain.features.RequestPermissionsEvent

@Composable
fun NeedPermissionColumn(permission: DFeaturePermission) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = permission.permission.getGrantAccessText(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpace(dp = 16.dp)
            Button(onClick = {
                sendEvent(RequestPermissionsEvent(*permission.permissions.toTypedArray()))
            }) {
                Text(text = stringResource(id = R.string.grant_access))
            }
        }
    }
}
