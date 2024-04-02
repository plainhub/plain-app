package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import kotlin.math.abs
import kotlin.math.roundToLong

object TimeAgoHelper {
    fun getString(ms: Long): String {
        val dim = getTimeDistanceInMinutes(ms)
        val timeAgo = StringBuilder()

        val foundTimePeriod = Periods.findByDistanceMinutes(dim)
        if (foundTimePeriod != null) {
            val periodKey = foundTimePeriod.propertyKey
            when (foundTimePeriod) {
                Periods.X_MINUTES_PAST -> timeAgo.append(MainApp.instance.getString(periodKey, dim))
                Periods.X_HOURS_PAST -> {
                    val hours = (dim / 60f).roundToLong()
                    val xHoursText = handlePeriodKeyAsPlural(
                        R.string.timeago_aboutanhour_past, periodKey, hours.toInt()
                    )
                    timeAgo.append(xHoursText)
                }

                Periods.X_DAYS_PAST -> {
                    val days = (dim / 1440f).roundToLong()
                    val xDaysText = handlePeriodKeyAsPlural(
                        R.string.timeago_oneday_past, periodKey, days.toInt()
                    )
                    timeAgo.append(xDaysText)
                }

                Periods.X_WEEKS_PAST -> {
                    val weeks = (dim / 10080f).roundToLong()
                    val xWeeksText = handlePeriodKeyAsPlural(
                        R.string.timeago_oneweek_past, periodKey, weeks.toInt()
                    )
                    timeAgo.append(xWeeksText)
                }

                Periods.X_MONTHS_PAST -> {
                    val months = (dim / 43200f).roundToLong()
                    val xMonthsText = handlePeriodKeyAsPlural(
                        R.string.timeago_aboutamonth_past, periodKey, months.toInt()
                    )
                    timeAgo.append(xMonthsText)
                }

                Periods.X_YEARS_PAST -> {
                    val years = (dim / 525600f).roundToLong()
                    timeAgo.append(MainApp.instance.getString(periodKey, years))
                }

                Periods.X_MINUTES_FUTURE -> timeAgo.append(MainApp.instance.getString(periodKey, abs(dim.toFloat())))
                Periods.X_HOURS_FUTURE -> {
                    val hours1 = abs((dim / 60f).roundToLong())
                    val yHoursText = if (hours1.toInt() == 24)
                        R.string.timeago_oneday_future
                    else
                        handlePeriodKeyAsPlural(
                            R.string.timeago_aboutanhour_future,
                            periodKey, hours1.toInt()
                        )
                    timeAgo.append(yHoursText)
                }

                Periods.X_DAYS_FUTURE -> {
                    val days1 = abs((dim / 1440f).roundToLong())
                    val yDaysText = handlePeriodKeyAsPlural(
                        R.string.timeago_oneday_future, periodKey, days1.toInt()
                    )
                    timeAgo.append(yDaysText)
                }

                Periods.X_WEEKS_FUTURE -> {
                    val weeks1 = abs((dim / 10080f).roundToLong())
                    val yWeeksText = handlePeriodKeyAsPlural(
                        R.string.timeago_oneweek_future, periodKey, weeks1.toInt()
                    )
                    timeAgo.append(yWeeksText)
                }

                Periods.X_MONTHS_FUTURE -> {
                    val months1 = abs((dim / 43200f).roundToLong())
                    val yMonthsText = if (months1.toInt() == 12)
                        R.string.timeago_aboutayear_future
                    else
                        handlePeriodKeyAsPlural(
                            R.string.timeago_aboutamonth_future, periodKey, months1.toInt()
                        )
                    timeAgo.append(yMonthsText)
                }

                Periods.X_YEARS_FUTURE -> {
                    val years1 = abs((dim / 525600f).roundToLong())
                    timeAgo.append(MainApp.instance.getString(periodKey, years1))
                }

                else -> timeAgo.append(MainApp.instance.getString(periodKey))
            }
        }

        return timeAgo.toString()
    }

    private fun handlePeriodKeyAsPlural(periodKey: Int, pluralKey: Int, value: Int): String {
        return if (value == 1) {
            MainApp.instance.getString(periodKey)
        } else {
            MainApp.instance.getString(pluralKey, value)
        }
    }

    private fun getTimeDistanceInMinutes(ms: Long): Long {
        return (System.currentTimeMillis() - ms) / 1000 / 60
    }

    enum class Periods(
        val propertyKey: Int,
        private val predicate: (distance: Long) -> Boolean,
    ) {
        NOW(R.string.timeago_now, { it in 0L..(0.99).toLong() }),
        ONE_MINUTE_PAST(R.string.timeago_oneminute_past, { it == 1L }),
        X_MINUTES_PAST(R.string.timeago_xminutes_past, { it in 2..44 }),
        ABOUT_AN_HOUR_PAST(R.string.timeago_aboutanhour_past, { it in 45..89 }),
        X_HOURS_PAST(R.string.timeago_xhours_past, { it in 90..1439 }),
        ONE_DAY_PAST(R.string.timeago_oneday_past, { it in 1440..2519 }),
        X_DAYS_PAST(R.string.timeago_xdays_past, { it in 2520..10079 }),
        ONE_WEEK_PAST(R.string.timeago_oneweek_past, { it in 10080..20159 }),
        X_WEEKS_PAST(R.string.timeago_xweeks_past, { it in 20160..43199 }),
        ABOUT_A_MONTH_PAST(R.string.timeago_aboutamonth_past, { it in 43200..86399 }),
        X_MONTHS_PAST(R.string.timeago_xmonths_past, { it in 86400..525599 }),
        ABOUT_A_YEAR_PAST(R.string.timeago_aboutayear_past, { it in 525600..655199 }),
        OVER_A_YEAR_PAST(R.string.timeago_overayear_past, { it in 655200..914399 }),
        ALMOST_TWO_YEARS_PAST(R.string.timeago_almosttwoyears_past, { it in 914400..1051199 }),
        X_YEARS_PAST(R.string.timeago_xyears_past, { (it / 525600).toFloat().roundToLong() > 1 }),
        ONE_MINUTE_FUTURE(R.string.timeago_oneminute_future, { it.toInt() == -1 }),
        X_MINUTES_FUTURE(R.string.timeago_xminutes_future, { it <= -2 && it >= -44 }),
        ABOUT_AN_HOUR_FUTURE(R.string.timeago_aboutanhour_future, { it <= -45 && it >= -89 }),
        X_HOURS_FUTURE(R.string.timeago_xhours_future, { it <= -90 && it >= -1439 }),
        ONE_DAY_FUTURE(R.string.timeago_oneday_future, { it <= -1440 && it >= -2519 }),
        X_DAYS_FUTURE(R.string.timeago_xdays_future, { it <= -2520 && it >= -10079 }),
        ONE_WEEK_FUTURE(R.string.timeago_oneweek_future, { it <= -10080 && it >= -20159 }),
        X_WEEKS_FUTURE(R.string.timeago_xweeks_future, { it <= -20160 && it >= -43199 }),
        ABOUT_A_MONTH_FUTURE(R.string.timeago_aboutamonth_future, { it <= -43200 && it >= -86399 }),
        X_MONTHS_FUTURE(R.string.timeago_xmonths_future, { it <= -86400 && it >= -525599 }),
        ABOUT_A_YEAR_FUTURE(R.string.timeago_aboutayear_future, { it <= -525600 && it >= -655199 }),
        OVER_A_YEAR_FUTURE(R.string.timeago_overayear_future, { it <= -655200 && it >= -914399 }),
        ALMOST_TWO_YEARS_FUTURE(R.string.timeago_almosttwoyears_future, { it <= -914400 && it >= -1051199 }),
        X_YEARS_FUTURE(R.string.timeago_xyears_future, { (it / 525600).toFloat().roundToLong() < -1 });

        companion object {
            fun findByDistanceMinutes(distanceMinutes: Long): Periods? {
                val values = entries.toTypedArray()
                for (item in values) {
                    val successful = item.predicate(distanceMinutes)
                    if (successful) {
                        return item
                    }
                }
                return null
            }
        }
    }
}