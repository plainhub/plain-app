package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.ScreenHelper
import com.ismartcoding.plain.preferences.LocalKeepScreenOn
import com.ismartcoding.plain.ui.components.DeviceRenameDialog
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.root.home.HomeFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RootNavigationDrawerContent(
    navController: NavHostController,
    selectedTab: Int,
    onCloseDrawer: () -> Unit,
    onTabSelected: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keepScreenOn = LocalKeepScreenOn.current
    var showRenameDialog by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        deviceName = TempData.deviceName
    }

    if (showRenameDialog) {
        DeviceRenameDialog(
            name = deviceName,
            onDismiss = { showRenameDialog = false },
            onDone = {
                deviceName = TempData.deviceName
            },
        )
    }

    val items = listOf(
        DrawerItem(RootPageType.HOME.value, R.drawable.house, R.string.phone_web_portal),
        DrawerItem(RootPageType.CHAT.value, R.drawable.message_circle, R.string.chat),
        DrawerItem(RootPageType.IMAGES.value, R.drawable.image, R.string.images),
        DrawerItem(RootPageType.AUDIO.value, R.drawable.music, R.string.audios),
        DrawerItem(RootPageType.VIDEOS.value, R.drawable.video, R.string.videos),
    )

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            DrawerHeader(
                deviceName = deviceName,
                navController = navController,
                onRenameClick = { showRenameDialog = true },
                onCloseDrawer = onCloseDrawer,
            )

            Spacer(modifier = Modifier.height(10.dp))

            DrawerTabs(items = items, selectedTab = selectedTab, onTabSelected = onTabSelected)

            Spacer(modifier = Modifier.height(8.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val itemWidth = ((maxWidth - 34.dp) / 3f).coerceAtLeast(72.dp)
                HomeFeatures(
                    navController = navController,
                    itemWidth = itemWidth,
                    onNavigate = { pageType ->
                        onCloseDrawer()
                        onTabSelected(pageType.value)
                    },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            DrawerBottomActions(
                onKeepScreenOnClick = {
                    scope.launch(Dispatchers.IO) {
                        ScreenHelper.keepScreenOnAsync(context, !keepScreenOn)
                    }
                },
                onScanClick = {
                    onCloseDrawer()
                    navController.navigate(Routing.Scan)
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}