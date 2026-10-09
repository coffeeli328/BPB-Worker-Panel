package com.gglee.qimendunjia.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AiSettings(context: Context) {
    companion object {
        const val PLACEHOLDER_BASE_URL_DEEPSEEK = "https://api.deepseek.com/v1"
        const val PLACEHOLDER_BASE_URL_OPENAI = "https://api.openai.com/v1"
        const val PLACEHOLDER_MODEL_DEEPSEEK = "deepseek-chat"
        const val PLACEHOLDER_MODEL_OPENAI = "gpt-4o-mini"

        private const val PREFS = "ai_settings"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
        private const val ENCRYPTED_PREFS = "ai_secure"
        private const val KEY_API_KEY = "api_key"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            ENCRYPTED_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value).apply()
        }

    var model: String
        get() = prefs.getString(KEY_MODEL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_MODEL, value).apply()
        }

    var apiKey: String
        get() = securePrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) {
            securePrefs.edit().putString(KEY_API_KEY, value).apply()
        }

    val isConfigured: Boolean
        get() = apiKey.trim().isNotEmpty()

    val resolvedBaseUrl: String
        get() {
            val t = baseUrl.trim()
            return if (t.isNotEmpty()) t else PLACEHOLDER_BASE_URL_DEEPSEEK
        }

    val resolvedModel: String
        get() {
            val t = model.trim()
            return if (t.isNotEmpty()) t else PLACEHOLDER_MODEL_DEEPSEEK
        }

    fun applyDeepSeekPreset() {
        baseUrl = PLACEHOLDER_BASE_URL_DEEPSEEK
        model = PLACEHOLDER_MODEL_DEEPSEEK
    }

    fun applyOpenAiPreset() {
        baseUrl = PLACEHOLDER_BASE_URL_OPENAI
        model = PLACEHOLDER_MODEL_OPENAI
    }

    fun clearKey() {
        apiKey = ""
    }
}
