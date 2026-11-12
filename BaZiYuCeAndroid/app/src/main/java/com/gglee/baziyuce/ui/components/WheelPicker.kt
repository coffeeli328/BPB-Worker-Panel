package com.gglee.baziyuce.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gglee.baziyuce.ui.theme.Ink
import com.gglee.baziyuce.ui.theme.Line
import com.gglee.baziyuce.ui.theme.Mist
import com.gglee.baziyuce.ui.theme.Slate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

/**
 * iOS `.pickerStyle(.wheel)` stand-in: snap list, center selection, fade edges.
 * Avoids Android [NumberPicker] chrome.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelIntPicker(
    value: Int,
    values: List<Int>,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 36.dp,
    visibleCount: Int = 5,
    label: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit,
) {
    val safeValues = values.ifEmpty { listOf(value) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val listHeight = itemHeight * visibleCount
    val half = visibleCount / 2

    val initialIndex = safeValues.indexOf(value).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val onChangeState = rememberUpdatedState(onValueChange)

    // Keep list synced when value changes externally (e.g. day clamp)
    LaunchedEffect(value, safeValues) {
        val target = safeValues.indexOf(value).coerceAtLeast(0)
        val centered = listState.firstVisibleItemIndex
        if (abs(centered - target) > 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(target)
        }
    }

    // Emit selection when scroll settles on a row
    LaunchedEffect(listState, safeValues) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val viewportCenter = layout.viewportStartOffset + layout.viewportSize.height / 2f
            layout.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2f) - viewportCenter)
            }?.index
        }
            .filter { it != null }
            .distinctUntilChanged()
            .collect { index ->
                val i = index ?: return@collect
                if (!listState.isScrollInProgress) {
                    val next = safeValues.getOrNull(i) ?: return@collect
                    if (next != value) onChangeState.value(next)
                }
            }
    }

    // Also update while scrolling near center (iOS wheel feel)
    val centeredIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val viewportCenter = layout.viewportStartOffset + layout.viewportSize.height / 2f
            layout.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2f) - viewportCenter)
            }?.index ?: initialIndex
        }
    }

    LaunchedEffect(centeredIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val next = safeValues.getOrNull(centeredIndex) ?: return@LaunchedEffect
            if (next != value) onChangeState.value(next)
        }
    }

    Box(
        modifier = modifier
            .height(listHeight)
            .drawWithContent {
                drawContent()
                // iOS-like selection hairlines
                val y1 = size.height / 2f - itemHeightPx / 2f
                val y2 = size.height / 2f + itemHeightPx / 2f
                drawLine(Line, Offset(0f, y1), Offset(size.width, y1), strokeWidth = 1f)
                drawLine(Line, Offset(0f, y2), Offset(size.width, y2), strokeWidth = 1f)
                // Fade top/bottom
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Mist.copy(alpha = 0.85f),
                        0.35f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1f to Mist.copy(alpha = 0.85f),
                    ),
                )
            },
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = itemHeight * half),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(safeValues.size, key = { safeValues[it] }) { index ->
                val selected = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(safeValues[index]),
                        color = if (selected) Ink else Slate.copy(alpha = 0.45f),
                        fontSize = if (selected) 18.sp else 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
