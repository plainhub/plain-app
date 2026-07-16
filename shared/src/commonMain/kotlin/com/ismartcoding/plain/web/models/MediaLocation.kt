package com.ismartcoding.plain.web.models

/**
 * Parse a media location string of the form "+lat+long" (with optional altitude)
 * into a [Location]. Returns null on failure or malformed input.
 */
internal fun parseMediaLocation(location: String?): Location? {
    try {
        if (location != null) {
            val regex = Regex("([+\\-]\\d{1,3}\\.\\d{4})([+\\-]\\d{1,3}\\.\\d{4})")
            val matchResult = regex.find(location)
            if (matchResult != null) {
                val (latitudeStr, longitudeStr) = matchResult.destructured
                val latitude = latitudeStr.toDouble()
                val longitude = longitudeStr.toDouble()
                return Location(latitude, longitude)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
