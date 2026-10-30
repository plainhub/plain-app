package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.platform.system.Capability

internal fun hasTools(capabilities: Collection<Capability>, localToolsAllowed: Boolean = true): Boolean = capabilities.any {
    when (it) {
        Capability.FILE_BROWSER, Capability.IMAGE_LIBRARY, Capability.AUDIO_LIBRARY,
        Capability.VIDEO_LIBRARY, Capability.DOCUMENT_LIBRARY,
        Capability.NOTES, Capability.FEEDS, Capability.PACKAGES -> true
        Capability.SOUND_METER, Capability.POMODORO, Capability.IMAGE_EDITOR -> localToolsAllowed
        else -> false
    }
}
