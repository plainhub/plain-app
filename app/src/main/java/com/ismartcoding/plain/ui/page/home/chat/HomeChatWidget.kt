package com.ismartcoding.plain.ui.page.home.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.preferences.HomeSectionCollapsedPreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.chat.components.PeerListItem
import com.ismartcoding.plain.ui.page.home.HomeSectionClickableHeader
import com.ismartcoding.plain.ui.page.home.HomeSectionCollapseButton
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

@Composable
fun HomeChatWidget(
    navController: NavHostController,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val collapsed = remember {
        context.dataStore.dataFlow.map { HomeSectionCollapsedPreference.get(it, AppFeatureType.CHAT) }
    }.collectAsStateValue(initial = false)
    val channels = channelVM.channels.collectAsStateValue()

    val localChat = peerVM.getLatestChat("local")
    val onlineText = stringResource(R.string.online)
    val channelsText = stringResource(R.string.channels)
    val localRow = ChatRow(
        sortAt = localChat?.createdAt ?: Instant.DISTANT_PAST,
        title = stringResource(R.string.local_chat),
        desc = stringResource(R.string.local_chat_desc),
        icon = R.drawable.bot,
        online = null,
        latestChat = localChat,
        route = Routing.Chat("local"),
    )
    val peerRows = peerVM.pairedPeers
        .filter { peerVM.isPeerOnline(it.id) }
        .map { peer ->
            val latestChat = peerVM.getLatestChat(peer.id)
            ChatRow(
                sortAt = latestChat?.createdAt ?: peer.updatedAt,
                title = peer.name,
                desc = onlineText,
                icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                online = true,
                latestChat = latestChat,
                route = Routing.Chat("peer:${peer.id}"),
            )
        }
    val channelRows = channels
        .filter { it.status == DChatChannel.STATUS_JOINED }
        .map { channel ->
            val latestChat = peerVM.getLatestChat(channel.id)
            ChatRow(
                sortAt = latestChat?.createdAt ?: channel.updatedAt,
                title = channel.name,
                desc = channelsText,
                icon = R.drawable.hash,
                online = null,
                latestChat = latestChat,
                route = Routing.Chat("channel:${channel.id}"),
            )
        }
    val rows = (listOf(localRow) + (peerRows + channelRows).sortedByDescending { it.sortAt }.take(4))
        .sortedByDescending { it.sortAt }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
        color = MaterialTheme.colorScheme.cardBackgroundNormal,
    ) {
        Column {
            HomeSectionClickableHeader(
                title = stringResource(R.string.chat),
                onClick = { navController.navigate(Routing.ChatList) },
                trailingContent = { HomeSectionCollapseButton(collapsed = collapsed, featureType = AppFeatureType.CHAT) },
            )

            if (!collapsed) {
                rows.forEachIndexed { index, row ->
                    PeerListItem(
                        title = row.title,
                        desc = row.desc,
                        icon = row.icon,
                        online = row.online,
                        latestChat = row.latestChat,
                        onClick = { navController.navigate(row.route) },
                    )
                }
            }
        }
    }
}
