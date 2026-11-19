package com.gglee.meihuayishu.ui.interpretation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.meihuayishu.AppContainer
import com.gglee.meihuayishu.engine.MeihuaChart
import com.gglee.meihuayishu.export.ChartImageExporter
import com.gglee.meihuayishu.ui.theme.Cinnabar
import com.gglee.meihuayishu.ui.theme.Ink
import com.gglee.meihuayishu.ui.theme.Muted
import com.gglee.meihuayishu.ui.theme.Paper
import com.gglee.meihuayishu.ui.theme.Pine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterpretationScreen(
    chart: MeihuaChart,
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: InterpretationViewModel = viewModel(
        factory = InterpretationViewModelFactory(chart, container),
    ),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 解读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper, titleContentColor = Ink),
            )
        },
        containerColor = Paper,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(chart.summaryTitle, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 18.sp)
            Text(
                if (container.aiSettings.isConfigured) "将调用你配置的模型" else "尚未配置 API Key，也可导出图片给外部 AI",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Button(
                onClick = { viewModel.run() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pine),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.height(22.dp), color = Paper, strokeWidth = 2.dp)
                } else {
                    Text(if (state.text == null) "开始解读" else "重新解读")
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    ChartImageExporter.shareImageAndPrompt(context, chart)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("改用：导出图片 + 提示词")
            }

            OutlinedButton(
                onClick = {
                    ChartImageExporter.copyPrompt(context, chart)
                    Toast.makeText(context, "已复制提示词", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("仅复制提示词")
            }

            state.error?.let {
                Text(it, color = Cinnabar, modifier = Modifier.padding(top = 16.dp))
            }
            state.text?.let {
                Spacer(Modifier.height(20.dp))
                Text("解读结果", color = Pine, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(it, color = Ink, fontSize = 15.sp, lineHeight = 22.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
