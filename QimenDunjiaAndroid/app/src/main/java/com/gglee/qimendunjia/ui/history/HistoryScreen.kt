package com.gglee.qimendunjia.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.qimendunjia.AppContainer
import com.gglee.qimendunjia.data.ChartSnapshotCodec
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.ui.theme.Ink
import com.gglee.qimendunjia.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    container: AppContainer,
    onOpenChart: (QimenChart) -> Unit,
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(container.historyRepository),
    ),
) {
    val items by viewModel.items.collectAsState()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text("暂无历史记录", color = Muted, fontSize = 16.sp)
            Text("起局排盘后会自动保存于此。", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val chart = ChartSnapshotCodec.decode(row.chartJson)
                        onOpenChart(chart)
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(row.summary, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink)
                if (row.question.isNotBlank()) {
                    Text(row.question, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    formatter.format(Date(row.createdAt)),
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            HorizontalDivider()
        }
    }
}
