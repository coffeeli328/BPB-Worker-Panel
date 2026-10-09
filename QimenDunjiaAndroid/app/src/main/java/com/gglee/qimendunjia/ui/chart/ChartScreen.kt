package com.gglee.qimendunjia.ui.chart

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.qimendunjia.engine.Palace
import com.gglee.qimendunjia.engine.PalaceCell
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.ui.theme.Cinnabar
import com.gglee.qimendunjia.ui.theme.EarthStem
import com.gglee.qimendunjia.ui.theme.HeavenStem
import com.gglee.qimendunjia.ui.theme.Ink
import com.gglee.qimendunjia.ui.theme.Muted
import com.gglee.qimendunjia.ui.theme.Pine
import com.gglee.qimendunjia.ui.theme.Wash
import com.gglee.qimendunjia.ui.theme.WashDeep

@Composable
fun ChartScreen(
    chart: QimenChart,
    onOpenInterpretation: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(chart.juTitle, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(chart.ganzhiLine, fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        if (chart.hasQuestion) {
            Text(
                "所问：${chart.question}",
                fontSize = 15.sp,
                color = Ink,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Text(
            "${chart.solarTermName} · ${chart.locationNote} · ${if (chart.usedTrueSolarTime) "真太阳时" else "民用时"}",
            fontSize = 12.sp,
            color = Muted,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        Text(
            "值符 ${chart.zhiFuStar.hanzi}@${chart.zhiFuPalace.hanzi} · 值使 ${chart.zhiShiGate.displayName}@${chart.zhiShiPalace.hanzi} · 旬空 ${chart.xunKong.joinToString("") { it.hanzi }}",
            fontSize = 13.sp,
            color = Ink,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        PalaceGrid(chart = chart)

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onOpenInterpretation,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Pine),
        ) {
            Text(if (chart.hasQuestion) "问事解读" else "简要解读")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        ) {
            Text("返回")
        }
    }
}

@Composable
private fun PalaceGrid(chart: QimenChart) {
    Column {
        LegendRow()
        Spacer(Modifier.height(8.dp))
        Text("南", fontSize = 10.sp, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("东", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(end = 4.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Wash.copy(alpha = 0.55f))
                    .border(1.5.dp, Ink.copy(alpha = 0.35f)),
            ) {
                Palace.gridOrder.forEachIndexed { rowIndex, row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { colIndex, palace ->
                            PalaceCellView(
                                cell = chart.cell(palace),
                                isYangDun = chart.isYangDun,
                                modifier = Modifier.weight(1f),
                            )
                            if (colIndex < 2) {
                                Box(
                                    Modifier
                                        .width(1.dp)
                                        .height(108.dp)
                                        .background(Color(0x331F241F)),
                                )
                            }
                        }
                    }
                    if (rowIndex < 2) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x331F241F)),
                        )
                    }
                }
            }
            Text("西", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(start = 4.dp))
        }
        Text("北", fontSize = 10.sp, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(HeavenStem, "天盘干")
        LegendDot(EarthStem, "地盘干")
        Text("符 值符", fontSize = 11.sp, color = Muted)
        Text("使 值使", fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .height(5.dp)
                .padding(0.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                .then(Modifier.height(5.dp)),
        )
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun PalaceCellView(
    cell: PalaceCell?,
    isYangDun: Boolean,
    modifier: Modifier = Modifier,
) {
    val isZhong = cell?.palace == Palace.ZHONG5
    val bg = when {
        cell?.isZhiFu == true || cell?.isZhiShi == true -> WashDeep.copy(alpha = 0.45f)
        isZhong -> Wash.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    Column(
        modifier = modifier
            .height(108.dp)
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                cell?.palace?.let { "${it.hanzi}${it.rawValue}" } ?: "",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            if (cell?.isEmpty == true) {
                Text("空", fontSize = 9.sp, color = Muted)
            }
            if (cell?.isZhiFu == true) {
                Text("符", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Cinnabar)
            }
            if (cell?.isZhiShi == true) {
                Text("使", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Pine)
            }
        }
        if (!isZhong) {
            Text(
                cell?.deity?.name(isYangDun) ?: "·",
                fontSize = 10.sp,
                color = Muted,
                maxLines = 1,
            )
        } else {
            Text("中五", fontSize = 10.sp, color = Muted)
        }
        Text(
            cell?.star?.hanzi ?: "·",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            maxLines = 1,
        )
        Row {
            Text(cell?.heavenStem?.hanzi ?: "·", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HeavenStem)
            Text("·", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(horizontal = 2.dp))
            Text(cell?.earthStem?.hanzi ?: "·", fontSize = 13.sp, color = EarthStem)
        }
        if (!isZhong) {
            Text(cell?.gate?.displayName ?: "·", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Pine, maxLines = 1)
        }
    }
}
