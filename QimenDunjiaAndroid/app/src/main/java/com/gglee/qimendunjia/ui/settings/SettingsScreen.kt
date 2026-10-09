package com.gglee.qimendunjia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.qimendunjia.ai.AiSettings
import com.gglee.qimendunjia.ui.theme.Ink
import com.gglee.qimendunjia.ui.theme.Muted
import com.gglee.qimendunjia.ui.theme.Pine

@Composable
fun SettingsScreen(aiSettings: AiSettings) {
    var baseUrl by rememberSaveable { mutableStateOf(aiSettings.baseUrl) }
    var model by rememberSaveable { mutableStateOf(aiSettings.model) }
    var apiKey by rememberSaveable { mutableStateOf(aiSettings.apiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("设置", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(20.dp))

        Text("AI 接口（OpenAI 兼容）", fontSize = 13.sp, color = Muted)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = {
                baseUrl = it
                aiSettings.baseUrl = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Base URL") },
            placeholder = { Text(AiSettings.PLACEHOLDER_BASE_URL_DEEPSEEK) },
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = model,
            onValueChange = {
                model = it
                aiSettings.model = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("模型") },
            placeholder = { Text(AiSettings.PLACEHOLDER_MODEL_DEEPSEEK) },
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                aiSettings.apiKey = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors(),
        )
        Text(
            if (aiSettings.isConfigured) "已配置密钥（仅存于本机加密存储）" else "未配置 API Key",
            fontSize = 12.sp,
            color = Muted,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(16.dp))
        RowPresets(aiSettings) { baseUrl = aiSettings.baseUrl; model = aiSettings.model }

        Spacer(Modifier.height(24.dp))
        Text("隐私说明", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Muted)
        Text(
            "排盘与规则解读均在本机完成。仅在您主动请求 AI 解读时，会将盘面摘要与所问之事发送至您配置的接口；密钥保存在本机 EncryptedSharedPreferences，不会上传至第三方服务器。",
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.85f),
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = {
            aiSettings.clearKey()
            apiKey = ""
        }) {
            Text("清除 API Key", color = Pine)
        }
    }
}

@Composable
private fun RowPresets(aiSettings: AiSettings, onApplied: () -> Unit) {
    Button(
        onClick = {
            aiSettings.applyDeepSeekPreset()
            onApplied()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Pine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("DeepSeek 预设")
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = {
            aiSettings.applyOpenAiPreset()
            onApplied()
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("OpenAI 预设")
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pine,
    cursorColor = Pine,
)
