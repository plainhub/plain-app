package com.ismartcoding.plain.platform

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.PListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ImageMetaTexts(
    val flash: String,
    val exposureProgram: String,
    val meteringMode: String,
    val whiteBalance: String,
)

/**
 * Format EXIF integer enums into localized display strings.
 * Android: backed by androidx.exifinterface ExifInterface constants.
 * iOS: returns empty strings (stub until a native EXIF reader is available).
 */
expect fun formatImageMetaTexts(mt: DImageMeta): ImageMetaTexts

@Composable
fun ImageMetaRows(
    path: String,
    loadMeta: suspend (String) -> DImageMeta?,
    formatTexts: (DImageMeta) -> ImageMetaTexts,
) {
    val extension = path.getFilenameExtension()
    if (setOf("gif", "svg").contains(extension)) {
        return
    }

    var meta by remember {
        mutableStateOf<DImageMeta?>(null)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.Default) {
            meta = loadMeta(path)
        }
    }
    meta?.let { mt ->
        if (mt.isScreenshot) {
            return
        }
        val texts = formatTexts(mt)
        mt.takenAt?.let { takenAt ->
            PListItem(title = stringResource(Res.string.taken_at), value = takenAt.formatDateTime())
        }
        if (mt.resolutionX > 0 && mt.resolutionY > 0) {
            PListItem(title = stringResource(Res.string.resolution), value = "${mt.resolutionX} x ${mt.resolutionY}")
        }
        if (mt.make.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.device_make), value = mt.make)
        }
        if (mt.model.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.device_model), value = mt.model)
        }
        if (mt.colorSpace.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.color_profile), value = mt.colorSpace)
        }
        if (mt.apertureValue > 0) {
            PListItem(title = stringResource(Res.string.aperture_value), value = FormatHelper.formatDouble(mt.apertureValue, 3))
        }
        if (mt.exposureTime.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.exposure_time), value = mt.exposureTime)
        }
        if (mt.focalLength.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.focal_length), value = mt.focalLength)
        }
        if (mt.isoSpeed > 0) {
            PListItem(title = stringResource(Res.string.iso_speed), value = mt.isoSpeed.toString())
        }
        if (mt.flash > 0) {
            PListItem(title = stringResource(Res.string.flash), value = texts.flash)
        }
        if (mt.fNumber > 0) {
            PListItem(title = stringResource(Res.string.f_number), value = "f/" + FormatHelper.formatDouble(mt.fNumber, 1))
        }
        PListItem(title = stringResource(Res.string.exposure_program), value = texts.exposureProgram)
        PListItem(title = stringResource(Res.string.metering_mode), value = texts.meteringMode)
        PListItem(title = stringResource(Res.string.white_balance), value = texts.whiteBalance)
        if (mt.creator.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.creator), value = mt.creator)
        }
        if (mt.description.isNotEmpty()) {
            PListItem(title = stringResource(Res.string.description), value = mt.description)
        }
    }
}
