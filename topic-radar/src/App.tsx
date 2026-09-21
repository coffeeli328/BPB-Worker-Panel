import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api, type Article, type CollectResult, type Topic } from './api'

function formatTime(value?: string | null): string {
  if (!value) return '时间未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export default function App() {
  const [topics, setTopics] = useState<Topic[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [articles, setArticles] = useState<Article[]>([])
  const [query, setQuery] = useState('')
  const [busy, setBusy] = useState(false)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [topicName, setTopicName] = useState('')
  const [topicKeywords, setTopicKeywords] = useState('')
  const [sourceName, setSourceName] = useState('')
  const [sourceUrl, setSourceUrl] = useState('')

  const activeTopic = topics.find((topic) => topic.id === activeId) || null

  async function refreshTopics(preferredId?: string) {
    const { topics: next } = await api.listTopics()
    setTopics(next)
    const nextActive = preferredId || activeId || next[0]?.id || null
    setActiveId(nextActive)
    return nextActive
  }

  async function refreshArticles(topicId: string, q = query) {
    const { articles: next } = await api.getArticles(topicId, q)
    setArticles(next)
  }

  useEffect(() => {
    refreshTopics()
      .then(async (id) => {
        if (id) await refreshArticles(id)
      })
      .catch((err: Error) => setError(err.message))
  }, [])

  useEffect(() => {
    if (!activeId) return
    const handle = window.setTimeout(() => {
      refreshArticles(activeId, query).catch((err: Error) => setError(err.message))
    }, 220)
    return () => window.clearTimeout(handle)
  }, [activeId, query])

  async function handleCollect() {
    if (!activeId) return
    setBusy(true)
    setError('')
    setStatus('正在从各源采集…')
    try {
      const { result } = await api.collectTopic(activeId)
      await refreshTopics(activeId)
      await refreshArticles(activeId)
      setStatus(summarizeCollect(result))
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
      setStatus('')
    } finally {
      setBusy(false)
    }
  }

  async function handleCreateTopic(event: FormEvent) {
    event.preventDefault()
    const keywords = topicKeywords
      .split(/[,，\n]/)
      .map((item) => item.trim())
      .filter(Boolean)
    if (!topicName.trim() || keywords.length === 0) {
      setError('请填写话题名称和至少一个关键词')
      return
    }
    setBusy(true)
    setError('')
    try {
      const { topic } = await api.createTopic({
        name: topicName.trim(),
        keywords,
        description: `跟踪「${topicName.trim()}」相关公开资讯`,
      })
      setTopicName('')
      setTopicKeywords('')
      await refreshTopics(topic.id)
      setArticles([])
      setStatus(`已创建话题「${topic.name}」，请添加 RSS 源后采集`)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleAddSource(event: FormEvent) {
    event.preventDefault()
    if (!activeId) return
    setBusy(true)
    setError('')
    try {
      await api.addSource(activeId, {
        name: sourceName.trim(),
        url: sourceUrl.trim(),
      })
      setSourceName('')
      setSourceUrl('')
      await refreshTopics(activeId)
      setStatus('已添加信息源')
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  async function handleToggleSource(sourceId: string, enabled: boolean) {
    if (!activeId) return
    await api.toggleSource(activeId, sourceId, enabled)
    await refreshTopics(activeId)
  }

  async function handleRemoveSource(sourceId: string) {
    if (!activeId) return
    await api.removeSource(activeId, sourceId)
    await refreshTopics(activeId)
  }

  return (
    <div className="app-shell">
      <header className="hero">
        <div className="hero-copy">
          <p className="brand">话题雷达</p>
          <h2>把散落在网站上的同一话题，收成一条情报流</h2>
          <p>选定话题、挂上公开 RSS，一键汇集标题、摘要与来源。已预置「温哥华房价」示例。</p>
          <div className="hero-actions">
            <button className="btn btn-primary" onClick={handleCollect} disabled={busy || !activeId}>
              {busy ? '采集中…' : '立即采集'}
            </button>
            <a className="btn btn-secondary" href="#workspace">
              查看结果
            </a>
          </div>
        </div>
      </header>

      <section className="panel">
        <div className="section-head">
          <div>
            <h3>我的话题</h3>
            <p>先选一个话题，再管理源与阅读结果。</p>
          </div>
        </div>
        <div className="topic-rail">
          {topics.map((topic) => (
            <button
              key={topic.id}
              className={`topic-chip ${topic.id === activeId ? 'active' : ''}`}
              onClick={() => setActiveId(topic.id)}
            >
              <strong>{topic.name}</strong>
              <span>
                {topic.articleCount ?? 0} 条 · {topic.sources.length} 个源
              </span>
            </button>
          ))}
        </div>
      </section>

      <section className="workspace" id="workspace">
        <div className="feed">
          <div className="section-head" style={{ marginTop: 0 }}>
            <div>
              <h3>{activeTopic?.name || '选择话题'}</h3>
              <p>{activeTopic?.description || '从上方选择或新建一个话题。'}</p>
            </div>
            <button className="btn btn-solid" onClick={handleCollect} disabled={busy || !activeId}>
              同步最新
            </button>
          </div>

          <div className="toolbar">
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="在已采集结果里搜索标题 / 摘要 / 来源"
            />
          </div>

          {status ? <p className="status">{status}</p> : null}
          {error ? <p className="status error">{error}</p> : null}

          {articles.length === 0 ? (
            <p className="empty">还没有内容。点击「立即采集」从已配置的 RSS 源拉取。</p>
          ) : (
            articles.map((article, index) => (
              <article className="article" key={article.id} style={{ animationDelay: `${Math.min(index, 12) * 40}ms` }}>
                <a href={article.url} target="_blank" rel="noreferrer">
                  <h4>{article.title}</h4>
                </a>
                <div className="meta">
                  <span>{article.sourceName}</span>
                  <span>{formatTime(article.publishedAt || article.collectedAt)}</span>
                </div>
                {article.summary ? <p className="summary">{article.summary}</p> : null}
                {article.matchedKeywords.length > 0 ? (
                  <div className="tags">
                    {article.matchedKeywords.slice(0, 6).map((keyword) => (
                      <span className="tag" key={keyword}>
                        {keyword}
                      </span>
                    ))}
                  </div>
                ) : null}
              </article>
            ))
          )}
        </div>

        <aside className="side">
          <h4>关键词</h4>
          <ul className="keyword-list">
            {(activeTopic?.keywords || []).map((keyword) => (
              <li key={keyword}>{keyword}</li>
            ))}
          </ul>

          <h4 style={{ marginTop: '1.4rem' }}>信息源</h4>
          <ul className="source-list">
            {(activeTopic?.sources || []).map((source) => (
              <li className="source-item" key={source.id}>
                <div>
                  <strong>{source.name}</strong>
                  <small>{source.url}</small>
                  {source.lastError ? <small style={{ color: 'var(--danger)' }}>{source.lastError}</small> : null}
                </div>
                <div style={{ display: 'grid', gap: '0.35rem' }}>
                  <button
                    className="btn btn-ghost"
                    onClick={() => handleToggleSource(source.id, !source.enabled)}
                  >
                    {source.enabled ? '停用' : '启用'}
                  </button>
                  <button className="btn btn-ghost" onClick={() => handleRemoveSource(source.id)}>
                    删除
                  </button>
                </div>
              </li>
            ))}
          </ul>

          <form className="form" onSubmit={handleAddSource}>
            <label>
              源名称
              <input value={sourceName} onChange={(e) => setSourceName(e.target.value)} placeholder="例如 CBC BC" required />
            </label>
            <label>
              RSS 地址
              <input
                value={sourceUrl}
                onChange={(e) => setSourceUrl(e.target.value)}
                placeholder="https://example.com/feed.xml"
                required
              />
            </label>
            <button className="btn btn-solid" type="submit" disabled={busy || !activeId}>
              添加源
            </button>
          </form>

          <form className="form" onSubmit={handleCreateTopic}>
            <h4>新建话题</h4>
            <label>
              话题名
              <input value={topicName} onChange={(e) => setTopicName(e.target.value)} placeholder="例如 多伦多租房" />
            </label>
            <label>
              关键词（逗号分隔）
              <textarea
                value={topicKeywords}
                onChange={(e) => setTopicKeywords(e.target.value)}
                placeholder="多伦多, 租房, rent, Toronto"
                rows={3}
              />
            </label>
            <button className="btn btn-ghost" type="submit" disabled={busy}>
              创建话题
            </button>
          </form>
        </aside>
      </section>
    </div>
  )
}

function summarizeCollect(result: CollectResult): string {
  const base = `采集完成：拉取 ${result.fetched} 条，新增 ${result.added}，更新 ${result.updated}`
  if (result.sourceErrors.length === 0) return base
  return `${base}；${result.sourceErrors.length} 个源失败`
}
