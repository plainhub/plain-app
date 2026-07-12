package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DImageMeta

actual fun formatImageMetaTexts(mt: DImageMeta): ImageMetaTexts =
    ImageMetaTexts(
        flash = "",
        exposureProgram = "",
        meteringMode = "",
        whiteBalance = "",
    )
