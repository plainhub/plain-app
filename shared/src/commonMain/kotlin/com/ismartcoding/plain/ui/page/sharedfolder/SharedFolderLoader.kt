package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.db.DSharePeerInfo
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.features.share.SharedInfoDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.lib.mdns.MdnsServiceBrowser
import com.ismartcoding.plain.lib.mdns.MdnsServiceSnapshot
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase
import kotlinx.coroutines.delay

/** A successful [SharedLinkClient.fetchSharedInfo] attempt: payload plus the address that worked. */
internal class FetchResult(val info: SharedInfoDto, val link: SharedLink)

/** Device address candidates for a share card: message endpoint first, then the paired peer record. */
internal fun addressCandidates(msg: DMessageShare): List<SharedLink> {
    val list = mutableListOf(
        SharedLinkClient.linkOf(msg.shareId, msg.urlToken, msg.peerInfo.ip, msg.peerInfo.port),
    )
    PeerCacher.getPeer(msg.peerInfo.id)?.let { peer ->
        if (peer.ip.isNotEmpty() && peer.port > 0) {
            list += SharedLinkClient.linkOf(msg.shareId, msg.urlToken, peer.ip, peer.port)
        }
    }
    return list.distinctBy { "${it.host}:${it.port}" }
}

private fun snapshotDeviceId(snapshot: MdnsServiceSnapshot): String =
    snapshot.txtRecords.firstOrNull { it.startsWith("id=") }?.removePrefix("id=") ?: ""

/**
 * Tries every known address, then falls back to mDNS discovery for the
 * sender's current IP (DHCP changes). Returns null when all attempts fail.
 */
internal suspend fun fetchSharedInfoWithFallback(
    msg: DMessageShare,
    virtualPath: String?,
): FetchResult? {
    suspend fun tryAll(links: List<SharedLink>): FetchResult? {
        for (link in links) {
            val fetched = runCatching { withIO { SharedLinkClient.fetchSharedInfo(link, virtualPath) } }.getOrNull()
            if (fetched != null) return FetchResult(fetched, link)
        }
        return null
    }

    tryAll(addressCandidates(msg))?.let { return it }

    // Stale IP: trigger mDNS queries; the resident listener refreshes the
    // snapshot with the sender's current addresses.
    repeat(12) { round ->
        if (round % 4 == 0) MdnsDiscoverManager.browse()
        delay(700)
        val discovered = MdnsServiceBrowser.snapshot()
            .filter { it.complete && snapshotDeviceId(it) == msg.peerInfo.id }
            .flatMap { snapshot -> snapshot.ips.map { SharedLinkClient.linkOf(msg.shareId, msg.urlToken, it, snapshot.port) } }
        if (discovered.isNotEmpty()) {
            tryAll(discovered)?.let { return it }
        }
    }
    return null
}

/**
 * Pulls the share's current name/expiry (the sender may have edited them
 * after the message was sent) plus the working address into the local
 * message row, so the chat card stays accurate. No-op when nothing changed.
 */
internal suspend fun syncCardBack(messageId: String, old: DMessageShare, result: FetchResult) {
    val fresh = DMessageShare(
        shareId = old.shareId,
        urlToken = old.urlToken,
        peerInfo = DSharePeerInfo(id = old.peerInfo.id, ip = result.link.host, port = result.link.port),
        name = result.info.name.ifEmpty { old.name },
        itemCount = old.itemCount,
        totalSize = old.totalSize,
        expiresAt = result.info.expiresAtInstant ?: old.expiresAt,
    )
    if (fresh.name == old.name && fresh.expiresAt == old.expiresAt &&
        fresh.peerInfo.ip == old.peerInfo.ip && fresh.peerInfo.port == old.peerInfo.port
    ) {
        return
    }
    withIO {
        val chat = ChatManager.getChatItem(messageId) ?: return@withIO
        chat.content.value = fresh
        AppDatabase.instance.chatDao().updateData(ChatItemDataUpdate(messageId, chat.content))
    }
    ChatViewModel.onMessageUpdated(messageId)
}
