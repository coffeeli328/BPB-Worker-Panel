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
 * X blocks most in-app WebViews. This screen opens the system browser / official app,
 * then lets the user confirm login (no X API).
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
            "检测到本机已安装 X/Twitter App"
        } else {
            "未检测到 X App。可先用浏览器登录；真发帖建议安装官方 App。"
        }

        findViewById<Button>(R.id.btnBrowserLogin).setOnClickListener {
            openedExternal = true
            val ok = XPublisher.openLoginInBrowser(this)
            Toast.makeText(
                this,
                if (ok) "已打开浏览器，请登录后返回本页点「我已登录成功」"
                else "无法打开浏览器，请检查是否安装了 Chrome / 系统浏览器",
                Toast.LENGTH_LONG,
            ).show()
        }

        findViewById<Button>(R.id.btnOpenXApp).setOnClickListener {
            openedExternal = true
            val ok = XPublisher.openXApp(this)
            Toast.makeText(
                this,
                if (ok) "已尝试打开 X。请在 App 内登录后返回，点「我已登录成功」"
                else "打不开 X App。请先安装官方 X，或改用浏览器登录",
                Toast.LENGTH_LONG,
            ).show()
        }

        findViewById<Button>(R.id.btnConfirmLoggedIn).setOnClickListener {
            confirmLoggedIn()
        }
    }

    override fun onResume() {
        super.onResume()
        if (openedExternal) {
            txtHint.text =
                "如果你已经在浏览器或 X App 里登录成功，请点下方「我已登录成功」。"
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
            Toast.makeText(this@XLoginActivity, "已保存：X 已登录", Toast.LENGTH_LONG).show()
            setResult(
                RESULT_OK,
                Intent()
                    .putExtra(EXTRA_LOGGED_IN, true)
                    .putExtra(EXTRA_USERNAME, username),
            )
            finish()
        }
    }

    companion object {
        const val EXTRA_LOGGED_IN = "logged_in"
        const val EXTRA_USERNAME = "username"
    }
}
