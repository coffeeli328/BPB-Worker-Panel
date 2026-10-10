package com.gglee.xhotpost.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.browser.customtabs.CustomTabsIntent
import java.net.URLEncoder

/** Opens X / Twitter without X API. */
object XPublisher {
    private val X_PACKAGES = listOf(
        "com.twitter.android",
        "com.twitter.android.lite",
    )

    fun isXAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return X_PACKAGES.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun openLoginInBrowser(context: Context): Boolean {
        val urls = listOf(
            "https://x.com/i/flow/login",
            "https://twitter.com/i/flow/login",
            "https://x.com/login",
        )
        for (url in urls) {
            if (openCustomTabOrBrowser(context, url)) return true
        }
        return false
    }

    fun openXApp(context: Context): Boolean {
        val appContext = context.applicationContext
        val pm = appContext.packageManager

        for (pkg in X_PACKAGES) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (startOnMain(appContext, launch)) return true
            }
        }

        // Deep links used by X/Twitter
        val deepLinks = listOf(
            "twitter://timeline",
            "twitter://login",
            "https://x.com/home",
            "https://twitter.com/home",
        )
        for (link in deepLinks) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                X_PACKAGES.firstOrNull()?.let { setPackage(it) }
            }
            // try with package, then without
            if (startOnMain(appContext, intent)) return true
            val any = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startOnMain(appContext, any)) return true
        }

        return openLoginInBrowser(context)
    }

    fun openCompose(context: Context, text: String): Boolean {
        val appContext = context.applicationContext
        for (pkg in X_PACKAGES) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (share.resolveActivity(appContext.packageManager) != null) {
                if (startOnMain(appContext, share)) return true
            }
        }

        val encoded = URLEncoder.encode(text, Charsets.UTF_8.name())
        val urls = listOf(
            "https://x.com/intent/post?text=$encoded",
            "https://twitter.com/intent/tweet?text=$encoded",
        )
        for (url in urls) {
            if (openCustomTabOrBrowser(context, url)) return true
        }
        return false
    }

    fun openCustomTabOrBrowser(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)
        return try {
            val tabs = CustomTabsIntent.Builder().build()
            tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            tabs.launchUrl(context, uri)
            true
        } catch (_: Exception) {
            try {
                val view = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(view)
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
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
