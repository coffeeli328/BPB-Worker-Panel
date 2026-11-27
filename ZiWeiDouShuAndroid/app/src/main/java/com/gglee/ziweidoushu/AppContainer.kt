package com.gglee.ziweidoushu

import android.content.Context
import com.gglee.ziweidoushu.ai.AiClient
import com.gglee.ziweidoushu.ai.AiSettings
import com.gglee.ziweidoushu.data.DatabaseProvider
import com.gglee.ziweidoushu.data.HistoryRepository

class AppContainer(context: Context) {
    val aiSettings = AiSettings(context.applicationContext)
    val aiClient = AiClient()
    private val db = DatabaseProvider.get(context.applicationContext)
    val historyRepository = HistoryRepository(db.historyDao())
}
