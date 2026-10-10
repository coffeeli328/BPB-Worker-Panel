package com.gglee.xhotpost

import android.app.Application
import android.util.Log
import com.gglee.xhotpost.work.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HotpostApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        try {
            container = AppContainer(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to init AppContainer", t)
            throw t
        }
        appScope.launch {
            try {
                val settings = container.repository.settings.first()
                SyncWorker.schedule(this@HotpostApplication, settings.pollIntervalMinutes.toLong())
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to schedule sync", t)
            }
            // 启动时抓热点失败不应杀死 App
            try {
                container.repository.runTick()
            } catch (t: Throwable) {
                Log.e(TAG, "Boot tick failed", t)
            }
        }
    }

    companion object {
        private const val TAG = "HotpostApp"
    }
}
