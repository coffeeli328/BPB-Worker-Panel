package com.gglee.ziweidoushu.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.ziweidoushu.engine.EarthlyBranch
import com.gglee.ziweidoushu.engine.PalaceCell
import com.gglee.ziweidoushu.engine.StarCategory
import com.gglee.ziweidoushu.engine.ZiWeiChart
import com.gglee.ziweidoushu.ui.theme.AuxStar
import com.gglee.ziweidoushu.ui.theme.GoldDeep
import com.gglee.ziweidoushu.ui.theme.Ink
import com.gglee.ziweidoushu.ui.theme.MajorStar
import com.gglee.ziweidoushu.ui.theme.Mist
import com.gglee.ziweidoushu.ui.theme.Muted
import com.gglee.ziweidoushu.ui.theme.Paper
import com.gglee.ziweidoushu.ui.theme.ShaStar
import com.gglee.ziweidoushu.ui.theme.Teal

@Composable
fun ChartScreen(
    chart: ZiWeiChart,
    onOpenInterpretation: () -> Unit,
    onBack: () -> Unit,
) {
    var selected: PalaceCell? by remember { mutableStateOf(chart.cells.firstOrNull { it.isMing }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mist.copy(0.4f), Paper)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(chart.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(chart.ganzhiLine, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        Text(
            "${chart.lunarLine} · ${chart.calendarNote}",
            fontSize = 12.sp,
            color = Muted,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        Text(
            "命宫${chart.mingBranch.hanzi} · 身宫${chart.shenBranch.hanzi} · 紫微${chart.ziweiBranch.hanzi}",
            fontSize = 13.sp,
            color = Ink,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        PalacePlate(chart = chart, selected = selected, onSelect = { selected = it })

        Spacer(Modifier.height(14.dp))
        selected?.let { DetailPanel(it) }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onOpenInterpretation,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
        ) { Text("命盘解读") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        ) { Text("返回") }
    }
}

@Composable
private fun PalacePlate(
    chart: ZiWeiChart,
    selected: PalaceCell?,
    onSelect: (PalaceCell) -> Unit,
) {
    val top = listOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.SHEN)
    val bottom = listOf(EarthlyBranch.YIN, EarthlyBranch.CHOU, EarthlyBranch.ZI, EarthlyBranch.HAI)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Ink.copy(alpha = 0.35f))
            .background(Mist.copy(alpha = 0.35f)),
    ) {
        Row(Modifier.fillMaxWidth()) {
            top.forEach { b ->
                PalaceTile(
                    cell = chart.cellByBranch(b),
                    selected = selected?.branch == b,
                    onClick = { onSelect(chart.cellByBranch(b)) },
                    modifier = Modifier.weight(1f).aspectRatio(0.9f),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
        ) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                PalaceTile(
                    cell = chart.cellByBranch(EarthlyBranch.CHEN),
                    selected = selected?.branch == EarthlyBranch.CHEN,
                    onClick = { onSelect(chart.cellByBranch(EarthlyBranch.CHEN)) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PalaceTile(
                    cell = chart.cellByBranch(EarthlyBranch.MAO),
                    selected = selected?.branch == EarthlyBranch.MAO,
                    onClick = { onSelect(chart.cellByBranch(EarthlyBranch.MAO)) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .border(0.5.dp, Ink.copy(alpha = 0.12f))
                    .background(Paper),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("紫微斗数", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GoldDeep)
                    Text(chart.fiveElementBureau.displayName, fontSize = 15.sp, color = Ink)
                    Text(chart.gender.displayName + "命", fontSize = 12.sp, color = Muted)
                    if (chart.name.isNotBlank()) {
                        Text(
                            chart.name,
                            fontSize = 12.sp,
                            color = Teal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                PalaceTile(
                    cell = chart.cellByBranch(EarthlyBranch.YOU),
                    selected = selected?.branch == EarthlyBranch.YOU,
                    onClick = { onSelect(chart.cellByBranch(EarthlyBranch.YOU)) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PalaceTile(
                    cell = chart.cellByBranch(EarthlyBranch.XU),
                    selected = selected?.branch == EarthlyBranch.XU,
                    onClick = { onSelect(chart.cellByBranch(EarthlyBranch.XU)) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            bottom.forEach { b ->
                PalaceTile(
                    cell = chart.cellByBranch(b),
                    selected = selected?.branch == b,
                    onClick = { onSelect(chart.cellByBranch(b)) },
                    modifier = Modifier.weight(1f).aspectRatio(0.9f),
                )
            }
        }
    }
}

@Composable
private fun PalaceTile(
    cell: PalaceCell,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .border(if (selected) 1.5.dp else 0.5.dp, if (selected) Teal else Ink.copy(alpha = 0.18f))
            .background(if (selected) Teal.copy(alpha = 0.08f) else Paper.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                cell.type.shortName + if (cell.isShen) "身" else "",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (cell.isMing) GoldDeep else Ink,
            )
            Text("${cell.stem.hanzi}${cell.branch.hanzi}", fontSize = 10.sp, color = Muted)
        }
        Spacer(Modifier.height(2.dp))
        cell.majorStars.take(3).forEach { ps ->
            Text(
                "${ps.star.hanzi}${ps.brightness.hanzi}",
                fontSize = 11.sp,
                color = MajorStar,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        val aux = cell.otherStars.take(3).joinToString(" ") { it.star.hanzi }
        if (aux.isNotEmpty()) {
            Text(aux, fontSize = 9.sp, color = AuxStar, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.weight(1f, fill = true))
        Text(
            "${cell.decadeAgeStart}-${cell.decadeAgeEnd}",
            fontSize = 9.sp,
            color = Muted,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DetailPanel(cell: PalaceCell) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Mist.copy(alpha = 0.45f))
            .padding(12.dp),
    ) {
        Text(
            "${cell.type.hanzi} · ${cell.stem.hanzi}${cell.branch.hanzi}" +
                (if (cell.isMing) " · 命" else "") +
                (if (cell.isShen) " · 身" else ""),
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            fontSize = 15.sp,
        )
        Text(
            "大限 ${cell.decadeAgeStart}–${cell.decadeAgeEnd} 岁",
            fontSize = 12.sp,
            color = Muted,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        if (cell.stars.isEmpty()) {
            Text("本宫无星", color = Muted, fontSize = 13.sp)
        } else {
            cell.stars.forEach { ps ->
                val color = when (ps.star.category) {
                    StarCategory.MAJOR -> MajorStar
                    StarCategory.AUXILIARY -> AuxStar
                    StarCategory.MISC -> ShaStar
                }
                Text(
                    "${ps.star.hanzi}  ${ps.brightness.hanzi}  · ${ps.star.category.displayName}",
                    color = color,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}
