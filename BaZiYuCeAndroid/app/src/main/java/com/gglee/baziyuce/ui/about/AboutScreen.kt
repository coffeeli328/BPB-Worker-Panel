package com.gglee.baziyuce.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.ui.components.InlineNavTitle
import com.gglee.baziyuce.ui.theme.CeladonSoft
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.Slate

/** Strict mirror of iOS `AboutView.swift` (工程路径为 Android 工程名). */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    ScreenBackground {
        Column(modifier = modifier.fillMaxSize()) {
            InlineNavTitle("关于")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    "八字运势",
                    color = Ink,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                )
                Text(
                    "本地四柱排盘与规则化流年 / 流月 / 流日提示。预测文案由十神、地支关系与五行中和规则生成，可在 `PredictionEngine` 中扩展。",
                    color = Slate,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                )

                InfoGroup(
                    title = "计算口径",
                    body = "· 年柱：立春换年\n· 月柱：十二节换月（五虎遁）\n· 日柱：儒略日干支；子时换日\n· 时柱：五鼠遁",
                )
                InfoGroup(
                    title = "残差说明",
                    body = "节气交节时刻采用 Meeus 低精度太阳黄经近似，1900–2100 通常与精密历差数分钟；交节前后极短窗口可能与专业历书差一日。本 App 供学习与参考，不构成决策建议。",
                )
                InfoGroup(
                    title = "工程",
                    body = "路径：`BaZiYuCeAndroid/`\n需要 Android Studio + JDK 17、SDK 34。包名 `com.gglee.baziyuce`。界面结构对齐 iOS `BaZiYuCe`。",
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoGroup(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.45f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            color = CeladonSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Text(body, color = Ink, fontSize = 17.sp, lineHeight = 22.sp)
    }
}
