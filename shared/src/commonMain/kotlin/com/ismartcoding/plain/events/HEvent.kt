package com.ismartcoding.plain.events

import com.ismartcoding.plain.lib.ChannelEvent

// HTTP-originated events are consumed locally, never uploaded as WebSocket events.
abstract class HEvent : ChannelEvent()
