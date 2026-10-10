package com.gglee.xhotpost.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.net.URLEncoder

/** Opens X / Twitter without X API. */
object XPublisher {
    private val X_PACKAGES = listOf(
        "com.twitter.android",
        "com.twitter.android.lite",
    )

    /** Prefer stable login pages. Avoid x.com/i/flow/login (often fails). */
    private val LOGIN_URLS = listOf(
        "https://mobile.twitter.com/login",
        "https://twitter.com/login",
        "https://x.com/login",
        "https://mobile.x.com/login",
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
        // Use chooser + ACTION_VIEW (more reliable than Custom Tabs on some OEMs / networks)
        for (url in LOGIN_URLS) {
            if (openUrlChooser(context, url, "选择浏览器打开 X 登录")) return true
        }
        return false
    }

    fun openLoginUrl(context: Context, which: Int): Boolean {
        val url = LOGIN_URLS.getOrElse(which) { LOGIN_URLS.first() }
        return openUrlChooser(context, url, "打开登录页")
    }

    fun openXApp(context: Context): Boolean {
        val appContext = context.applicationContext
        val pm = appContext.packageManager

        for (pkg in X_PACKAGES) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (startSafely(appContext, launch)) return true
            }
        }

        val deepLinks = listOf(
            "twitter://timeline",
            "twitter://login",
            "https://twitter.com/home",
            "https://x.com/home",
        )
        for (link in deepLinks) {
            for (pkg in X_PACKAGES) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(pkg)
                }
                if (startSafely(appContext, intent)) return true
            }
            val any = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startSafely(appContext, any)) return true
        }

        return openLoginInBrowser(context)
    }

    fun openPlayStoreForX(context: Context): Boolean {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=com.twitter.android"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (startSafely(context, market)) return true
        return openUrlChooser(
            context,
            "https://play.google.com/store/apps/details?id=com.twitter.android",
            "安装 X",
        )
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
                if (startSafely(appContext, share)) return true
            }
        }

        val encoded = URLEncoder.encode(text, Charsets.UTF_8.name())
        val urls = listOf(
            "https://twitter.com/intent/tweet?text=$encoded",
            "https://x.com/intent/post?text=$encoded",
            "https://mobile.twitter.com/compose/tweet?text=$encoded",
        )
        for (url in urls) {
            if (openUrlChooser(context, url, "选择应用发帖")) return true
        }
        return false
    }

    private fun openUrlChooser(context: Context, url: String, title: String): Boolean {
        val uri = Uri.parse(url)
        val view = Intent(Intent.ACTION_VIEW, uri)
        return try {
            val chooser = Intent.createChooser(view, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Exception) {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun startSafely(context: Context, intent: Intent): Boolean {
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
