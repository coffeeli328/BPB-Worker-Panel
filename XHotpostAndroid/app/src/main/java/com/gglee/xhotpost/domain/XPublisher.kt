package com.gglee.xhotpost.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/** Opens X / Twitter compose with pre-filled text — no X API. */
object XPublisher {
    const val TWITTER_PACKAGE = "com.twitter.android"
    const val X_PACKAGE = "com.twitter.android"

    fun openCompose(context: Context, text: String): Boolean {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(TWITTER_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (share.resolveActivity(context.packageManager) != null) {
            context.startActivity(share)
            return true
        }

        val encoded = URLEncoder.encode(text, Charsets.UTF_8.name())
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://twitter.com/intent/tweet?text=$encoded"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (web.resolveActivity(context.packageManager) != null) {
            context.startActivity(web)
            return true
        }
        return false
    }
}
