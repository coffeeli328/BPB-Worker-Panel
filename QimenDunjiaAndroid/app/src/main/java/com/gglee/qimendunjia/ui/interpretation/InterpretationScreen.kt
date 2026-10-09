package com.gglee.qimendunjia.ui.interpretation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.qimendunjia.AppContainer
import com.gglee.qimendunjia.engine.InterpretationItem
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.ui.theme.Cinnabar
import com.gglee.qimendunjia.ui.theme.Ink
import com.gglee.qimendunjia.ui.theme.Muted
import com.gglee.qimendunjia.ui.theme.Paper
import com.gglee.qimendunjia.ui.theme.Pine
import com.gglee.qimendunjia.ui.theme.Wash

@Composable
fun InterpretationScreen(
    chart: QimenChart,
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: InterpretationViewModel = viewModel(
        factory = InterpretationViewModelFactory(container),
    ),
) {
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(chart.id) {
        viewModel.loadCached(chart)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        if (chart.hasQuestion) {
            Text(chart.question, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(
                "事项：${chart.questionTopic.rawValue} · 规则模板，仅供参考",
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
        } else {
            Text(
                "尚未填写所问之事。请返回起局页填写后再排盘，以获得针对性解读。",
                fontSize = 14.sp,
                color = Cinnabar,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        SectionHeader("本机规则解读")
        chart.interpretations.forEach { item ->
            RuleRow(item)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("AI 辅助解读")
        if (!container.aiSettings.isConfigured) {
            Text(
                "尚未配置 API Key。可在「设置」中填写 Base URL、模型与密钥后使用。",
                fontSize = 13.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Button(
            onClick = { viewModel.requestAi(chart) },
            enabled = !uiState.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Pine),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
            } else {
                Text("生成 AI 解读")
            }
        }
        uiState.errorMessage?.let { err ->
            Text(err, color = Cinnabar, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (uiState.aiText.isNotEmpty()) {
            Text(
                if (uiState.loadedFromCache) "（已缓存）" else "",
                fontSize = 11.sp,
                color = Muted,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                uiState.aiText,
                fontSize = 15.sp,
                color = Ink,
                lineHeight = 22.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(Wash.copy(alpha = 0.5f))
                    .padding(12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
            Text("返回盘面")
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun RuleRow(item: InterpretationItem) {
    val toneColor = when (item.tone) {
        InterpretationItem.Tone.AUSPICIOUS -> Pine
        InterpretationItem.Tone.CAUTION -> Cinnabar
        InterpretationItem.Tone.NEUTRAL -> Muted
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .width(3.dp)
                .height(48.dp)
                .background(toneColor),
        ) {}
        Spacer(Modifier.width(12.dp))
        Column {
            Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(item.detail, fontSize = 14.sp, color = Ink.copy(alpha = 0.85f), lineHeight = 20.sp)
        }
    }
}
