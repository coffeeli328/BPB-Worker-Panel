package com.gglee.meihuayishu

import android.content.Context
import com.gglee.meihuayishu.ai.AiClient
import com.gglee.meihuayishu.ai.AiSettings
import com.gglee.meihuayishu.data.DatabaseProvider
import com.gglee.meihuayishu.data.HistoryRepository

class AppContainer(context: Context) {
    val aiSettings = AiSettings(context.applicationContext)
    val aiClient = AiClient()
    private val db = DatabaseProvider.get(context.applicationContext)
    val historyRepository = HistoryRepository(db.historyDao())
}
