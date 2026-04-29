package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.components.DeviceRenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarHome(navController: NavHostController) {
    val context = LocalContext.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { deviceName = TempData.deviceName }

    if (showRenameDialog) {
        DeviceRenameDialog(
            name = deviceName,
            onDismiss = { showRenameDialog = false },
            onDone = { deviceName = TempData.deviceName },
        )
    }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = deviceName.ifEmpty { PhoneHelper.getDeviceName(context) },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = { showRenameDialog = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pen),
                        contentDescription = stringResource(R.string.device_name),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            ActionButtonMoreWithMenu { dismiss ->
                HomeQuickMenuItems(
                    navController = navController,
                    onDismiss = { dismiss() },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}