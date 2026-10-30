package com.ismartcoding.plain.tests

import android.content.res.Configuration
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.ui.platform.system.applySystemBarAppearanceForDarkTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSystemBarAppearanceTest {
    @Test
    fun systemBarAppearanceSurvivesConfigurationDispatch() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                for (useDarkTheme in listOf(false, true)) {
                    applySystemBarAppearanceForDarkTheme(useDarkTheme)
                    val configuration = Configuration(activity.resources.configuration).apply {
                        fontScale += 0.1f
                    }
                    window.decorView.dispatchConfigurationChanged(configuration)
                    assertEquals(!activity.isGestureInteractionMode(), window.isNavigationBarContrastEnforced)
                    assertEquals(!useDarkTheme, controller.isAppearanceLightNavigationBars)
                    assertEquals(!useDarkTheme, controller.isAppearanceLightStatusBars)
                }
            }
        }
    }
}
