package com.ismartcoding.plain.ui.web

import com.ismartcoding.plain.features.settings.PasswordType
import com.ismartcoding.plain.models.PasswordSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PasswordSettingsStateTest {
    @Test
    fun initial_status_comes_from_settings_not_empty_draft() = runTest {
        for (configured in listOf(false, true)) {
            val fixture = Fixture(configured)
            assertNull(fixture.state.settings)
            fixture.state.refresh()
            assertEquals(configured, fixture.state.settings?.passwordConfigured)
            assertFalse(fixture.state.hasDraft)
            assertFalse(fixture.state.busy)
        }
    }

    @Test
    fun read_failure_remains_unknown_and_cannot_open_editor_or_toggle() = runTest {
        val fixture = Fixture()
        fixture.read = { error("unavailable") }
        fixture.state.refresh()
        fixture.state.edit()
        fixture.state.setEnabled(true)
        assertNull(fixture.state.settings)
        assertEquals(PasswordSettingsError.LOAD, fixture.state.error)
        assertFalse(fixture.state.editing)
        assertTrue(fixture.toggles.isEmpty())
        assertFalse(fixture.state.busy)
    }

    @Test
    fun repeated_entry_preserves_configured_status_and_clears_cancelled_draft() = runTest {
        val fixture = Fixture(configured = true)
        fixture.state.refresh()
        repeat(2) {
            fixture.state.edit()
            assertTrue(fixture.state.settings!!.passwordConfigured)
            assertFalse(fixture.state.hasDraft)
            fixture.state.updatePassword("cancelled-secret")
            fixture.state.updateConfirmation("cancelled-secret")
            fixture.state.discard()
            fixture.state.save()
            assertFalse(fixture.state.editing)
            assertFalse(fixture.state.hasDraft)
        }
        assertTrue(fixture.passwords.isEmpty())
        assertTrue(fixture.toggles.isEmpty())
    }

    @Test
    fun mismatched_manual_password_cannot_write() = runTest {
        val fixture = Fixture().editing()
        fixture.state.updatePassword("first-secret")
        fixture.state.updateConfirmation("different-secret")
        fixture.state.save()
        assertEquals(PasswordSettingsError.MISMATCH, fixture.state.error)
        assertTrue(fixture.state.editing)
        assertTrue(fixture.passwords.isEmpty())
        assertTrue(fixture.toggles.isEmpty())
    }

    @Test
    fun blank_password_or_missing_confirmation_cannot_write() = runTest {
        val fixture = Fixture().editing()
        for ((password, confirmation) in listOf("" to "", "   " to "   ", "secret" to "")) {
            fixture.state.updatePassword(password)
            fixture.state.updateConfirmation(confirmation)
            assertFalse(fixture.state.canSave)
            fixture.state.save()
        }
        assertTrue(fixture.passwords.isEmpty())
    }

    @Test
    fun generation_only_changes_draft_and_automatically_confirms_it() = runTest {
        val fixture = Fixture().editing()
        fixture.state.generate()
        assertEquals("generated-secret", fixture.state.password)
        assertEquals(fixture.state.password, fixture.state.confirmation)
        assertTrue(fixture.state.generated)
        assertTrue(fixture.state.canSave)
        assertFalse(fixture.state.saved)
        assertFalse(fixture.state.settings!!.passwordConfigured)
        assertTrue(fixture.passwords.isEmpty())
        assertTrue(fixture.toggles.isEmpty())
        fixture.state.discard()
        fixture.state.save()
        assertFalse(fixture.state.hasDraft)
        assertFalse(fixture.state.generated)
        assertTrue(fixture.passwords.isEmpty())
    }

    @Test
    fun failed_or_blank_generation_preserves_existing_draft() = runTest {
        val fixture = Fixture().editing()
        fixture.state.updatePassword("old-secret")
        fixture.state.updateConfirmation("old-confirmation")
        for (generate in listOf<suspend () -> String>({ error("unavailable") }, { " " })) {
            fixture.generate = generate
            fixture.state.generate()
            assertEquals("old-secret", fixture.state.password)
            assertEquals("old-confirmation", fixture.state.confirmation)
            assertEquals(PasswordSettingsError.GENERATE, fixture.state.error)
            assertFalse(fixture.state.busy)
        }
        assertTrue(fixture.passwords.isEmpty())
    }

    @Test
    fun manual_change_after_generation_requires_matching_confirmation() = runTest {
        val fixture = Fixture().editing()
        fixture.state.generate()
        fixture.state.updatePassword("manual-secret")
        assertFalse(fixture.state.generated)
        fixture.state.save()
        assertEquals(PasswordSettingsError.MISMATCH, fixture.state.error)
        assertTrue(fixture.passwords.isEmpty())
    }

    @Test
    fun save_success_clears_secrets_exits_editor_and_refreshes_settings() = runTest {
        val fixture = Fixture(configured = true, type = PasswordType.FIXED).editing()
        fixture.state.generate()
        fixture.state.save()
        assertEquals(listOf("generated-secret"), fixture.passwords)
        assertTrue(fixture.state.saved)
        assertFalse(fixture.state.editing)
        assertFalse(fixture.state.hasDraft)
        assertFalse(fixture.state.generated)
        assertFalse(fixture.state.busy)
        assertNull(fixture.state.error)
        assertEquals(fixture.source, fixture.state.settings)
        assertTrue(fixture.toggles.isEmpty())
    }

    @Test
    fun failed_save_preserves_editable_draft_for_retry() = runTest {
        val fixture = Fixture().editing()
        fixture.state.generate()
        fixture.write = { error("write failed") }
        fixture.state.save()
        assertEquals(PasswordSettingsError.SAVE, fixture.state.error)
        assertEquals("generated-secret", fixture.state.password)
        assertEquals("generated-secret", fixture.state.confirmation)
        assertTrue(fixture.state.editing)
        assertTrue(fixture.state.canSave)
        assertFalse(fixture.state.saved)
        assertTrue(fixture.toggles.isEmpty())
        fixture.write = { fixture.source = fixture.source.copy(passwordConfigured = true) }
        fixture.state.save()
        assertTrue(fixture.state.saved)
        assertFalse(fixture.state.hasDraft)
    }

    @Test
    fun suspended_save_blocks_duplicate_save_and_draft_changes() = runTest {
        val fixture = Fixture(configured = true).editing()
        fixture.state.generate()
        val release = CompletableDeferred<Unit>()
        fixture.write = { release.await() }
        val save = launch(start = CoroutineStart.UNDISPATCHED) { fixture.state.save() }
        assertEquals(PasswordSettingsOperation.SAVE, fixture.state.operation)
        assertFalse(fixture.state.canSave)
        fixture.state.save()
        fixture.state.updatePassword("replacement")
        fixture.state.updateConfirmation("replacement")
        fixture.state.discard()
        fixture.state.generate()
        assertEquals(listOf("generated-secret"), fixture.passwords)
        assertEquals("generated-secret", fixture.state.password)
        assertTrue(fixture.state.editing)
        release.complete(Unit)
        save.join()
        assertTrue(fixture.state.saved)
        assertFalse(fixture.state.busy)
    }

    @Test
    fun enabling_without_password_opens_editor_without_enabling_then_saves_before_enable() = runTest {
        val fixture = Fixture()
        fixture.state.refresh()
        fixture.state.setEnabled(true)
        assertTrue(fixture.state.editing)
        assertEquals(PasswordType.NONE, fixture.state.settings?.passwordType)
        assertTrue(fixture.toggles.isEmpty())
        fixture.state.updatePassword("first-secret")
        fixture.state.updateConfirmation("first-secret")
        fixture.state.save()
        assertEquals(listOf("password", "enable:true"), fixture.effects)
        assertTrue(fixture.state.settings!!.passwordConfigured)
        assertEquals(PasswordType.FIXED, fixture.state.settings?.passwordType)
    }

    @Test
    fun saving_first_password_directly_also_enables_protection() = runTest {
        val fixture = Fixture().editing()
        fixture.state.generate()
        fixture.state.save()
        assertEquals(listOf("password", "enable:true"), fixture.effects)
        assertEquals(PasswordType.FIXED, fixture.state.settings?.passwordType)
    }

    @Test
    fun editing_existing_disabled_password_preserves_disabled_mode() = runTest {
        val fixture = Fixture(configured = true).editing()
        fixture.state.generate()
        fixture.state.save()
        assertTrue(fixture.state.saved)
        assertEquals(PasswordType.NONE, fixture.state.settings?.passwordType)
        assertTrue(fixture.toggles.isEmpty())
    }

    @Test
    fun configured_password_can_be_enabled_and_disabled_without_editing() = runTest {
        val fixture = Fixture(configured = true)
        fixture.state.refresh()
        fixture.state.setEnabled(true)
        assertEquals(PasswordType.FIXED, fixture.state.settings?.passwordType)
        fixture.state.setEnabled(false)
        assertEquals(PasswordType.NONE, fixture.state.settings?.passwordType)
        assertEquals(listOf(true, false), fixture.toggles)
        assertFalse(fixture.state.editing)
        assertTrue(fixture.passwords.isEmpty())
    }

    @Test
    fun enable_failure_after_save_retains_saved_status_without_secrets() = runTest {
        val fixture = Fixture().editing()
        fixture.toggle = { error("enable failed") }
        fixture.state.generate()
        fixture.state.save()
        assertTrue(fixture.state.saved)
        assertFalse(fixture.state.editing)
        assertFalse(fixture.state.hasDraft)
        assertEquals(PasswordSettingsError.ENABLE, fixture.state.error)
        assertTrue(fixture.state.settings!!.passwordConfigured)
        assertEquals(PasswordType.NONE, fixture.state.settings?.passwordType)
    }

    @Test
    fun refresh_failure_after_save_retains_saved_status_without_secrets() = runTest {
        val fixture = Fixture(configured = true).editing()
        fixture.state.generate()
        fixture.read = { error("read failed") }
        fixture.state.save()
        assertTrue(fixture.state.saved)
        assertFalse(fixture.state.editing)
        assertFalse(fixture.state.hasDraft)
        assertEquals(PasswordSettingsError.LOAD, fixture.state.error)
        assertNull(fixture.state.settings)
        assertFalse(fixture.state.busy)
    }

    @Test
    fun utf8_password_limit_accepts_1024_bytes_and_rejects_1025() = runTest {
        val accepted = "\u00e9".repeat(512)
        val rejected = accepted + "x"
        val fixture = Fixture(configured = true).editing()
        fixture.state.updatePassword(rejected)
        fixture.state.updateConfirmation(rejected)
        fixture.state.save()
        assertEquals(PasswordSettingsError.TOO_LONG, fixture.state.error)
        assertTrue(fixture.passwords.isEmpty())
        assertTrue(fixture.state.editing)
        fixture.state.updatePassword(accepted)
        fixture.state.updateConfirmation(accepted)
        fixture.state.save()
        assertEquals(listOf(accepted), fixture.passwords)
        assertTrue(fixture.state.saved)
    }

    @Test
    fun cancellation_propagates_from_each_operation_and_clears_busy() = runTest {
        for (operation in PasswordSettingsOperation.entries) {
            val fixture = Fixture(configured = true).editing()
            fixture.state.generate()
            val cancelled = CancellationException("cancelled")
            val action: suspend () -> Unit = when (operation) {
                PasswordSettingsOperation.READ -> {
                    fixture.read = { throw cancelled }
                    suspend { fixture.state.refresh() }
                }
                PasswordSettingsOperation.SAVE -> {
                    fixture.write = { throw cancelled }
                    suspend { fixture.state.save() }
                }
                PasswordSettingsOperation.GENERATE -> {
                    fixture.generate = { throw cancelled }
                    suspend { fixture.state.generate() }
                }
                PasswordSettingsOperation.TOGGLE -> {
                    fixture.toggle = { throw cancelled }
                    suspend { fixture.state.setEnabled(true) }
                }
            }
            assertSame(cancelled, assertFailsWith<CancellationException> { action() })
            assertFalse(fixture.state.busy)
            assertNull(fixture.state.error)
            assertFalse(fixture.state.saved)
            assertEquals("generated-secret", fixture.state.password)
        }
    }

    @Test
    fun cancelling_suspended_save_preserves_unsaved_draft_and_releases_busy() = runTest {
        val fixture = Fixture().editing()
        fixture.state.generate()
        val release = CompletableDeferred<Unit>()
        fixture.write = { release.await() }
        val save = launch(start = CoroutineStart.UNDISPATCHED) { fixture.state.save() }
        assertTrue(fixture.state.busy)
        save.cancelAndJoin()
        assertTrue(save.isCancelled)
        assertFalse(fixture.state.busy)
        assertFalse(fixture.state.saved)
        assertTrue(fixture.state.editing)
        assertTrue(fixture.state.canSave)
        assertEquals("generated-secret", fixture.state.password)
        assertEquals("generated-secret", fixture.state.confirmation)
        assertNull(fixture.state.error)
        assertTrue(fixture.toggles.isEmpty())
    }

    @Test
    fun cancellation_after_password_write_preserves_saved_fact() = runTest {
        for (cancelEnable in listOf(false, true)) {
            val fixture = Fixture().editing()
            fixture.state.generate()
            val cancelled = CancellationException("cancelled after write")
            if (cancelEnable) fixture.toggle = { throw cancelled }
            else fixture.read = { throw cancelled }
            assertSame(cancelled, assertFailsWith<CancellationException> { fixture.state.save() })
            assertTrue(fixture.state.saved)
            assertFalse(fixture.state.hasDraft)
            assertFalse(fixture.state.editing)
            assertFalse(fixture.state.busy)
            assertNull(fixture.state.error)
        }
    }

    private class Fixture(configured: Boolean = false, type: PasswordType = PasswordType.NONE) {
        var source = PasswordSettings(configured, type)
        val passwords = mutableListOf<String>()
        val toggles = mutableListOf<Boolean>()
        val effects = mutableListOf<String>()
        var read: suspend () -> PasswordSettings = { source }
        var write: suspend (String) -> Unit = { source = source.copy(passwordConfigured = true) }
        var toggle: suspend (Boolean) -> Unit = { enabled ->
            source = source.copy(passwordType = if (enabled) PasswordType.FIXED else PasswordType.NONE)
        }
        var generate: suspend () -> String = { "generated-secret" }
        val state = PasswordSettingsState(
            readSettings = { read() },
            writePassword = {
                passwords += it
                effects += "password"
                write(it)
            },
            writeEnabled = {
                toggles += it
                effects += "enable:$it"
                toggle(it)
            },
            createPassword = { generate() },
        )

        suspend fun editing(): Fixture {
            state.refresh()
            state.edit()
            return this
        }
    }
}
