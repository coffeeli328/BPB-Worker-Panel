package com.gglee.xhotpost.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.ContentLanguage
import com.gglee.xhotpost.domain.NicheId
import com.gglee.xhotpost.domain.WritingStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "hotpost_settings")

class SettingsStore(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        read(prefs)
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(read(prefs))
            prefs[stringKey("displayName")] = next.displayName
            prefs[stringKey("niche")] = next.niche.name
            prefs[stringKey("customNicheLabel")] = next.customNicheLabel
            prefs[stringKey("language")] = next.language.name
            prefs[stringKey("writingStyle")] = next.writingStyle.name
            prefs[stringKey("persona")] = next.persona
            prefs[stringKey("affiliateUrl")] = next.affiliateUrl
            prefs[stringKey("affiliateLabel")] = next.affiliateLabel
            prefs[stringKey("ctaTemplate")] = next.ctaTemplate
            prefs[booleanKey("autoDraft")] = next.autoDraft
            prefs[booleanKey("autoPublishApproved")] = next.autoPublishApproved
            prefs[intKey("pollIntervalMinutes")] = next.pollIntervalMinutes
            prefs[intKey("maxDraftsPerTick")] = next.maxDraftsPerTick
            prefs[booleanKey("demoMode")] = next.demoMode
            prefs[booleanKey("xLoggedIn")] = next.xLoggedIn
            prefs[stringKey("xUsername")] = next.xUsername
            prefs[booleanKey("aiEnabled")] = next.aiEnabled
            prefs[stringKey("aiBaseUrl")] = next.aiBaseUrl
            prefs[stringKey("aiApiKey")] = next.aiApiKey
            prefs[stringKey("aiModel")] = next.aiModel
        }
    }

    private fun read(
        prefs: androidx.datastore.preferences.core.Preferences,
    ): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            displayName = prefs[stringKey("displayName")] ?: "热帖",
            niche = runCatching {
                NicheId.valueOf(prefs[stringKey("niche")] ?: NicheId.TECH.name)
            }.getOrDefault(NicheId.TECH),
            customNicheLabel = prefs[stringKey("customNicheLabel")] ?: "",
            language = runCatching {
                ContentLanguage.valueOf(
                    prefs[stringKey("language")] ?: ContentLanguage.ZH.name,
                )
            }.getOrDefault(ContentLanguage.ZH),
            writingStyle = runCatching {
                WritingStyle.valueOf(
                    prefs[stringKey("writingStyle")] ?: WritingStyle.OPINION.name,
                )
            }.getOrDefault(WritingStyle.OPINION),
            persona = prefs[stringKey("persona")] ?: defaults.persona,
            affiliateUrl = prefs[stringKey("affiliateUrl")] ?: "",
            affiliateLabel = prefs[stringKey("affiliateLabel")] ?: "了解更多",
            ctaTemplate = prefs[stringKey("ctaTemplate")] ?: defaults.ctaTemplate,
            autoDraft = prefs[booleanKey("autoDraft")] ?: true,
            autoPublishApproved = prefs[booleanKey("autoPublishApproved")] ?: true,
            pollIntervalMinutes = prefs[intKey("pollIntervalMinutes")] ?: 30,
            maxDraftsPerTick = prefs[intKey("maxDraftsPerTick")] ?: 3,
            demoMode = prefs[booleanKey("demoMode")] ?: true,
            xLoggedIn = prefs[booleanKey("xLoggedIn")] ?: false,
            xUsername = prefs[stringKey("xUsername")] ?: "",
            aiEnabled = prefs[booleanKey("aiEnabled")] ?: false,
            aiBaseUrl = prefs[stringKey("aiBaseUrl")] ?: defaults.aiBaseUrl,
            aiApiKey = prefs[stringKey("aiApiKey")] ?: "",
            aiModel = prefs[stringKey("aiModel")] ?: defaults.aiModel,
        )
    }

    private fun stringKey(name: String) = stringPreferencesKey(name)
    private fun booleanKey(name: String) = booleanPreferencesKey(name)
    private fun intKey(name: String) = intPreferencesKey(name)
}
