package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DSignatureKeyPair
import com.ismartcoding.plain.data.DUpdateInfo
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.generateChaCha20Key
import com.ismartcoding.plain.platform.generateEd25519KeyPair
import com.ismartcoding.plain.platform.randomPassword
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object SystemPrefs {
    private inline fun <reified T> flow(key: String, default: T): PrefFlow<T> = Prefs.systemFlow(key, default)

    val password = flow("password", "")
    val passwordType = flow("password_type", PasswordType.NONE.value)
    val authTwoFactor = flow("auth_two_factor", true)
    val rotateUrlTokenOnRestart = flow("rotate_url_token_on_restart", false)
    val adbToken = flow("adb_token", "")
    val updateInfo = flow("update_info", "")
    val urlToken = flow("url_token", "")
    val masterSecret = flow("master_secret", "")
    val apiPermissions = flow("api_permissions", setOf<String>())
    val onboarding = flow("onboarding_completed", false)
    val clientId = flow("client_id", "")
    val keyStorePassword = flow("key_store_password", "")
    val audioPlaying = flow("audio_playing", "")
    val audioQueueMigrated = flow("audio_queue_migrated", false)
    val mdnsHostname = flow("mdns_hostname", "plainapp.local")
    val fidUriExtMigrated = flow("fid_uri_ext_migrated", false)
    val appFileRealPathMigrated = flow("app_file_real_path_migrated", false)
    val signatureKey = flow("signature_key_pair", "")

    internal fun initialize() = Unit

    fun passwordTypeValue(): PasswordType = PasswordType.parse(passwordType.value)
    fun setPasswordType(value: PasswordType) { passwordType.value = value.value }

    fun resetAdbToken() {
        TempData.adbToken = randomPassword(32)
        adbToken.value = TempData.adbToken
    }

    fun parseUpdateInfo(value: String): DUpdateInfo = Prefs.decodeOrDefault(value) { DUpdateInfo() }
    fun updateInfoValue(): DUpdateInfo = parseUpdateInfo(updateInfo.value)
    fun setUpdateInfo(value: DUpdateInfo) { updateInfo.value = Prefs.json.encodeToString(value) }
    fun updateInfo(block: (DUpdateInfo) -> DUpdateInfo) = setUpdateInfo(block(updateInfoValue()))

    fun ensureUrlToken() {
        val stored = urlToken.value
        val token = if (rotateUrlTokenOnRestart.value || stored.isEmpty()) generateChaCha20Key() else stored
        TempData.urlToken = Base64Lenient.decode(token)
        if (token != stored) urlToken.value = token
    }

    fun resetUrlToken() {
        val token = generateChaCha20Key()
        TempData.urlToken = Base64Lenient.decode(token)
        urlToken.value = token
    }

    fun ensureMasterSecret(): String {
        if (masterSecret.value.isNotEmpty()) return masterSecret.value
        return generateChaCha20Key().also { masterSecret.value = it }
    }

    fun setApiPermission(permission: Permission, enable: Boolean) {
        val permissions = apiPermissions.value.toMutableSet()
        if (enable) permissions.add(permission.name) else permissions.remove(permission.name)
        apiPermissions.value = permissions
    }

    fun ensureClientId() {
        TempData.clientId = clientId.value.ifEmpty { StringHelper.shortUUID().also { clientId.value = it } }
    }

    fun ensureKeyStorePassword() {
        if (keyStorePassword.value.isEmpty()) keyStorePassword.value = StringHelper.shortUUID()
    }

    fun resetKeyStorePassword() { keyStorePassword.value = StringHelper.shortUUID() }

    fun audioPlayingValue(): String = audioPlaying.value.takeUnless { it.startsWith("{") } ?: ""
    fun setAudioPlaying(value: String) { audioPlaying.value = value }

    fun ensureMdnsHostname() {
        val stored = mdnsHostname.value
        if (stored.isEmpty()) {
            val allowedChars = ('a'..'z').filter { it !in listOf('i', 'l', 'o', 'v') }
            val hostname = (1..2).map { allowedChars.random() }.joinToString("") + ".local"
            TempData.mdnsHostname = hostname
            mdnsHostname.value = hostname
        } else TempData.mdnsHostname = stored
    }
    fun setMdnsHostname(value: String) { mdnsHostname.value = value }

    @OptIn(ExperimentalEncodingApi::class)
    fun ensureSignatureKeyPair() {
        if (signatureKey.value.isNotEmpty()) return
        val (privateKey, publicKey) = generateEd25519KeyPair()
        signatureKey.value = jsonEncode(DSignatureKeyPair(Base64.encode(privateKey), Base64.encode(publicKey)))
    }
    fun signatureKeyPair(): DSignatureKeyPair = jsonDecode(signatureKey.value)

}
