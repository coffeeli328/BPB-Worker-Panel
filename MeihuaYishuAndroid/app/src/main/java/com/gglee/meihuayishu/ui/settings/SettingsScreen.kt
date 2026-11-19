package com.gglee.meihuayishu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.meihuayishu.ai.AiSettings
import com.gglee.meihuayishu.ui.theme.Ink
import com.gglee.meihuayishu.ui.theme.Muted
import com.gglee.meihuayishu.ui.theme.Pine
import com.gglee.meihuayishu.ui.theme.Wash

@Composable
fun SettingsScreen(aiSettings: AiSettings) {
    var baseUrl by remember { mutableStateOf(aiSettings.baseUrl) }
    var model by remember { mutableStateOf(aiSettings.model) }
    var apiKey by remember { mutableStateOf(aiSettings.apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var savedHint by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(
            "可选：配置 OpenAI 兼容接口做应用内解读。不解也可导出图片给外部 AI。",
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        Text("AI 接口", color = Pine, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = {
                aiSettings.applyDeepSeekPreset()
                baseUrl = aiSettings.baseUrl
                model = aiSettings.model
            }) { Text("DeepSeek") }
            Spacer(Modifier.padding(6.dp))
            OutlinedButton(onClick = {
                aiSettings.applyOpenAiPreset()
                baseUrl = aiSettings.baseUrl
                model = aiSettings.model
            }) { Text("OpenAI") }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Base URL") },
            placeholder = { Text(AiSettings.PLACEHOLDER_BASE_URL_DEEPSEEK) },
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model") },
            placeholder = { Text(AiSettings.PLACEHOLDER_MODEL_DEEPSEEK) },
            colors = fieldColors(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors(),
            trailingIcon = {
                OutlinedButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "隐藏" else "显示")
                }
            },
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                aiSettings.baseUrl = baseUrl.trim()
                aiSettings.model = model.trim()
                aiSettings.apiKey = apiKey.trim()
                savedHint = "已保存"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Pine),
        ) {
            Text("保存")
        }
        OutlinedButton(
            onClick = {
                aiSettings.clearKey()
                apiKey = ""
                savedHint = "已清除 API Key"
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text("清除密钥")
        }
        savedHint?.let {
            Text(it, color = Pine, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(28.dp))
        Text("关于", color = Pine, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            "梅花易数学习参考 App。时间起卦用先天数（年支+月+日+时支）。" +
                "体用、互变与五行生克为本地规则纲要；导出图片可交给 ChatGPT / Claude 等解读。",
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pine,
    unfocusedBorderColor = Wash,
    focusedLabelColor = Pine,
    cursorColor = Pine,
)
