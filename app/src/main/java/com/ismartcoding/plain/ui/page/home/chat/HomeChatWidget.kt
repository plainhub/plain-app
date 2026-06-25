package com.ismartcoding.plain.ui.page.home.chat

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.preferences.HomeSectionCollapsedPreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
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
    showOnlineStatus: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val collapsed = remember {
        context.dataStore.dataFlow.map { HomeSectionCollapsedPreference.get(it, AppFeatureType.CHAT) }
    }.collectAsStateValue(initial = false)
    val channels = ChannelCacher.channels.collectAsStateValue()
    val pairedPeers = PeerCacher.pairedPeers.collectAsStateValue()

    val localChat = ChatCacher.getLatestChat("local")
    val onlineText = stringResource(Res.string.online)
    val localRow = ChatRow(
        sortAt = localChat?.createdAt ?: Instant.DISTANT_PAST,
        title = stringResource(Res.string.local_chat),
        desc = stringResource(Res.string.local_chat_desc),
        icon = Res.drawable.bot,
        online = null,
        createdAt = localChat?.createdAt ?: Instant.DISTANT_PAST,
        latestChat = localChat,
        route = Routing.Chat("peer:local"),
    )
    val peerRows = pairedPeers
        .map { peer ->
            val latestChat = ChatCacher.getLatestChat(peer.id)
            val online = PeerCacher.isPeerOnline(peer.id)
            ChatRow(
                sortAt = latestChat?.createdAt ?: Instant.DISTANT_PAST,
                title = peer.name,
                desc = if (online) onlineText else peer.getBestIp(),
                icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                online = if (showOnlineStatus) online else null,
                createdAt = peer.createdAt,
                latestChat = latestChat,
                route = Routing.Chat("peer:${peer.id}"),
            )
        }
    val channelRows = channels
        .filter { it.status == DChatChannel.STATUS_JOINED }
        .map { channel ->
            val latestChat = ChatCacher.getLatestChat(channel.id)
            ChatRow(
                sortAt = latestChat?.createdAt ?: channel.updatedAt,
                title = channel.name,
                desc = "",
                icon = Res.drawable.hash,
                online = null,
                createdAt = channel.createdAt,
                latestChat = latestChat,
                route = Routing.Chat("channel:${channel.id}"),
            )
        }
    val rowComparator = compareByDescending<ChatRow> { it.sortAt }
        .thenByDescending { it.online == true }
        .thenByDescending { it.createdAt }
    val rows = (listOf(localRow) + (peerRows + channelRows).sortedWith(rowComparator).take(4))
        .sortedWith(rowComparator)

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
        color = MaterialTheme.colorScheme.cardBackgroundNormal,
    ) {
        Column {
            HomeSectionClickableHeader(
                title = stringResource(Res.string.chat),
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
