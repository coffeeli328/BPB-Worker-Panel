package com.gglee.xhotpost.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gglee.xhotpost.AppContainer
import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.DashboardStats
import com.gglee.xhotpost.domain.Draft
import com.gglee.xhotpost.domain.DraftStatus
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.work.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HotpostUiState(
    val settings: AppSettings = AppSettings(),
    val stats: DashboardStats = DashboardStats(),
    val pendingDrafts: List<Draft> = emptyList(),
    val recentDrafts: List<Draft> = emptyList(),
    val topics: List<HotTopic> = emptyList(),
    val busy: Boolean = false,
    val toast: String? = null,
    val error: String? = null,
)

class HotpostViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val repo = container.repository
    private val busy = MutableStateFlow(false)
    private val toast = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val dataState = combine(
        repo.settings,
        repo.stats,
        repo.drafts,
        repo.topics,
    ) { settings, stats, drafts, topics ->
        DataBundle(settings, stats, drafts, topics)
    }

    val uiState: StateFlow<HotpostUiState> = combine(
        dataState,
        busy,
        toast,
        error,
    ) { data, isBusy, toastMsg, err ->
        HotpostUiState(
            settings = data.settings,
            stats = data.stats,
            pendingDrafts = data.drafts.filter { it.status == DraftStatus.PENDING_REVIEW },
            recentDrafts = data.drafts.filter { it.status != DraftStatus.PENDING_REVIEW }.take(8),
            topics = data.topics.take(12),
            busy = isBusy,
            toast = toastMsg,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HotpostUiState())

    fun clearToast() {
        toast.value = null
    }

    fun runTick() {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            try {
                val result = withContext(Dispatchers.IO) { repo.runTick() }
                toast.value =
                    "完成：热点 ${result.topics} · 新草稿 ${result.drafts} · 发布 ${result.published}"
            } catch (e: Exception) {
                error.value = e.message ?: "任务失败"
            } finally {
                busy.value = false
            }
        }
    }

    fun saveDraft(id: String, text: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.saveDraftText(id, text) }
            toast.value = "草稿已保存"
        }
    }

    fun rejectDraft(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.rejectDraft(id) }
            toast.value = "已拒绝"
        }
    }

    fun approveDraft(id: String, text: String) {
        viewModelScope.launch {
            busy.value = true
            try {
                val draft = withContext(Dispatchers.IO) { repo.approveDraft(id, text) }
                toast.value = when {
                    draft.status == DraftStatus.PUBLISHED && draft.demo ->
                        "已通过并在演示模式发布"
                    draft.status == DraftStatus.PUBLISHED ->
                        "已打开 X，请确认发送"
                    draft.status == DraftStatus.FAILED ->
                        "发布失败：${draft.publishError ?: "未知"}"
                    else -> "已通过"
                }
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                busy.value = false
            }
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.updateSettings { settings }
            }
            try {
                SyncWorker.schedule(container.appContext, settings.pollIntervalMinutes.toLong())
            } catch (_: Exception) {
                // WorkManager 在部分设备上可能尚未就绪，忽略
            }
            toast.value = "设置已保存"
        }
    }

    private data class DataBundle(
        val settings: AppSettings,
        val stats: DashboardStats,
        val drafts: List<Draft>,
        val topics: List<HotTopic>,
    )
}

class HotpostViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HotpostViewModel::class.java)) {
            return HotpostViewModel(container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
