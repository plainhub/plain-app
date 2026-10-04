package com.ismartcoding.plain.data

import com.ismartcoding.plain.db.DPeer

data class DPairingOutcome(val ticket: DPairingTicket, val peer: DPeer?, val error: String)
