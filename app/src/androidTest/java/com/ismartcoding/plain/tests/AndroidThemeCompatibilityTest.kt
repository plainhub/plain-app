package com.ismartcoding.plain.tests

import android.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidThemeCompatibilityTest {

    @Test
    fun plainActivityThemeUsesSupportedCutoutMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val themedContext = ContextThemeWrapper(context, R.style.Theme_PlainActivity)
        val attributes = themedContext.obtainStyledAttributes(
            intArrayOf(android.R.attr.windowLayoutInDisplayCutoutMode)
        )
        val actual = try {
            attributes.getInt(0, -1)
        } finally {
            attributes.recycle()
        }
        // ALWAYS crashes on Samsung One UI builds whose PhoneWindow rejects the mode even
        // when the framework API level supports it; SHORT_EDGES is safe everywhere.
        assertEquals(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES, actual)
    }
}
