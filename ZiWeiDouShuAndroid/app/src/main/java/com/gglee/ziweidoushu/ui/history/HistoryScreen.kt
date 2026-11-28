package com.gglee.ziweidoushu.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gglee.ziweidoushu.AppContainer
import com.gglee.ziweidoushu.data.ChartSnapshotCodec
import com.gglee.ziweidoushu.data.HistoryEntity
import com.gglee.ziweidoushu.data.HistoryRepository
import com.gglee.ziweidoushu.engine.ZiWeiChart
import com.gglee.ziweidoushu.ui.theme.Ink
import com.gglee.ziweidoushu.ui.theme.Muted
import com.gglee.ziweidoushu.ui.theme.ShaStar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(private val repo: HistoryRepository) : ViewModel() {
    val items = repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun open(entity: HistoryEntity): ZiWeiChart = ChartSnapshotCodec.decode(entity.chartJson)

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}

class HistoryViewModelFactory(private val repo: HistoryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel(repo) as T
}

@Composable
fun HistoryScreen(
    container: AppContainer,
    onOpenChart: (ZiWeiChart) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(container.historyRepository)),
) {
    val items by viewModel.items.collectAsState()
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("历史命盘", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(
            "本地保存，可随时回看",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        if (items.isEmpty()) {
            Text("暂无记录，去「排盘」起一盘吧。", color = Muted, fontSize = 14.sp)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items, key = { it.id }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChart(viewModel.open(row)) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(row.summary, fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Medium)
                            Text(
                                fmt.format(Date(row.createdAt)),
                                fontSize = 12.sp,
                                color = Muted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        IconButton(onClick = { viewModel.delete(row.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = ShaStar)
                        }
                    }
                }
            }
        }
    }
}
