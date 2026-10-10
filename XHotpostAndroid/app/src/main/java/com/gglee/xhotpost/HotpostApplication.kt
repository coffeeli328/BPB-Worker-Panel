package com.gglee.xhotpost

import android.app.Application
import android.util.Log

class HotpostApplication : Application() {
    @Volatile
    private var containerOrNull: AppContainer? = null

    val container: AppContainer
        get() = containerOrNull ?: synchronized(this) {
            containerOrNull ?: AppContainer(this).also { containerOrNull = it }
        }

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        try {
            containerOrNull = AppContainer(this)
        } catch (t: Throwable) {
            Log.e(TAG, "AppContainer init failed", t)
            CrashLogger.save(this, t)
        }
        // 不在 Application 里跑网络 / WorkManager，避免启动期崩溃
    }

    companion object {
        private const val TAG = "HotpostApp"
    }
}
