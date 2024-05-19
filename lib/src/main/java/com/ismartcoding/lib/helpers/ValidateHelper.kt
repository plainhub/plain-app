package com.ismartcoding.lib.helpers


object ValidateHelper {
    fun isEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isPhone(phone: String): Boolean {
        return android.util.Patterns.PHONE.matcher(phone).matches()
    }
}