package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DPeer

actual suspend fun openAwareByteSocket(peer: DPeer): PeerByteSocket = error("Wi-Fi Aware is unavailable on iOS")
