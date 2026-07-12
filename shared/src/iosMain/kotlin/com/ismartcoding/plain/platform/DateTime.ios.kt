@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.platform.LocaleHelper
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.localeWithLocaleIdentifier
import kotlin.time.Instant

private const val EPOCH_TO_REFERENCE_DATE_SECONDS = 978307200.0

actual fun Instant.formatDateTime(): String {
    val formatter = NSDateFormatter()
    val locale = LocaleHelper.currentLocale()
    formatter.locale = NSLocale.localeWithLocaleIdentifier("${locale.language}_${locale.country}")
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.timeStyle = NSDateFormatterShortStyle
    return formatter.stringFromDate(NSDate(timeIntervalSinceReferenceDate = epochSeconds.toDouble() - EPOCH_TO_REFERENCE_DATE_SECONDS))
}

actual fun Instant.formatDate(): String {
    val formatter = NSDateFormatter()
    val locale = LocaleHelper.currentLocale()
    formatter.locale = NSLocale.localeWithLocaleIdentifier("${locale.language}_${locale.country}")
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.timeStyle = NSDateFormatterNoStyle
    return formatter.stringFromDate(NSDate(timeIntervalSinceReferenceDate = epochSeconds.toDouble() - EPOCH_TO_REFERENCE_DATE_SECONDS))
}

actual fun Instant.formatTime(): String {
    val formatter = NSDateFormatter()
    val locale = LocaleHelper.currentLocale()
    formatter.locale = NSLocale.localeWithLocaleIdentifier("${locale.language}_${locale.country}")
    formatter.dateStyle = NSDateFormatterNoStyle
    formatter.timeStyle = NSDateFormatterShortStyle
    return formatter.stringFromDate(NSDate(timeIntervalSinceReferenceDate = epochSeconds.toDouble() - EPOCH_TO_REFERENCE_DATE_SECONDS))
}

actual fun Instant.formatName(): String {
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
    formatter.dateFormat = InstantFormats.FILE_NAME_DATE_FORMAT
    return formatter.stringFromDate(NSDate(timeIntervalSinceReferenceDate = epochSeconds.toDouble() - EPOCH_TO_REFERENCE_DATE_SECONDS))
}
