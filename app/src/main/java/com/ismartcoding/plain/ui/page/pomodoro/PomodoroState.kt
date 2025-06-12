package com.ismartcoding.plain.ui.page.pomodoro

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper

enum class PomodoroState {
    WORK, SHORT_BREAK, LONG_BREAK;

    fun getText(): String {
        return when (this) {
            WORK ->  LocaleHelper.getString(R.string.work_time)
            SHORT_BREAK -> LocaleHelper.getString(R.string.short_break)
            LONG_BREAK -> LocaleHelper.getString(R.string.long_break)
        }
    }
}