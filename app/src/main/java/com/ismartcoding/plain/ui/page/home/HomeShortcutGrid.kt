package com.ismartcoding.plain.ui.page.home
import com.ismartcoding.plain.preferences.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.preferences.HomeFeaturesPreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.home.chat.HomeChatWidget
import kotlinx.coroutines.flow.map

@Composable
fun HomeShortcutGrid(
    navController: NavHostController,
    peerVM: PeerViewModel,
    showOnlineStatus: Boolean,
) {
    val context = LocalContext.current
    val featuresStr = remember {
        context.dataStore.dataFlow.map { HomeFeaturesPreference.get(it) }
    }.collectAsStateValue(initial = HomeFeaturesPreference.default)
    val enabledIds = remember(featuresStr) {
        HomeFeaturesPreference.parseList(featuresStr.ifEmpty { HomeFeaturesPreference.default })
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (AppFeatureType.CHAT.name in enabledIds) {
            HomeChatWidget(navController, peerVM, showOnlineStatus)
        }
        HomeFeatureItemsGrid(navController)
    }
}
