package com.gglee.ziweidoushu

import android.app.Application

class ZiWeiApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
