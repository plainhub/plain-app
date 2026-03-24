package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroPage

@Composable
fun PageContentPomodoro(
    navController: NavHostController,
    pomodoroVM: PomodoroViewModel,
    onOpenDrawer: () -> Unit,
    paddingValues: PaddingValues,
) {
    PomodoroPage(
        navController = navController,
        pomodoroVM = pomodoroVM,
        onOpenDrawer = onOpenDrawer,
    )
}
