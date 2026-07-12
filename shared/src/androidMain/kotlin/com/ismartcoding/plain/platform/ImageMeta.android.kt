package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DImageMeta
import com.ismartcoding.plain.helpers.ImageHelper

actual fun formatImageMetaTexts(mt: DImageMeta): ImageMetaTexts =
    ImageMetaTexts(
        flash = ImageHelper.getFlashText(mt.flash),
        exposureProgram = ImageHelper.getExposureProgramText(mt.exposureProgram),
        meteringMode = ImageHelper.getMeteringModeText(mt.meteringMode),
        whiteBalance = ImageHelper.getWhiteBalanceText(mt.whiteBalance),
    )
