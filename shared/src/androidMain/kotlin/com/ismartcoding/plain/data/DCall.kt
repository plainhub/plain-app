package com.ismartcoding.plain.data

import com.ismartcoding.plain.features.call.PhoneGeoCache
import com.ismartcoding.plain.web.models.PhoneGeo

fun DCall.getGeo(): PhoneGeo? = PhoneGeoCache.lookup(number)
