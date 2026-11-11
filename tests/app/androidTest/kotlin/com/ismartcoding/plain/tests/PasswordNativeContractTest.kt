package com.ismartcoding.plain.tests

import android.content.ClipDescription
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.features.settings.PasswordType
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.RustRuntime
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.nativeValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PasswordNativeContractTest {
    @Test
    fun native_password_generation_save_and_modes_preserve_opaque_credentials() = runBlocking {
        requireDisposableEmulator()
        assertTrue(RustRuntime.isCoreInitialized)
        val initial = settings()
        assertFalse(initial.getValue("passwordConfigured").jsonPrimitive.boolean)
        assertEquals("NONE", initial.getValue("password_type").jsonPrimitive.content)
        val initialCredentials = storedCredentials()
        assertTrue("Fresh fixture must not contain a password credential", initialCredentials.isEmpty())

        val generated = Prefs.generatePassword()
        assertEquals(6, generated.length)
        assertTrue(generated.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' })
        assertFalse(settings().getValue("passwordConfigured").jsonPrimitive.boolean)
        assertEquals("NONE", settings().getValue("password_type").jsonPrimitive.content)
        assertTrue("Generation must not persist credentials", storedCredentials().isEmpty())

        assertTrue(Prefs.setPassword(generated))
        val saved = storedCredentials()
        assertEquals(setOf("opaque_server_setup", "opaque_password_record"), saved.keys)
        assertTrue(saved.values.all { it.jsonPrimitive.content.isNotEmpty() })
        assertTrue("Plaintext password must not be persisted", storedPreferences()["password"]?.jsonPrimitive?.content.orEmpty().isEmpty())
        assertStatus(PasswordType.NONE)

        Prefs.generatePassword()
        assertTrue("Generating a replacement must preserve the saved credential", saved == storedCredentials())
        assertStatus(PasswordType.NONE)

        for (type in listOf(PasswordType.FIXED, PasswordType.NONE, PasswordType.FIXED, PasswordType.NONE)) {
            assertTrue(Prefs.setPasswordType(type))
            assertStatus(type)
            assertTrue("Changing protection mode must preserve both credential fields", saved == storedCredentials())
        }

        assertTrue(Prefs.setPassword("synthetic-replacement-password"))
        val replaced = storedCredentials()
        assertEquals(saved.keys, replaced.keys)
        assertTrue("Saving a replacement must replace the password record", saved["opaque_password_record"] != replaced["opaque_password_record"])
        assertStatus(PasswordType.NONE)
    }

    @Test
    fun copied_password_is_marked_sensitive_on_android() = runBlocking<Unit> {
        requireDisposableEmulator()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            withTimeout(5_000) {
                var focused = false
                while (!focused) {
                    scenario.onActivity { focused = it.hasWindowFocus() }
                    if (!focused) delay(50)
                }
            }
            scenario.onActivity {
                setClipboardText("password-test", "synthetic-clipboard-password", sensitive = true)
                assertEquals(
                    true,
                    clipboardManager.primaryClipDescription?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false),
                )
            }
        }
    }

    private fun requireDisposableEmulator() {
        assumeTrue(
            "Requires an explicitly selected disposable emulator with fresh application data",
            InstrumentationRegistry.getArguments().getString("passwordFixture") == "disposable-emulator-5580" &&
                Build.HARDWARE in setOf("ranchu", "goldfish"),
        )
    }

    private suspend fun assertStatus(type: PasswordType) {
        val snapshot = settings()
        assertTrue(snapshot.getValue("passwordConfigured").jsonPrimitive.boolean)
        assertEquals(type.name, snapshot.getValue("password_type").jsonPrimitive.content)
        assertEquals("", snapshot.getValue("password").jsonPrimitive.content)
        assertFalse(snapshot.containsKey("opaque_server_setup"))
        assertFalse(snapshot.containsKey("opaque_password_record"))
        val typed = RustNativeApi.passwordSettings()
        assertTrue(typed.passwordConfigured)
        assertEquals(type, typed.passwordType)
        Prefs.load()
        assertEquals(true, Prefs.passwordConfigured.value)
        assertEquals(type, Prefs.passwordType.value)
    }

    private fun settings(): JsonObject = nativeValue(RustNativeBindings.loadPreferencesResponse())

    private fun storedPreferences(): JsonObject =
        JsonHelper.jsonDecode(File(RustRuntime.dataDir, "prefs.json").readText())

    private fun storedCredentials(): Map<String, kotlinx.serialization.json.JsonElement> =
        storedPreferences().filterKeys { it in setOf("opaque_server_setup", "opaque_password_record") }
}
