package com.ismartcoding.plain.platform

import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.db.DPeer

actual suspend fun openAwareByteSocket(peer: DPeer): PeerByteSocket = WifiAwareTransport.openSocket(peer)
