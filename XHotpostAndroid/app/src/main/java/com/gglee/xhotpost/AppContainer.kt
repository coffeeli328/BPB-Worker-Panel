package com.gglee.xhotpost

import android.content.Context
import com.gglee.xhotpost.data.HotpostRepository
import com.gglee.xhotpost.data.SettingsStore
import com.gglee.xhotpost.data.local.HotpostDatabase

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val db = HotpostDatabase.build(appContext)
    private val settingsStore = SettingsStore(appContext)
    val repository = HotpostRepository(
        context = appContext,
        dao = db.hotpostDao(),
        settingsStore = settingsStore,
    )
}
