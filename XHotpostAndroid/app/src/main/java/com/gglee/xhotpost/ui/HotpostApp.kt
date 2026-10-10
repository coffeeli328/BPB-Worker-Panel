package com.gglee.xhotpost.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gglee.xhotpost.AppContainer
import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.ContentLanguage
import com.gglee.xhotpost.domain.Draft
import com.gglee.xhotpost.domain.DraftStatus
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.domain.NicheId
import com.gglee.xhotpost.ui.theme.HotpostTheme

private sealed class Tab(val route: String, val label: String) {
    data object Review : Tab("review", "审核")
    data object Topics : Tab("topics", "热点")
    data object Settings : Tab("settings", "设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotpostApp(container: AppContainer) {
    HotpostTheme {
        val vm: HotpostViewModel = viewModel(factory = HotpostViewModelFactory(container))
        val state by vm.uiState.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(state.toast) {
            state.toast?.let {
                snackbar.showSnackbar(it)
                vm.clearToast()
            }
        }

        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text("热帖") },
                    actions = {
                        TextButton(onClick = { vm.runTick() }, enabled = !state.busy) {
                            Text(if (state.busy) "处理中…" else "跑一轮")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf(Tab.Review, Tab.Topics, Tab.Settings).forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.navigate(tab.route) { launchSingleTop = true } },
                            icon = {
                                Icon(
                                    when (tab) {
                                        Tab.Review -> Icons.Outlined.Article
                                        Tab.Topics -> Icons.Outlined.Whatshot
                                        Tab.Settings -> Icons.Outlined.Settings
                                    },
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Tab.Review.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(Tab.Review.route) {
                    ReviewScreen(state = state, vm = vm)
                }
                composable(Tab.Topics.route) {
                    TopicsScreen(topics = state.topics)
                }
                composable(Tab.Settings.route) {
                    SettingsScreen(settings = state.settings, onSave = vm::saveSettings)
                }
            }
        }
    }
}

@Composable
private fun ReviewScreen(state: HotpostUiState, vm: HotpostViewModel) {
    val edits = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(state.pendingDrafts) {
        state.pendingDrafts.forEach { draft ->
            if (!edits.containsKey(draft.id)) edits[draft.id] = draft.text
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StatsRow(state)
            Banner(state.settings.demoMode)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        }
        if (state.pendingDrafts.isEmpty()) {
            item {
                Text(
                    "暂无待审草稿。点右上角「跑一轮」或等待后台定时任务。",
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            items(state.pendingDrafts, key = { it.id }) { draft ->
                DraftCard(
                    draft = draft,
                    text = edits[draft.id] ?: draft.text,
                    onTextChange = { edits[draft.id] = it },
                    onSave = { vm.saveDraft(draft.id, edits[draft.id] ?: draft.text) },
                    onReject = { vm.rejectDraft(draft.id) },
                    onApprove = { vm.approveDraft(draft.id, edits[draft.id] ?: draft.text) },
                    busy = state.busy,
                )
            }
        }
        if (state.recentDrafts.isNotEmpty()) {
            item { Text("最近处理", style = MaterialTheme.typography.titleMedium) }
            items(state.recentDrafts, key = { it.id }) { draft ->
                RecentDraftCard(draft)
            }
        }
    }
}

@Composable
private fun StatsRow(state: HotpostUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatChip("${state.stats.pendingReview}", "待审核")
        StatChip("${state.stats.published}", "已发布")
        StatChip("${state.stats.draftsCreated}", "草稿")
        StatChip("${state.settings.pollIntervalMinutes}m", "后台")
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun Banner(demoMode: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = if (demoMode) {
                "演示模式：不会打开 X。关闭后，通过审核会跳转到 X 应用发帖（不用 X API）。"
            } else {
                "非演示：审核通过后会打开 X/Twitter 应用，文案已填好，你只需点发送。"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftCard(
    draft: Draft,
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onReject: () -> Unit,
    onApprove: () -> Unit,
    busy: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(draft.topicTitle, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            Text("${text.length}/280", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSave, enabled = !busy) { Text("保存") }
                OutlinedButton(onClick = onReject, enabled = !busy) { Text("拒绝") }
                Button(onClick = onApprove, enabled = !busy) { Text("通过并发布") }
            }
        }
    }
}

@Composable
private fun RecentDraftCard(draft: Draft) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${draft.topicTitle} · ${statusLabel(draft.status)}", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(draft.text, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TopicsScreen(topics: List<HotTopic>) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("当前热点", style = MaterialTheme.typography.titleLarge)
            Text("来自 Google News RSS，按你的赛道过滤。", color = MaterialTheme.colorScheme.outline)
        }
        if (topics.isEmpty()) {
            item { Text("暂无热点，先跑一轮。") }
        } else {
            items(topics, key = { it.id }) { topic ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(topic.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${topic.source} · 热度 ${topic.score}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        topic.url?.let { url ->
                            TextButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) { Text("来源") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(settings: AppSettings, onSave: (AppSettings) -> Unit) {
    var form by remember(settings) { androidx.compose.runtime.mutableStateOf(settings) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("赛道与变现", style = MaterialTheme.typography.titleLarge)
            Text("手机端全自动发帖需你在 X 里确认发送；不使用 X API。", color = MaterialTheme.colorScheme.outline)
        }
        item {
            OutlinedTextField(
                value = form.displayName,
                onValueChange = { form = form.copy(displayName = it) },
                label = { Text("显示名") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            NicheChips(form.niche) { form = form.copy(niche = it) }
        }
        if (form.niche == NicheId.CUSTOM) {
            item {
                OutlinedTextField(
                    value = form.customNicheLabel,
                    onValueChange = { form = form.copy(customNicheLabel = it) },
                    label = { Text("自定义赛道") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            LanguageChips(form.language) { form = form.copy(language = it) }
        }
        item {
            OutlinedTextField(
                value = form.affiliateUrl,
                onValueChange = { form = form.copy(affiliateUrl = it) },
                label = { Text("变现链接") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = form.ctaTemplate,
                onValueChange = { form = form.copy(ctaTemplate = it) },
                label = { Text("CTA 模板（{link}）") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            ToggleRow("演示模式", form.demoMode) { form = form.copy(demoMode = it) }
            ToggleRow("自动写草稿", form.autoDraft) { form = form.copy(autoDraft = it) }
            ToggleRow("通过后打开 X", form.autoPublishApproved) {
                form = form.copy(autoPublishApproved = it)
            }
        }
        item {
            OutlinedTextField(
                value = form.pollIntervalMinutes.toString(),
                onValueChange = { v -> form = form.copy(pollIntervalMinutes = v.toIntOrNull() ?: 30) },
                label = { Text("后台间隔（分钟，≥15）") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(onClick = { onSave(form) }, modifier = Modifier.fillMaxWidth()) {
                Text("保存设置")
            }
        }
    }
}

@Composable
private fun NicheChips(selected: NicheId, onSelect: (NicheId) -> Unit) {
    val labels = listOf(
        NicheId.TECH to "科技",
        NicheId.FINANCE to "财经",
        NicheId.LIFESTYLE to "生活",
        NicheId.CREATOR to "自媒体",
        NicheId.LOCAL to "综合",
        NicheId.CUSTOM to "自定义",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun LanguageChips(selected: ContentLanguage, onSelect: (ContentLanguage) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ContentLanguage.ZH to "中文",
            ContentLanguage.EN to "英文",
            ContentLanguage.MIXED to "混合",
        ).forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private fun statusLabel(status: DraftStatus): String = when (status) {
    DraftStatus.PENDING_REVIEW -> "待审核"
    DraftStatus.APPROVED -> "已通过"
    DraftStatus.REJECTED -> "已拒绝"
    DraftStatus.PUBLISHED -> "已发布"
    DraftStatus.FAILED -> "失败"
}
