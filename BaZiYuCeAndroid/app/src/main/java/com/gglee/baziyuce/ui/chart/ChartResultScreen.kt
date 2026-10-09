package com.gglee.baziyuce.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.engine.BaZiChart
import com.gglee.baziyuce.engine.TenGods
import com.gglee.baziyuce.engine.WuXing
import com.gglee.baziyuce.ui.theme.Accent
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.CeladonSoft
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.Line
import com.gglee.baziyuce.ui.theme.MistDeep
import com.gglee.baziyuce.ui.theme.ScreenBackground
import com.gglee.baziyuce.ui.theme.Slate
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChartResultScreen(
    session: BaZiSessionViewModel,
    modifier: Modifier = Modifier,
) {
    ScreenBackground {
        val chart = session.chart
        if (chart == null) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = CeladonSoft, modifier = Modifier.height(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("请先在「起盘」输入出生时间", color = Slate)
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Header(chart)
                PillarsRow(chart)
                DayMasterBlock(chart)
                FiveElementsBlock(chart)
                Text(chart.solarTermNote, style = MaterialTheme.typography.bodySmall, color = Slate)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Header(chart: BaZiChart) {
    val fmt = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(chart.gender.displayName + "命", color = Celadon, style = MaterialTheme.typography.titleMedium)
        Text(fmt.format(chart.birthDate), style = MaterialTheme.typography.headlineLarge, color = Ink)
    }
}

@Composable
private fun PillarsRow(chart: BaZiChart) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        chart.pillars.forEach { p ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Line, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(p.kind.displayName, color = Slate, fontSize = 12.sp)
                    Text(p.stemBranch.stem.displayName, color = Ink, fontSize = 28.sp, fontFamily = MaterialTheme.typography.headlineLarge.fontFamily)
                    Text(p.stemBranch.branch.displayName, color = Celadon, fontSize = 28.sp, fontFamily = MaterialTheme.typography.headlineLarge.fontFamily)
                    Text(
                        p.stemGod?.displayName ?: "日主",
                        color = Accent,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayMasterBlock(chart: BaZiChart) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("日主", style = MaterialTheme.typography.titleMedium, color = CeladonSoft)
        Text(
            "${chart.dayMaster.displayName}（${chart.dayMaster.yinYang.displayName}${chart.dayMaster.wuXing.displayName}）",
            style = MaterialTheme.typography.headlineMedium,
            color = Ink,
        )
        Text(
            "十神均以日主为基准推算；日支藏干主气为「${TenGods.ofBranchMain(chart.day.branch, chart.dayMaster).displayName}」。",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate,
        )
    }
}

@Composable
private fun FiveElementsBlock(chart: BaZiChart) {
    val maxV = WuXing.entries.maxOf { chart.balance.score(it) }.coerceAtLeast(0.1)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("五行概览", style = MaterialTheme.typography.titleMedium, color = CeladonSoft)
            Text(chart.balance.summary, style = MaterialTheme.typography.bodyLarge, color = Ink)
            WuXing.entries.forEach { wx ->
                val value = chart.balance.score(wx)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(wx.displayName, color = Ink)
                        Text(String.format(Locale.CHINA, "%.1f", value), color = Slate, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MistDeep.copy(alpha = 0.5f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((value / maxV).toFloat().coerceIn(0.05f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(CeladonSoft.copy(alpha = 0.85f)),
                        )
                    }
                }
            }
        }
    }
}
