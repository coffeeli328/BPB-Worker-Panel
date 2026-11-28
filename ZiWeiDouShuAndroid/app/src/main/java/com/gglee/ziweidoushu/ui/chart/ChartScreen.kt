package com.gglee.ziweidoushu.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.ziweidoushu.engine.ChartScope
import com.gglee.ziweidoushu.engine.EarthlyBranch
import com.gglee.ziweidoushu.engine.PalaceCell
import com.gglee.ziweidoushu.engine.ScopeOverlay
import com.gglee.ziweidoushu.engine.ScopeProjector
import com.gglee.ziweidoushu.engine.SiHua
import com.gglee.ziweidoushu.engine.StarCategory
import com.gglee.ziweidoushu.engine.ZiWeiChart
import com.gglee.ziweidoushu.engine.ZiWeiEngine
import com.gglee.ziweidoushu.ui.theme.AuxStar
import com.gglee.ziweidoushu.ui.theme.GoldDeep
import com.gglee.ziweidoushu.ui.theme.Ink
import com.gglee.ziweidoushu.ui.theme.MajorStar
import com.gglee.ziweidoushu.ui.theme.Mist
import com.gglee.ziweidoushu.ui.theme.Muted
import com.gglee.ziweidoushu.ui.theme.Paper
import com.gglee.ziweidoushu.ui.theme.ShaStar
import com.gglee.ziweidoushu.ui.theme.Teal
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    chart: ZiWeiChart,
    onOpenInterpretation: () -> Unit,
    onBack: () -> Unit,
) {
    var scope by remember { mutableStateOf(ChartScope.NATAL) }
    var decadeIndex by remember { mutableIntStateOf(0) }
    val birthYear = remember(chart.birthDate) {
        Calendar.getInstance().apply { time = chart.birthDate }.get(Calendar.YEAR)
    }
    var annualYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    val (viewCells, overlay) = remember(chart, scope, decadeIndex, annualYear) {
        when (scope) {
            ChartScope.NATAL -> ScopeProjector.natal(chart)
            ChartScope.DECADE -> {
                val dec = chart.decades.getOrElse(decadeIndex) { chart.decades.first() }
                ScopeProjector.decade(chart, dec)
            }
            ChartScope.ANNUAL -> {
                val sb = ZiWeiEngine.annualStemBranch(annualYear)
                ScopeProjector.annual(chart, sb, annualYear)
            }
        }
    }
    val cellsByBranch = remember(viewCells) { viewCells.associateBy { it.branch } }
    var selectedBranch by remember(chart.id) {
        mutableStateOf(chart.mingBranch)
    }
    val selected = cellsByBranch[selectedBranch]

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
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            "命宫${chart.mingBranch.hanzi} · 身宫${chart.shenBranch.hanzi} · 紫微${chart.ziweiBranch.hanzi}",
            fontSize = 13.sp,
            color = Ink,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        if (chart.natalSiHuaLine.isNotEmpty()) {
            Text(
                "生年四化 ${chart.natalSiHuaLine}",
                fontSize = 12.sp,
                color = GoldDeep,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        ScopeControls(
            scope = scope,
            onScope = { scope = it },
            decades = chart.decades,
            decadeIndex = decadeIndex,
            onDecade = { decadeIndex = it },
            annualYear = annualYear,
            onAnnualYear = { annualYear = it },
            minYear = birthYear,
        )

        Text(
            overlay.label + overlay.siHuaLine(),
            fontSize = 12.sp,
            color = Teal,
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
        )

        PalacePlate(
            cellsByBranch = cellsByBranch,
            chart = chart,
            overlay = overlay,
            selectedBranch = selectedBranch,
            onSelect = { selectedBranch = it },
        )

        Spacer(Modifier.height(14.dp))
        selected?.let { DetailPanel(it, overlay) }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopeControls(
    scope: ChartScope,
    onScope: (ChartScope) -> Unit,
    decades: List<com.gglee.ziweidoushu.engine.DecadeLimit>,
    decadeIndex: Int,
    onDecade: (Int) -> Unit,
    annualYear: Int,
    onAnnualYear: (Int) -> Unit,
    minYear: Int,
) {
    val scopes = ChartScope.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        scopes.forEachIndexed { index, s ->
            SegmentedButton(
                selected = scope == s,
                onClick = { onScope(s) },
                shape = SegmentedButtonDefaults.itemShape(index, scopes.size),
            ) { Text(s.displayName, fontSize = 13.sp) }
        }
    }
    when (scope) {
        ChartScope.DECADE -> {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                decades.forEachIndexed { i, d ->
                    FilterChip(
                        selected = decadeIndex == i,
                        onClick = { onDecade(i) },
                        label = {
                            Text("${d.ageStart}-${d.ageEnd}", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Mist,
                            labelColor = Ink,
                            selectedContainerColor = Teal.copy(alpha = 0.2f),
                            selectedLabelColor = Teal,
                        ),
                    )
                }
            }
        }
        ChartScope.ANNUAL -> {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onAnnualYear(annualYear - 1) }) { Text("上一年") }
                Text(
                    "$annualYear",
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                TextButton(onClick = { onAnnualYear(annualYear + 1) }) { Text("下一年") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    onAnnualYear(Calendar.getInstance().get(Calendar.YEAR))
                }) { Text("今年") }
                TextButton(onClick = { onAnnualYear(minYear.coerceAtLeast(1900)) }) { Text("出生年") }
            }
        }
        ChartScope.NATAL -> Unit
    }
}

private fun ScopeOverlay.siHuaLine(): String {
    if (siHua.isEmpty()) return ""
    return " · " + siHua.joinToString("、") { "${it.star.hanzi}${it.siHua.hanzi}" }
}

@Composable
private fun PalacePlate(
    cellsByBranch: Map<EarthlyBranch, PalaceCell>,
    chart: ZiWeiChart,
    overlay: ScopeOverlay,
    selectedBranch: EarthlyBranch,
    onSelect: (EarthlyBranch) -> Unit,
) {
    val top = listOf(EarthlyBranch.SI, EarthlyBranch.WU, EarthlyBranch.WEI, EarthlyBranch.SHEN)
    val bottom = listOf(EarthlyBranch.YIN, EarthlyBranch.CHOU, EarthlyBranch.ZI, EarthlyBranch.HAI)

    fun cell(b: EarthlyBranch) = cellsByBranch.getValue(b)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Ink.copy(alpha = 0.35f))
            .background(Mist.copy(alpha = 0.35f)),
    ) {
        Row(Modifier.fillMaxWidth()) {
            top.forEach { b ->
                PalaceTile(
                    cell = cell(b),
                    focus = overlay.focusBranch == b,
                    selected = selectedBranch == b,
                    onClick = { onSelect(b) },
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
                listOf(EarthlyBranch.CHEN, EarthlyBranch.MAO).forEach { b ->
                    PalaceTile(
                        cell = cell(b),
                        focus = overlay.focusBranch == b,
                        selected = selectedBranch == b,
                        onClick = { onSelect(b) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
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
                    Text(
                        overlay.scope.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldDeep,
                    )
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
                listOf(EarthlyBranch.YOU, EarthlyBranch.XU).forEach { b ->
                    PalaceTile(
                        cell = cell(b),
                        focus = overlay.focusBranch == b,
                        selected = selectedBranch == b,
                        onClick = { onSelect(b) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            bottom.forEach { b ->
                PalaceTile(
                    cell = cell(b),
                    focus = overlay.focusBranch == b,
                    selected = selectedBranch == b,
                    onClick = { onSelect(b) },
                    modifier = Modifier.weight(1f).aspectRatio(0.9f),
                )
            }
        }
    }
}

@Composable
private fun PalaceTile(
    cell: PalaceCell,
    focus: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val borderColor = when {
        selected -> Teal
        focus -> GoldDeep
        else -> Ink.copy(alpha = 0.18f)
    }
    Column(
        modifier = modifier
            .border(if (selected || focus) 1.5.dp else 0.5.dp, borderColor)
            .background(
                when {
                    selected -> Teal.copy(alpha = 0.08f)
                    focus -> GoldDeep.copy(alpha = 0.08f)
                    else -> Paper.copy(alpha = 0.7f)
                },
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val label = buildString {
                append(cell.scopeType.shortName)
                if (cell.isShen) append("身")
            }
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (cell.scopeType.ordinal == 0) GoldDeep else Ink,
            )
            Text("${cell.stem.hanzi}${cell.branch.hanzi}", fontSize = 10.sp, color = Muted)
        }
        Spacer(Modifier.height(2.dp))
        cell.majorStars.take(3).forEach { ps ->
            Text(
                starLabel(ps.star.hanzi, ps.brightness.hanzi, ps.natalSiHua, ps.scopeSiHua),
                fontSize = 11.sp,
                color = MajorStar,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        val aux = cell.otherStars.take(3).joinToString(" ") {
            starLabel(it.star.hanzi, null, it.natalSiHua, it.scopeSiHua)
        }
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

private fun starLabel(
    name: String,
    brightness: String?,
    natal: SiHua?,
    scope: SiHua?,
): String = buildString {
    append(name)
    if (brightness != null) append(brightness)
    natal?.let { append(it.hanzi) }
    if (scope != null && scope != natal) append(scope.hanzi)
}

@Composable
private fun DetailPanel(cell: PalaceCell, overlay: ScopeOverlay) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Mist.copy(alpha = 0.45f))
            .padding(12.dp),
    ) {
        Text(
            buildString {
                append(cell.scopeType.hanzi)
                if (cell.scopeType != cell.type) {
                    append("（本命")
                    append(cell.type.hanzi)
                    append("）")
                }
                append(" · ")
                append(cell.stem.hanzi)
                append(cell.branch.hanzi)
                if (cell.isMing) append(" · 命")
                if (cell.isShen) append(" · 身")
                if (overlay.focusBranch == cell.branch) append(" · 焦点")
            },
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            fontSize = 15.sp,
        )
        Text(
            "本命大限 ${cell.decadeAgeStart}–${cell.decadeAgeEnd} 岁",
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
                val marks = buildString {
                    append(ps.brightness.hanzi)
                    ps.natalSiHua?.let { append(" · 生年").append(it.hanzi) }
                    ps.scopeSiHua?.let {
                        if (overlay.scope != ChartScope.NATAL) {
                            append(" · ").append(overlay.scope.displayName).append(it.hanzi)
                        }
                    }
                }
                Text(
                    "${ps.star.hanzi}  $marks  · ${ps.star.category.displayName}",
                    color = colorForSiHua(ps.natalSiHua ?: ps.scopeSiHua) ?: color,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

private fun colorForSiHua(siHua: SiHua?): Color? = when (siHua) {
    SiHua.LU -> Color(0xFF2F6B4F)
    SiHua.QUAN -> Color(0xFF3A5A8A)
    SiHua.KE -> Color(0xFF8A6B2A)
    SiHua.JI -> ShaStar
    null -> null
}
