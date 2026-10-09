package com.gglee.qimendunjia

import android.content.Context
import com.gglee.qimendunjia.ai.AiClient
import com.gglee.qimendunjia.ai.AiSettings
import com.gglee.qimendunjia.data.DatabaseProvider
import com.gglee.qimendunjia.data.HistoryRepository

class AppContainer(context: Context) {
    val aiSettings = AiSettings(context.applicationContext)
    val aiClient = AiClient()
    private val db = DatabaseProvider.get(context.applicationContext)
    val historyRepository = HistoryRepository(db.historyDao())
}
