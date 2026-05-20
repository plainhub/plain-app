package com.ismartcoding.plain.ui.page.pomodoro

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.stringResource

enum class PomodoroState {
    WORK, SHORT_BREAK, LONG_BREAK;

    @Composable
    fun getText(): String {
        return when (this) {
            WORK -> stringResource(Res.string.work_time)
            SHORT_BREAK -> stringResource(Res.string.short_break)
            LONG_BREAK -> stringResource(Res.string.long_break)
        }
    }
}
