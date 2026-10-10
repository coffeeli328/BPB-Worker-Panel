package com.gglee.xhotpost

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object CrashLogger {
    private const val PREFS = "hotpost_crash"
    private const val KEY = "last_crash"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                save(app, throwable)
            } catch (_: Exception) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun save(context: Context, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val text = sw.toString().take(4000)
        Log.e("HotpostCrash", text)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, text)
            .commit()
    }

    fun consume(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY, null)
        if (value != null) {
            prefs.edit().remove(KEY).apply()
        }
        return value
    }
}
