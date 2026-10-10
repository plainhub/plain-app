package com.ismartcoding.plain.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
sealed class SystemCommand {
    @Serializable @SerialName("SET_PASSWORD") data class SetPassword(val password: String) : SystemCommand()
    @Serializable @SerialName("SET_PASSWORD_TYPE") data class SetPasswordType(val value: Int) : SystemCommand()
    @Serializable @SerialName("SET_TWO_FACTOR") data class SetTwoFactor(val enabled: Boolean) : SystemCommand()
    @Serializable @SerialName("SET_ROTATE_URL_TOKEN") data class SetRotateUrlToken(val enabled: Boolean) : SystemCommand()
    @Serializable @SerialName("RESET_PASSWORD") data object ResetPassword : SystemCommand()
    @Serializable @SerialName("RESET_URL_TOKEN") data object ResetUrlToken : SystemCommand()
    @Serializable @SerialName("RESET_ADB_TOKEN") data object ResetAdbToken : SystemCommand()
    @Serializable @SerialName("SET_API_PERMISSION") data class SetApiPermission(val permission: String, val enabled: Boolean) : SystemCommand()
    @Serializable @SerialName("SET_API_PERMISSIONS") data class SetApiPermissions(val permissions: Set<String>) : SystemCommand()
    @Serializable @SerialName("COMPLETE_ONBOARDING") data object CompleteOnboarding : SystemCommand()
    @Serializable @SerialName("SET_MDNS_HOSTNAME") data class SetMdnsHostname(val hostname: String) : SystemCommand()
    @Serializable @SerialName("PATCH_UPDATE") data class PatchUpdate(val patch: UpdateInfoPatch) : SystemCommand()
}
