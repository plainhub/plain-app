package com.ismartcoding.plain.events

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.lib.ChannelEvent

// Emitted after media rows are trashed/restored/deleted so the sidebar
// (folder counts, trash badge) refreshes without a page re-enter.
class MediaStoreChangedEvent(val dataType: DataType) : ChannelEvent()
