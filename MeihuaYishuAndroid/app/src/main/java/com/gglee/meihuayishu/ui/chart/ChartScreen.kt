package com.gglee.meihuayishu.ui.chart

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.meihuayishu.engine.Hexagram
import com.gglee.meihuayishu.engine.MeihuaChart
import com.gglee.meihuayishu.engine.Tone
import com.gglee.meihuayishu.export.ChartImageExporter
import com.gglee.meihuayishu.ui.theme.Cinnabar
import com.gglee.meihuayishu.ui.theme.Ink
import com.gglee.meihuayishu.ui.theme.Muted
import com.gglee.meihuayishu.ui.theme.Paper
import com.gglee.meihuayishu.ui.theme.Pine
import com.gglee.meihuayishu.ui.theme.Plum
import com.gglee.meihuayishu.ui.theme.Wash

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    chart: MeihuaChart,
    onOpenInterpretation: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("解盘") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Paper,
                    titleContentColor = Ink,
                ),
                actions = {
                    IconButton(onClick = {
                        ChartImageExporter.copyPrompt(context, chart)
                        Toast.makeText(context, "已复制 AI 提示词", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "复制提示词")
                    }
                    IconButton(onClick = {
                        ChartImageExporter.shareImageAndPrompt(context, chart)
                    }) {
                        Icon(Icons.Outlined.Image, contentDescription = "导出图片")
                    }
                },
            )
        },
        containerColor = Paper,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (chart.hasQuestion) {
                Text(chart.question, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            }
            Text(
                "${chart.method.label} · ${chart.summaryTitle}",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HexColumn("本卦", chart.original, chart.movingLine)
                HexColumn("互卦", chart.mutual, movingLine = null)
                HexColumn("变卦", chart.changed, movingLine = null)
            }

            Spacer(Modifier.height(20.dp))
            InfoBlock {
                Text("体用", color = Pine, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "体 ${chart.ti.displayName}（${chart.ti.element.hanzi}）　用 ${chart.yong.displayName}（${chart.yong.element.hanzi}）",
                    color = Ink,
                    fontSize = 16.sp,
                )
                Text(
                    "${chart.tiYongRelation.label} — ${chart.tiYongRelation.hint}",
                    color = Plum,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            InfoBlock {
                Text("起卦明细", color = Pine, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(chart.methodDetail, color = Ink, fontSize = 14.sp)
                if (chart.ganzhiLine.isNotBlank()) {
                    Text("干支 ${chart.ganzhiLine}", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("规则纲要", color = Pine, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            chart.interpretations.forEach { item ->
                val toneColor = when (item.tone) {
                    Tone.POSITIVE -> Pine
                    Tone.CAUTION -> Cinnabar
                    Tone.NEUTRAL -> Ink
                }
                Text(item.title, color = toneColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    item.detail,
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    ChartImageExporter.shareImageAndPrompt(context, chart)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Plum),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导出图片给其他 AI")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenInterpretation,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Outlined.Psychology, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("应用内 AI 解读")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoBlock(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Wash)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun HexColumn(title: String, hex: Hexagram, movingLine: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Pine, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(hex.name, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        HexagramGlyph(hex = hex, movingLine = movingLine)
        Text(
            "${hex.upper.symbol}${hex.lower.symbol}",
            color = Muted,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun HexagramGlyph(
    hex: Hexagram,
    movingLine: Int?,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .width(88.dp)
            .height(140.dp),
    ) {
        val w = size.width
        val gap = size.height / 7f
        val stroke = 7.dp.toPx()
        // draw top (上爻=index5) first
        for (visual in 0..5) {
            val lineIndex = 5 - visual
            val y = gap * (visual + 1)
            val yang = hex.lines[lineIndex]
            val color = if (movingLine != null && lineIndex + 1 == movingLine) Cinnabar else Ink
            if (yang) {
                drawLine(
                    color = color,
                    start = Offset(w * 0.1f, y),
                    end = Offset(w * 0.9f, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            } else {
                drawLine(
                    color = color,
                    start = Offset(w * 0.1f, y),
                    end = Offset(w * 0.42f, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.58f, y),
                    end = Offset(w * 0.9f, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
