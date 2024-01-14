package com.ismartcoding.lib.rss

import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


object DateParser {
    private var ADDITIONAL_MASKS: Array<String> = arrayOf("yyyy-MM-dd HH:mm:ss")

    // order is like this because the SimpleDateFormat.parse does not fail with exception if it can
    // parse a valid date out of a substring of the full string given the mask so we have to check
    // the most complete format first, then it fails with exception
    private val RFC822_MASKS = arrayOf(
        "EEE, dd MMM yy HH:mm:ss z",
        "EEE, dd MMM yy HH:mm z",
        "dd MMM yy HH:mm:ss z",
        "dd MMM yy HH:mm z")

    // order is like this because the SimpleDateFormat.parse does not fail with exception if it can
    // parse a valid date out of a substring of the full string given the mask so we have to check
    // the most complete format first, then it fails with exception
    private val W3CDATETIME_MASKS = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSz", "yyyy-MM-dd't'HH:mm:ss.SSSz", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd't'HH:mm:ss.SSS'z'", "yyyy-MM-dd'T'HH:mm:ssz", "yyyy-MM-dd't'HH:mm:ssz", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd't'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd't'HH:mm:ss'z'", "yyyy-MM-dd'T'HH:mmz",  // together
        // with
        // logic
        // in
        // the
        // parseW3CDateTime
        // they
        "yyyy-MM'T'HH:mmz",  // handle W3C dates without time forcing them to
        // be GMT
        "yyyy'T'HH:mmz", "yyyy-MM-dd't'HH:mmz", "yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd't'HH:mm'z'", "yyyy-MM-dd", "yyyy-MM", "yyyy"
    )

    /**
     * The masks used to validate and parse the input to this Atom date. These are a lot more
     * forgiving than what the Atom spec allows. The forms that are invalid according to the spec
     * are indicated.
     */
    private val masks = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSz", "yyyy-MM-dd't'HH:mm:ss.SSSz",  // invalid
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd't'HH:mm:ss.SSS'z'",  // invalid
        "yyyy-MM-dd'T'HH:mm:ssz", "yyyy-MM-dd't'HH:mm:ssz",  // invalid
        "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd't'HH:mm:ss'z'",  // invalid
        "yyyy-MM-dd'T'HH:mmz",  // invalid
        "yyyy-MM-dd't'HH:mmz",  // invalid
        "yyyy-MM-dd'T'HH:mm'Z'",  // invalid
        "yyyy-MM-dd't'HH:mm'z'",  // invalid
        "yyyy-MM-dd", "yyyy-MM", "yyyy"
    )

    /**
     * Parses a Date out of a string using an array of masks.
     *
     *
     * It uses the masks in order until one of them succedes or all fail.
     *
     *
     *
     * @param masks array of masks to use for parsing the string
     * @param input string to parse for a date.
     * @return the Date represented by the given string using one of the given masks. It returns
     * **null** if it was not possible to parse the the string with any of the masks.
     */
    private fun parseUsingMask(masks: Array<String>, input: String, locale: Locale): Date? {
        val newDate = input.trim()
        var pp: ParsePosition?
        var d: Date? = null
        var i = 0
        while (d == null && i < masks.size) {
            val df: DateFormat = SimpleDateFormat(masks[i].trim { it <= ' ' }, locale)
            // df.setLenient(false);
            df.isLenient = true
            try {
                pp = ParsePosition(0)
                d = df.parse(newDate, pp)
                if (pp.index != newDate.length) {
                    d = null
                }
            } catch (ex1: Exception) {
            }
            i++
        }
        return d
    }

    /**
     * Parses a Date out of a String with a date in RFC822 format.
     *
     *
     * It parsers the following formats:
     *
     *  * "EEE, dd MMM yyyy HH:mm:ss z"
     *  * "EEE, dd MMM yyyy HH:mm z"
     *  * "EEE, dd MMM yy HH:mm:ss z"
     *  * "EEE, dd MMM yy HH:mm z"
     *  * "dd MMM yyyy HH:mm:ss z"
     *  * "dd MMM yyyy HH:mm z"
     *  * "dd MMM yy HH:mm:ss z"
     *  * "dd MMM yy HH:mm z"
     *
     *
     *
     * Refer to the java.text.SimpleDateFormat javadocs for details on the format of each element.
     *
     *
     *
     * @param input string to parse for a date.
     * @return the Date represented by the given RFC822 string. It returns **null** if it was not
     * possible to parse the given string into a Date.
     */
    fun parseRFC822(input: String, locale: Locale): Date? {
        return parseUsingMask(RFC822_MASKS,  convertUnsupportedTimeZones(input), locale)
    }

    private fun convertUnsupportedTimeZones(input: String): String {
        val unsupportedZeroOffsetTimeZones: List<String> = mutableListOf("UT", "Z")
        val splitted = input.split(" ")
        for (timeZone in unsupportedZeroOffsetTimeZones) {
            if (splitted.contains(timeZone)) {
                return replaceLastOccurrence(input, timeZone, "UTC")
            }
        }
        return input
    }

    private fun replaceLastOccurrence(original: String, target: String, replacement: String): String {
        val lastIndexOfTarget = original.lastIndexOf(target)
        return if (lastIndexOfTarget == -1) {
            original
        } else {
            StringBuilder(original)
                .replace(lastIndexOfTarget, lastIndexOfTarget + target.length, replacement)
                .toString()
        }
    }

    /**
     * Parses a Date out of a String with a date in W3C date-time format.
     *
     *
     * It parsers the following formats:
     *
     *  * "yyyy-MM-dd'T'HH:mm:ssz"
     *  * "yyyy-MM-dd'T'HH:mmz"
     *  * "yyyy-MM-dd"
     *  * "yyyy-MM"
     *  * "yyyy"
     *
     *
     *
     * Refer to the java.text.SimpleDateFormat javadocs for details on the format of each element.
     *
     *
     *
     * @param input string to parse for a date.
     * @return the Date represented by the given W3C date-time string. It returns **null** if it
     * was not possible to parse the given string into a Date.
     */
    fun parseW3CDateTime(input: String, locale: Locale): Date? {
        // if sDate has time on it, it injects 'GTM' before de TZ displacement to allow the
        // SimpleDateFormat parser to parse it properly
        var sDate = input
        val tIndex = sDate.indexOf("T")
        if (tIndex > -1) {
            if (sDate.endsWith("Z")) {
                sDate = sDate.substring(0, sDate.length - 1) + "+00:00"
            }
            var tzdIndex = sDate.indexOf("+", tIndex)
            if (tzdIndex == -1) {
                tzdIndex = sDate.indexOf("-", tIndex)
            }
            if (tzdIndex > -1) {
                var pre = sDate.substring(0, tzdIndex)
                val secFraction = pre.indexOf(",")
                if (secFraction > -1) {
                    pre = pre.substring(0, secFraction)
                }
                val post = sDate.substring(tzdIndex)
                sDate = pre + "GMT" + post
            }
        } else {
            sDate += "T00:00GMT"
        }
        return parseUsingMask(W3CDATETIME_MASKS, sDate, locale)
    }

    /**
     * Parses a Date out of a String with a date in W3C date-time format or in a RFC822 format.
     *
     * @param sDate string to parse for a date.
     * @return the Date represented by the given W3C date-time string. It returns **null** if it
     * was not possible to parse the given string into a Date.
     *
     */
    fun parseDate(sDate: String, locale: Locale): Date? {
        var date: Date?
        if (ADDITIONAL_MASKS.isNotEmpty()) {
            date = parseUsingMask(ADDITIONAL_MASKS, sDate, locale)
            if (date != null) {
                return date
            }
        }
        date = parseW3CDateTime(sDate, locale)
        if (date == null) {
            date = parseRFC822(sDate, locale)
        }
        return date
    }

    /**
     * create a RFC822 representation of a date.
     *
     *
     * Refer to the java.text.SimpleDateFormat javadocs for details on the format of each element.
     *
     *
     *
     * @param date Date to parse
     * @return the RFC822 represented by the given Date It returns **null** if it was not
     * possible to parse the date.
     */
    fun formatRFC822(date: Date, locale: Locale): String {
        val dateFormater = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", locale)
        dateFormater.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormater.format(date)
    }

    /**
     * create a W3C Date Time representation of a date.
     *
     *
     * Refer to the java.text.SimpleDateFormat javadocs for details on the format of each element.
     *
     *
     *
     * @param date Date to parse
     * @return the W3C Date Time represented by the given Date It returns **null** if it was not
     * possible to parse the date.
     */
    fun formatW3CDateTime(date: Date, locale: Locale): String {
        val dateFormater = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", locale)
        dateFormater.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormater.format(date)
    }
}