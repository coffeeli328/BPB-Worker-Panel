package com.gglee.xhotpost.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.net.URLEncoder

/** Opens X / Twitter compose with pre-filled text — no X API. */
object XPublisher {
    private const val TWITTER_PACKAGE = "com.twitter.android"

    fun openXApp(context: Context): Boolean {
        val appContext = context.applicationContext
        val launch = appContext.packageManager.getLaunchIntentForPackage(TWITTER_PACKAGE)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return startOnMain(appContext, launch)
        }
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/login"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startOnMain(appContext, web)
    }

    fun openCompose(context: Context, text: String): Boolean {
        val appContext = context.applicationContext
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(TWITTER_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (share.resolveActivity(appContext.packageManager) != null) {
            return startOnMain(appContext, share)
        }

        val encoded = URLEncoder.encode(text, Charsets.UTF_8.name())
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://twitter.com/intent/tweet?text=$encoded"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (web.resolveActivity(appContext.packageManager) != null) {
            return startOnMain(appContext, web)
        }
        return false
    }

    private fun startOnMain(context: Context, intent: Intent): Boolean {
        return try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                context.startActivity(intent)
            } else {
                Handler(Looper.getMainLooper()).post {
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
