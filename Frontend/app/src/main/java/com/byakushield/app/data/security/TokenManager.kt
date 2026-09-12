package com.byakushield.app.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val preferences =
        EncryptedSharedPreferences.create(
            context,
            "byakushield_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun saveToken(token: String) {
        preferences.edit()
            .putString("jwt_token", token)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString(
            "jwt_token",
            null
        )
    }

    fun clearToken() {
        preferences.edit()
            .remove("jwt_token")
            .apply()
    }
}