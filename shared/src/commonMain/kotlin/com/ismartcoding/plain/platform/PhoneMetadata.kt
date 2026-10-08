package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.call.PhoneLocaleFacts
import com.ismartcoding.plain.features.call.PhoneMetadataFacts
import com.ismartcoding.plain.features.call.PhoneMetadataRequest

internal expect fun phoneLocaleFacts(): PhoneLocaleFacts
internal expect fun phoneMetadata(request: PhoneMetadataRequest): PhoneMetadataFacts
