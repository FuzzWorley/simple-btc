package com.bitcoinportfolio.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SecureStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SecureStorage {

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Spoof amount is intentionally stored in plain (unencrypted) SharedPreferences.
    // An attacker who bypasses auth should be able to read a plausible-looking BTC value
    // — that is the entire point of the deception model.
    private val plainPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("plain_prefs", Context.MODE_PRIVATE)
    }

    override fun isSetupComplete(): Boolean =
        encryptedPrefs.getBoolean(KEY_SETUP_COMPLETE, false)

    override fun markSetupComplete() {
        encryptedPrefs.edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
    }

    override fun savePinHash(hash: String, salt: String) {
        encryptedPrefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
    }

    override fun getPinHash(): String? = encryptedPrefs.getString(KEY_PIN_HASH, null)
    override fun getPinSalt(): String? = encryptedPrefs.getString(KEY_PIN_SALT, null)

    override fun saveBtcAmount(amount: Double) {
        encryptedPrefs.edit().putString(KEY_BTC_AMOUNT, amount.toString()).apply()
    }

    override fun getBtcAmount(): Double? =
        encryptedPrefs.getString(KEY_BTC_AMOUNT, null)?.toDoubleOrNull()

    override fun saveSpoofAmount(amount: Double) {
        plainPrefs.edit().putFloat(KEY_SPOOF_AMOUNT, amount.toFloat()).apply()
    }

    override fun getSpoofAmount(): Double? =
        if (plainPrefs.contains(KEY_SPOOF_AMOUNT))
            plainPrefs.getFloat(KEY_SPOOF_AMOUNT, 0f).toDouble()
        else null

    override fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        plainPrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SETUP_COMPLETE = "is_setup_complete"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_BTC_AMOUNT = "btc_amount"
        private const val KEY_SPOOF_AMOUNT = "spoof_amount"
    }
}
