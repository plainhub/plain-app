package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DPeer

actual fun startAwareIfNeeded(): Boolean = false

actual fun subscribeAwareForPeer(peer: DPeer) {}
