package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

data class AuthState(
    val isSetupDone: Boolean = false,
    val isUnlocked: Boolean = false,
    val phoneNumber: String = "",
    val maskedPhoneNumber: String = "",
    val recoveryHint: String = ""
)

class AuthManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("vault_auth_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREF_IS_SETUP = "pref_is_setup_v1"
        private const val PREF_PHONE_NUM = "pref_phone_num_v1"
        private const val PREF_PIN_HASH = "pref_pin_hash_v1"
        private const val PREF_SALT = "pref_salt_v1"
        private const val PREF_RECOVERY_HINT = "pref_recovery_hint_v1"
    }

    private var sessionUnlocked: Boolean = false

    fun getAuthState(): AuthState {
        val isSetup = prefs.getBoolean(PREF_IS_SETUP, false)
        val phone = prefs.getString(PREF_PHONE_NUM, "") ?: ""
        val masked = maskPhoneNumber(phone)
        val hint = prefs.getString(PREF_RECOVERY_HINT, "") ?: ""

        return AuthState(
            isSetupDone = isSetup,
            isUnlocked = !isSetup || sessionUnlocked,
            phoneNumber = phone,
            maskedPhoneNumber = masked,
            recoveryHint = hint
        )
    }

    fun completeSetup(phoneNumber: String, pin: String, recoveryHint: String = ""): Boolean {
        if (phoneNumber.isBlank() || pin.length < 4) return false

        val salt = System.currentTimeMillis().toString()
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putBoolean(PREF_IS_SETUP, true)
            .putString(PREF_PHONE_NUM, phoneNumber)
            .putString(PREF_PIN_HASH, hash)
            .putString(PREF_SALT, salt)
            .putString(PREF_RECOVERY_HINT, recoveryHint)
            .apply()

        sessionUnlocked = true
        return true
    }

    fun verifyPin(enteredPin: String): Boolean {
        val savedHash = prefs.getString(PREF_PIN_HASH, null) ?: return false
        val savedSalt = prefs.getString(PREF_SALT, "") ?: ""
        val enteredHash = hashPin(enteredPin, savedSalt)

        val isValid = enteredHash == savedHash
        if (isValid) {
            sessionUnlocked = true
        }
        return isValid
    }

    fun lockVault() {
        sessionUnlocked = false
    }

    fun unlockVaultDirectly() {
        sessionUnlocked = true
    }

    fun resetVaultAuth() {
        prefs.edit().clear().apply()
        sessionUnlocked = false
    }

    private fun hashPin(pin: String, salt: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((pin + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun maskPhoneNumber(phone: String): String {
        if (phone.length <= 4) return phone
        val start = phone.take(3)
        val end = phone.takeLast(3)
        val middleLength = (phone.length - 6).coerceAtLeast(3)
        return "$start ${"•".repeat(middleLength)} $end"
    }
}
