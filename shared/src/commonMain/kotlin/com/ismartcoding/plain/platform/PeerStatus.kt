package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DPeer

expect fun startAwareIfNeeded(): Boolean

expect fun subscribeAwareForPeer(peer: DPeer)
