package com.gglee.meihuayishu.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.gglee.meihuayishu.AppContainer
import com.gglee.meihuayishu.data.ChartSnapshotCodec
import com.gglee.meihuayishu.engine.MeihuaChart
import com.gglee.meihuayishu.ui.theme.Ink
import com.gglee.meihuayishu.ui.theme.Muted
import com.gglee.meihuayishu.ui.theme.Pine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    container: AppContainer,
    onOpenChart: (MeihuaChart) -> Unit,
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(container.historyRepository),
    ),
) {
    val rows by viewModel.rows.collectAsState()
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("历史解盘", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text("本地保存，点击可再次查看与导出", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        if (rows.isEmpty()) {
            Text("暂无记录，先去起一卦吧。", color = Muted)
        } else {
            LazyColumn {
                items(rows, key = { it.id }) { row ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val chart = ChartSnapshotCodec.decode(row.chartJson)
                                onOpenChart(chart)
                            }
                            .padding(vertical = 14.dp),
                    ) {
                        Text(row.summary, color = Ink, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        if (row.question.isNotBlank()) {
                            Text(row.question, color = Pine, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text(fmt.format(Date(row.createdAt)), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    HorizontalDivider(color = Muted.copy(alpha = 0.2f))
                }
            }
        }
    }
}
