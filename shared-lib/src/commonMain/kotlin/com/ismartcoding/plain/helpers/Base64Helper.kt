package com.ismartcoding.plain.helpers

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Accepts both padded and unpadded input on decode, matching android.util.Base64 decode leniency.
// Encode always produces padded output (standard Base64).
@OptIn(ExperimentalEncodingApi::class)
val Base64Lenient: Base64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
