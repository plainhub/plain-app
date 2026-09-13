package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.stringPreferencesKey

// Selected whisper model id ("tiny" | "base" | "small"); "base" balances
// size and accuracy out of the box.
object WhisperModelPreference : BasePreference<String>() {
    override val default = "base"
    override val key = stringPreferencesKey("whisper_model")
}
