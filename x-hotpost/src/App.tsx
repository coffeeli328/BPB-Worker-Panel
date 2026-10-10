import { useEffect, useState, type FormEvent } from 'react'
import {
  approveDraft,
  fetchOverview,
  patchSettings,
  rejectDraft,
  runTick,
  updateDraft,
  type Draft,
  type Overview,
  type PublicSettings,
} from './api'

type Tab = 'review' | 'settings' | 'logs'

const nicheOptions = [
  { id: 'tech', label: '科技 / AI' },
  { id: 'finance', label: '财经投资' },
  { id: 'lifestyle', label: '生活方式' },
  { id: 'creator', label: '自媒体变现' },
  { id: 'local', label: '综合热点' },
  { id: 'custom', label: '自定义' },
] as const

const statusLabel: Record<Draft['status'], string> = {
  pending_review: '待审核',
  approved: '已通过',
  rejected: '已拒绝',
  published: '已发布',
  failed: '发布失败',
}

function formatTime(value?: string | null) {
  if (!value) return '尚未运行'
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false,
  })
}

export default function App() {
  const [data, setData] = useState<Overview | null>(null)
  const [tab, setTab] = useState<Tab>('review')
  const [busy, setBusy] = useState(false)
  const [toast, setToast] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [edits, setEdits] = useState<Record<string, string>>({})
  const [settingsForm, setSettingsForm] = useState<PublicSettings | null>(null)

  async function refresh() {
    const overview = await fetchOverview()
    setData(overview)
    setSettingsForm(overview.settings)
    setEdits((prev) => {
      const next = { ...prev }
      for (const draft of overview.drafts) {
        if (next[draft.id] === undefined) next[draft.id] = draft.text
      }
      return next
    })
  }

  useEffect(() => {
    void refresh().catch((err: unknown) => {
      setError(err instanceof Error ? err.message : String(err))
    })
    const timer = setInterval(() => {
      void refresh().catch(() => undefined)
    }, 15_000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(null), 2600)
    return () => clearTimeout(timer)
  }, [toast])

  async function onTick() {
    setBusy(true)
    setError(null)
    try {
      const result = await runTick()
      await refresh()
      setToast(
        `完成一轮：热点 ${result.topics} · 新草稿 ${result.drafts} · 发布 ${result.published}`,
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function onApprove(draft: Draft) {
    setBusy(true)
    setError(null)
    try {
      const result = await approveDraft(draft.id, edits[draft.id] ?? draft.text)
      await refresh()
      setToast(
        result.draft.status === 'published'
          ? result.demo
            ? '已通过并在演示模式发布'
            : '已通过并发布到 X'
          : '已通过，等待自动发布',
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function onReject(draft: Draft) {
    setBusy(true)
    try {
      await rejectDraft(draft.id)
      await refresh()
      setToast('已拒绝该草稿')
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function onSaveText(draft: Draft) {
    setBusy(true)
    try {
      await updateDraft(draft.id, { text: edits[draft.id] ?? draft.text })
      await refresh()
      setToast('草稿已保存')
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function onSaveSettings(event: FormEvent) {
    event.preventDefault()
    if (!settingsForm) return
    setBusy(true)
    setError(null)
    try {
      await patchSettings({
        displayName: settingsForm.displayName,
        niche: settingsForm.niche,
        customNicheLabel: settingsForm.customNicheLabel,
        language: settingsForm.language,
        persona: settingsForm.persona,
        affiliateUrl: settingsForm.affiliateUrl,
        affiliateLabel: settingsForm.affiliateLabel,
        ctaTemplate: settingsForm.ctaTemplate,
        autoDraft: settingsForm.autoDraft,
        autoPublishApproved: settingsForm.autoPublishApproved,
        pollIntervalMinutes: settingsForm.pollIntervalMinutes,
        maxDraftsPerTick: settingsForm.maxDraftsPerTick,
        demoMode: settingsForm.demoMode,
        x: {
          apiKey: settingsForm.x.apiKey,
          apiSecret: settingsForm.x.apiSecret,
          accessToken: settingsForm.x.accessToken,
          accessTokenSecret: settingsForm.x.accessTokenSecret,
        },
        openaiCompatible: settingsForm.openaiCompatible,
      })
      await refresh()
      setToast('设置已保存')
      setTab('review')
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  if (!data || !settingsForm) {
    return (
      <div className="app-shell">
        <div className="brand">
          <div className="brand-mark">
            <span className="brand-dot" />
            <h1>热帖</h1>
          </div>
          <p>{error ? `加载失败：${error}` : '正在启动后台流水线…'}</p>
        </div>
      </div>
    )
  }

  const pending = data.drafts.filter((d) => d.status === 'pending_review')
  const recent = data.drafts.filter((d) => d.status !== 'pending_review').slice(0, 8)

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <div className="brand-mark">
            <span className="brand-dot" />
            <h1>热帖</h1>
          </div>
          <p>
            自动跟踪热点、写好草稿，你只负责审核。通过后后台发布，少盯屏也能持续输出。
          </p>
        </div>
        <div className="top-actions">
          <button className="btn btn-ghost" type="button" onClick={() => setTab('settings')}>
            账号与变现
          </button>
          <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void onTick()}>
            {busy ? '处理中…' : '立即跑一轮'}
          </button>
        </div>
      </header>

      <div className="banner">
        {data.settings.demoMode
          ? '当前是演示模式：热点与发布都可本地跑通，不会真实发到 X。配好 API 后关闭演示即可上线。'
          : data.settings.x.configured
            ? '已连接 X 凭证。通过审核的内容会按设置自动发布。'
            : '演示已关闭，但尚未配置完整 X API 凭证；发布会回退为本地演示。'}
      </div>

      <section className="stats">
        <div className="stat">
          <strong>{data.worker.pendingReview}</strong>
          <span>待你审核</span>
        </div>
        <div className="stat">
          <strong>{data.stats.published}</strong>
          <span>已发布</span>
        </div>
        <div className="stat">
          <strong>{data.stats.draftsCreated}</strong>
          <span>累计草稿</span>
        </div>
        <div className="stat">
          <strong>{data.worker.pollIntervalMinutes}m</strong>
          <span>后台间隔 · {formatTime(data.worker.lastTickAt)}</span>
        </div>
      </section>

      {error ? <div className="banner">{error}</div> : null}

      <div className="tabs">
        <button
          className={`tab ${tab === 'review' ? 'active' : ''}`}
          type="button"
          onClick={() => setTab('review')}
        >
          审核台
        </button>
        <button
          className={`tab ${tab === 'settings' ? 'active' : ''}`}
          type="button"
          onClick={() => setTab('settings')}
        >
          设置
        </button>
        <button
          className={`tab ${tab === 'logs' ? 'active' : ''}`}
          type="button"
          onClick={() => setTab('logs')}
        >
          后台日志
        </button>
      </div>

      {tab === 'review' ? (
        <div className="layout">
          <section className="panel">
            <h2>待审核发布</h2>
            <p className="sub">改文案 → 通过 / 拒绝。通过后若开启自动发布，会立刻发出。</p>
            <div className="draft-list">
              {pending.length === 0 ? (
                <div className="empty">暂无待审草稿。点右上角「立即跑一轮」，或等后台定时抓热点。</div>
              ) : (
                pending.map((draft) => (
                  <article className="draft" key={draft.id}>
                    <div className="draft-head">
                      <h3>{draft.topicTitle}</h3>
                      <span className="badge badge-pending">{statusLabel[draft.status]}</span>
                    </div>
                    <textarea
                      value={edits[draft.id] ?? draft.text}
                      onChange={(event) =>
                        setEdits((prev) => ({ ...prev, [draft.id]: event.target.value }))
                      }
                      maxLength={280}
                    />
                    <div className="meta">
                      {(edits[draft.id] ?? draft.text).length}/280 · 钩子：{draft.monetizationHook}
                    </div>
                    <div className="draft-actions">
                      <button className="btn btn-ghost" type="button" disabled={busy} onClick={() => void onSaveText(draft)}>
                        仅保存
                      </button>
                      <button className="btn btn-danger" type="button" disabled={busy} onClick={() => void onReject(draft)}>
                        拒绝
                      </button>
                      <button className="btn btn-ok" type="button" disabled={busy} onClick={() => void onApprove(draft)}>
                        通过并发布
                      </button>
                    </div>
                  </article>
                ))
              )}
            </div>

            {recent.length > 0 ? (
              <>
                <h2 style={{ marginTop: '1.4rem' }}>最近处理</h2>
                <div className="draft-list" style={{ marginTop: '0.75rem' }}>
                  {recent.map((draft) => (
                    <article className="draft" key={draft.id}>
                      <div className="draft-head">
                        <h3>{draft.topicTitle}</h3>
                        <span className={`badge badge-${draft.status}`}>{statusLabel[draft.status]}</span>
                      </div>
                      <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.45 }}>{draft.text}</p>
                      <div className="meta">
                        {draft.publishedAt
                          ? `发布于 ${formatTime(draft.publishedAt)}${draft.externalPostId ? ` · ${draft.externalPostId}` : ''}`
                          : `更新于 ${formatTime(draft.updatedAt)}`}
                        {draft.publishError ? ` · ${draft.publishError}` : ''}
                      </div>
                    </article>
                  ))}
                </div>
              </>
            ) : null}
          </section>

          <aside className="panel">
            <h2>当前热点</h2>
            <p className="sub">按你的赛道聚合公开资讯源，再据此写草稿。</p>
            <div className="topic-list">
              {data.topics.length === 0 ? (
                <div className="empty">还没有热点数据。</div>
              ) : (
                data.topics.map((topic) => (
                  <div className="topic" key={topic.id}>
                    <strong>{topic.title}</strong>
                    <span>
                      {topic.source} · 热度 {topic.score}
                      {topic.url ? (
                        <>
                          {' · '}
                          <a href={topic.url} target="_blank" rel="noreferrer">
                            来源
                          </a>
                        </>
                      ) : null}
                    </span>
                  </div>
                ))
              )}
            </div>
          </aside>
        </div>
      ) : null}

      {tab === 'settings' ? (
        <section className="panel">
          <h2>账号、赛道与变现</h2>
          <p className="sub">
            X 发帖需 Developer App 的 OAuth 1.0a 用户凭证（按次计费）。先用演示模式验证流程，再填密钥上线。
          </p>
          <form className="form-grid" onSubmit={(event) => void onSaveSettings(event)}>
            <div className="form-row">
              <label>
                品牌显示名
                <input
                  value={settingsForm.displayName}
                  onChange={(e) => setSettingsForm({ ...settingsForm, displayName: e.target.value })}
                />
              </label>
              <label>
                内容赛道
                <select
                  value={settingsForm.niche}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      niche: e.target.value as PublicSettings['niche'],
                    })
                  }
                >
                  {nicheOptions.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {settingsForm.niche === 'custom' ? (
              <label>
                自定义赛道关键词
                <input
                  value={settingsForm.customNicheLabel}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, customNicheLabel: e.target.value })
                  }
                  placeholder="例如：温哥华房产"
                />
              </label>
            ) : null}

            <div className="form-row">
              <label>
                语言
                <select
                  value={settingsForm.language}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      language: e.target.value as PublicSettings['language'],
                    })
                  }
                >
                  <option value="zh">中文为主</option>
                  <option value="en">英文为主</option>
                  <option value="mixed">中英混合</option>
                </select>
              </label>
              <label>
                后台间隔（分钟）
                <input
                  type="number"
                  min={5}
                  max={240}
                  value={settingsForm.pollIntervalMinutes}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      pollIntervalMinutes: Number(e.target.value) || 30,
                    })
                  }
                />
              </label>
            </div>

            <label>
              人设 / 语气
              <textarea
                rows={3}
                value={settingsForm.persona}
                onChange={(e) => setSettingsForm({ ...settingsForm, persona: e.target.value })}
              />
            </label>

            <div className="form-row">
              <label>
                变现链接（联盟 / 产品 / 社群）
                <input
                  value={settingsForm.affiliateUrl}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, affiliateUrl: e.target.value })
                  }
                  placeholder="https://"
                />
              </label>
              <label>
                链接文案标签
                <input
                  value={settingsForm.affiliateLabel}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, affiliateLabel: e.target.value })
                  }
                />
              </label>
            </div>

            <label>
              CTA 模板（可用 {'{link}'}）
              <input
                value={settingsForm.ctaTemplate}
                onChange={(e) => setSettingsForm({ ...settingsForm, ctaTemplate: e.target.value })}
              />
            </label>

            <div className="form-row">
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={settingsForm.demoMode}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, demoMode: e.target.checked })
                  }
                />
                演示模式（不真实发帖）
              </label>
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={settingsForm.autoDraft}
                  onChange={(e) =>
                    setSettingsForm({ ...settingsForm, autoDraft: e.target.checked })
                  }
                />
                自动生成待审草稿
              </label>
            </div>

            <div className="form-row">
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={settingsForm.autoPublishApproved}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      autoPublishApproved: e.target.checked,
                    })
                  }
                />
                审核通过后自动发布
              </label>
              <label>
                每轮最多新草稿
                <input
                  type="number"
                  min={1}
                  max={10}
                  value={settingsForm.maxDraftsPerTick}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      maxDraftsPerTick: Number(e.target.value) || 3,
                    })
                  }
                />
              </label>
            </div>

            <h2 style={{ marginTop: '0.6rem' }}>X API（OAuth 1.0a）</h2>
            <p className="sub">在 developer.x.com 创建 App，生成 API Key/Secret 与用户 Access Token/Secret。</p>
            <div className="form-row">
              <label>
                API Key
                <input
                  value={settingsForm.x.apiKey}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      x: { ...settingsForm.x, apiKey: e.target.value },
                    })
                  }
                  placeholder={settingsForm.x.configured ? '已配置（可覆盖）' : ''}
                />
              </label>
              <label>
                API Secret
                <input
                  type="password"
                  value={settingsForm.x.apiSecret}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      x: { ...settingsForm.x, apiSecret: e.target.value },
                    })
                  }
                  placeholder="留空表示不修改"
                />
              </label>
            </div>
            <div className="form-row">
              <label>
                Access Token
                <input
                  value={settingsForm.x.accessToken}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      x: { ...settingsForm.x, accessToken: e.target.value },
                    })
                  }
                />
              </label>
              <label>
                Access Token Secret
                <input
                  type="password"
                  value={settingsForm.x.accessTokenSecret}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      x: { ...settingsForm.x, accessTokenSecret: e.target.value },
                    })
                  }
                  placeholder="留空表示不修改"
                />
              </label>
            </div>

            <h2 style={{ marginTop: '0.6rem' }}>可选：AI 写稿</h2>
            <p className="sub">兼容 OpenAI 接口。不填则用本地模板写稿，照样能跑通审核流。</p>
            <label className="toggle">
              <input
                type="checkbox"
                checked={Boolean(settingsForm.openaiCompatible?.enabled)}
                onChange={(e) =>
                  setSettingsForm({
                    ...settingsForm,
                    openaiCompatible: {
                      enabled: e.target.checked,
                      baseUrl: settingsForm.openaiCompatible?.baseUrl ?? 'https://api.openai.com/v1',
                      apiKey: settingsForm.openaiCompatible?.apiKey ?? '',
                      model: settingsForm.openaiCompatible?.model ?? 'gpt-4o-mini',
                    },
                  })
                }
              />
              启用 AI 写稿
            </label>
            <div className="form-row">
              <label>
                Base URL
                <input
                  value={settingsForm.openaiCompatible?.baseUrl ?? ''}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      openaiCompatible: {
                        enabled: settingsForm.openaiCompatible?.enabled ?? false,
                        baseUrl: e.target.value,
                        apiKey: settingsForm.openaiCompatible?.apiKey ?? '',
                        model: settingsForm.openaiCompatible?.model ?? 'gpt-4o-mini',
                      },
                    })
                  }
                />
              </label>
              <label>
                Model
                <input
                  value={settingsForm.openaiCompatible?.model ?? ''}
                  onChange={(e) =>
                    setSettingsForm({
                      ...settingsForm,
                      openaiCompatible: {
                        enabled: settingsForm.openaiCompatible?.enabled ?? false,
                        baseUrl:
                          settingsForm.openaiCompatible?.baseUrl ?? 'https://api.openai.com/v1',
                        apiKey: settingsForm.openaiCompatible?.apiKey ?? '',
                        model: e.target.value,
                      },
                    })
                  }
                />
              </label>
            </div>
            <label>
              API Key
              <input
                type="password"
                value={settingsForm.openaiCompatible?.apiKey ?? ''}
                onChange={(e) =>
                  setSettingsForm({
                    ...settingsForm,
                    openaiCompatible: {
                      enabled: settingsForm.openaiCompatible?.enabled ?? false,
                      baseUrl:
                        settingsForm.openaiCompatible?.baseUrl ?? 'https://api.openai.com/v1',
                      apiKey: e.target.value,
                      model: settingsForm.openaiCompatible?.model ?? 'gpt-4o-mini',
                    },
                  })
                }
                placeholder="留空表示不修改"
              />
            </label>

            <div className="draft-actions">
              <button className="btn btn-primary" type="submit" disabled={busy}>
                保存设置
              </button>
            </div>
          </form>
        </section>
      ) : null}

      {tab === 'logs' ? (
        <section className="panel">
          <h2>后台日志</h2>
          <p className="sub">抓热点、写草稿、发布结果都会记在这里。</p>
          <div className="log-list">
            {data.logs.length === 0 ? (
              <div className="empty">暂无日志。</div>
            ) : (
              data.logs.map((log) => (
                <div
                  className={`log-item ${log.level === 'error' ? 'log-error' : ''} ${log.level === 'warn' ? 'log-warn' : ''}`}
                  key={log.id}
                >
                  <time>{formatTime(log.at)}</time>
                  <span>
                    [{log.level}] {log.message}
                  </span>
                </div>
              ))
            )}
          </div>
        </section>
      ) : null}

      {toast ? <div className="toast">{toast}</div> : null}
    </div>
  )
}
