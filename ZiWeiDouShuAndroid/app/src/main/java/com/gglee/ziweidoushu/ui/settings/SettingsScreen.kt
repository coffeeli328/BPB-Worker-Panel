package com.gglee.ziweidoushu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.ziweidoushu.ai.AiSettings
import com.gglee.ziweidoushu.ui.theme.Ink
import com.gglee.ziweidoushu.ui.theme.Mist
import com.gglee.ziweidoushu.ui.theme.Muted
import com.gglee.ziweidoushu.ui.theme.Teal

@Composable
fun SettingsScreen(aiSettings: AiSettings) {
    var baseUrl by remember { mutableStateOf(aiSettings.baseUrl) }
    var model by remember { mutableStateOf(aiSettings.model) }
    var apiKey by remember { mutableStateOf(aiSettings.apiKey) }
    var savedHint by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(
            "排盘默认完全本地；AI 为可选项。",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        Text("AI（OpenAI 兼容）", fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(10.dp))
        Field("Base URL", baseUrl, "https://api.deepseek.com/v1") { baseUrl = it }
        Spacer(Modifier.height(8.dp))
        Field("Model", model, "deepseek-chat") { model = it }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                aiSettings.applyDeepSeekPreset()
                baseUrl = aiSettings.baseUrl
                model = aiSettings.model
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("填入 DeepSeek 预设") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                aiSettings.baseUrl = baseUrl.trim()
                aiSettings.model = model.trim()
                aiSettings.apiKey = apiKey.trim()
                savedHint = "已保存"
            },
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存") }
        savedHint?.let {
            Text(it, color = Teal, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(28.dp))
        Text("关于", fontWeight = FontWeight.SemiBold, color = Ink)
        Text(
            "紫微斗数 Android · 三合派安星 v1.1\n农历 1900–2100 · 十四主星 + 辅煞 + 四化\n本命 / 大限 / 流年 · 解读仅供学习参考",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Field(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        colors = fieldColors(),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Teal,
    unfocusedBorderColor = Muted.copy(alpha = 0.4f),
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedContainerColor = Mist.copy(alpha = 0.3f),
    unfocusedContainerColor = Mist.copy(alpha = 0.15f),
)
