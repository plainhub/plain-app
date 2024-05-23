package com.ismartcoding.plain.data

import kotlinx.datetime.Instant

data class DImageMeta(
    val make: String,
    val model: String,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val colorSpace : String,
    val apertureValue: Double,
    val exposureTime: String,
    val focalLength : String,
    val isoSpeed: Int,
    val takenAt: Instant?,
    val flash: Int,
    val fNumber: Double,
    val exposureProgram: Int,
    val meteringMode: Int,
    val whiteBalance: Int,
    val creator: String,
    val resolutionX: Int,
    val resolutionY: Int,
    val description: String
) {
    val isScreenshot: Boolean
        get() = exposureTime.isEmpty()
}