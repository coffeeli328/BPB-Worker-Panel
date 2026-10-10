package com.gglee.xhotpost

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.gglee.xhotpost.data.SettingsStore
import kotlinx.coroutines.launch

class XLoginActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_x_login)

        val webView = findViewById<WebView>(R.id.webView)
        val hint = findViewById<TextView>(R.id.txtHint)
        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString =
            webView.settings.userAgentString.replace("; wv", "")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean = false

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val u = url.orEmpty()
                hint.text = "当前：$u"
                if (looksLoggedIn(u)) {
                    CookieManager.getInstance().flush()
                    lifecycleScope.launch {
                        SettingsStore(applicationContext).update {
                            it.copy(xLoggedIn = true)
                        }
                    }
                    Toast.makeText(
                        this@XLoginActivity,
                        "已检测到登录，状态已保存",
                        Toast.LENGTH_LONG,
                    ).show()
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_LOGGED_IN, true))
                    finish()
                }
            }
        }

        webView.loadUrl("https://x.com/i/flow/login")
    }

    private fun looksLoggedIn(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains("login") || lower.contains("flow/login") || lower.contains("logout")) {
            return false
        }
        val cookie = CookieManager.getInstance().getCookie("https://x.com").orEmpty()
        val hasAuth = cookie.contains("auth_token=")
        return hasAuth && (
            lower.contains("/home") ||
                lower.contains("x.com/home") ||
                lower.contains("twitter.com/home")
            )
    }

    companion object {
        const val EXTRA_LOGGED_IN = "logged_in"
    }
}
