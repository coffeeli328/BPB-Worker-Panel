package com.gglee.xhotpost

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.gglee.xhotpost.data.SettingsStore
import com.gglee.xhotpost.domain.XPublisher
import kotlinx.coroutines.launch

/**
 * Opens stable Twitter/X login pages (not i/flow/login) or the official app,
 * then lets the user confirm login. No X API.
 */
class XLoginActivity : ComponentActivity() {
    private lateinit var txtHint: TextView
    private lateinit var txtAppStatus: TextView
    private lateinit var editUsername: EditText
    private var openedExternal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_x_login)

        txtHint = findViewById(R.id.txtHint)
        txtAppStatus = findViewById(R.id.txtAppStatus)
        editUsername = findViewById(R.id.editUsername)

        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }

        val installed = XPublisher.isXAppInstalled(this)
        txtAppStatus.text = if (installed) {
            "已检测到 X/Twitter App，优先用 App 登录更稳"
        } else {
            "未安装 X App。网页若打不开，请先点「去应用商店安装 X」"
        }

        findViewById<Button>(R.id.btnBrowserLogin).setOnClickListener {
            openLogin(0, "已打开 mobile.twitter.com/login")
        }
        findViewById<Button>(R.id.btnAltLogin).setOnClickListener {
            openLogin(1, "已打开 twitter.com/login")
        }
        findViewById<Button>(R.id.btnOpenXApp).setOnClickListener {
            openedExternal = true
            val ok = XPublisher.openXApp(this)
            toast(
                if (ok) "已尝试打开 X。登录后返回点「我已登录成功」"
                else "打不开 X App，请先安装",
            )
        }
        findViewById<Button>(R.id.btnInstallX).setOnClickListener {
            openedExternal = true
            val ok = XPublisher.openPlayStoreForX(this)
            toast(if (ok) "请安装 X 后返回再登录" else "打不开应用商店")
        }
        findViewById<Button>(R.id.btnConfirmLoggedIn).setOnClickListener {
            confirmLoggedIn()
        }
    }

    private fun openLogin(index: Int, okMsg: String) {
        openedExternal = true
        val ok = XPublisher.openLoginUrl(this, index)
        toast(
            if (ok) "$okMsg。登录后返回点「我已登录成功」"
            else "打不开浏览器。请安装 Chrome，或改用 X App",
        )
    }

    override fun onResume() {
        super.onResume()
        if (openedExternal) {
            txtHint.text =
                "若浏览器里 flow/login 报错，请改用「mobile.twitter.com」或官方 App。登录成功后点下方按钮。"
        }
    }

    private fun confirmLoggedIn() {
        val username = editUsername.text.toString().trim()
        lifecycleScope.launch {
            SettingsStore(applicationContext).update {
                it.copy(
                    xLoggedIn = true,
                    xUsername = username,
                    demoMode = false,
                )
            }
            toast("已保存：X 已登录")
            setResult(
                RESULT_OK,
                Intent()
                    .putExtra(EXTRA_LOGGED_IN, true)
                    .putExtra(EXTRA_USERNAME, username),
            )
            finish()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_LOGGED_IN = "logged_in"
        const val EXTRA_USERNAME = "username"
    }
}
