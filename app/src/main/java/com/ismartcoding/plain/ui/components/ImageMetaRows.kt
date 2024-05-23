package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.ui.base.PListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ImageMetaRows(path: String) {
    val extension = path.getFilenameExtension()
    if (setOf("gif", "svg").contains(extension)) {
        return
    }

    var meta by remember {
        mutableStateOf<DImageMeta?>(null)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            meta = ImageHelper.getMeta(path)
        }
    }
    meta?.let { mt ->
        if (mt.isScreenshot) {
            return
        }
        if (mt.takenAt != null) {
            PListItem(title = stringResource(id = R.string.taken_at), value = mt.takenAt.formatDateTime())
        }
        if (mt.resolutionX > 0 && mt.resolutionY > 0) {
            PListItem(title = stringResource(id = R.string.resolution), value = "${mt.resolutionX} x ${mt.resolutionY}")
        }
        if (mt.make.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.device_make), value = mt.make)
        }
        if (mt.model.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.device_model), value = mt.model)
        }
        if (mt.colorSpace.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.color_profile), value = mt.colorSpace)
        }
        if (mt.apertureValue > 0) {
            PListItem(title = stringResource(id = R.string.aperture_value), value = FormatHelper.formatDouble(mt.apertureValue, 3))
        }
        if (mt.exposureTime.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.exposure_time), value = mt.exposureTime)
        }
        if (mt.focalLength.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.focal_length), value = mt.focalLength)
        }
        if (mt.isoSpeed > 0) {
            PListItem(title = stringResource(id = R.string.iso_speed), value = mt.isoSpeed.toString())
        }
        if (mt.flash > 0) {
            PListItem(title = stringResource(id = R.string.flash), value = ImageHelper.getFlashText(mt.flash))
        }
        if (mt.fNumber > 0) {
            PListItem(title = stringResource(id = R.string.f_number), value = "f/" + FormatHelper.formatDouble(mt.fNumber, 1))
        }
        PListItem(title = stringResource(id = R.string.exposure_program), value = ImageHelper.getExposureProgramText(mt.exposureProgram))
        PListItem(title = stringResource(id = R.string.metering_mode), value = ImageHelper.getMeteringModeText(mt.meteringMode))
        PListItem(title = stringResource(id = R.string.white_balance), value = ImageHelper.getWhiteBalanceText(mt.whiteBalance))
        if (mt.creator.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.creator), value = mt.creator)
        }
        if (mt.description.isNotEmpty()) {
            PListItem(title = stringResource(id = R.string.description), value = mt.description)
        }
    }
}