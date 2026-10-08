package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.call.PhoneLocaleFacts
import com.ismartcoding.plain.features.call.PhoneMetadataFacts
import com.ismartcoding.plain.features.call.PhoneMetadataRequest

internal actual fun phoneLocaleFacts() = PhoneLocaleFacts("", "", false)
internal actual fun phoneMetadata(request: PhoneMetadataRequest) = PhoneMetadataFacts("", "")
