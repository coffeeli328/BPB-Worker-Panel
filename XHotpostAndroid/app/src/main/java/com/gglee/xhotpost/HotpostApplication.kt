package com.gglee.xhotpost

import android.app.Application
import com.gglee.xhotpost.work.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HotpostApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            val settings = container.repository.settings.first()
            SyncWorker.schedule(this@HotpostApplication, settings.pollIntervalMinutes.toLong())
            container.repository.runTick()
        }
    }
}
