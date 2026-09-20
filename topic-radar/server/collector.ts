import Parser from 'rss-parser'
import { createId, getTopic, listTopics, saveTopic, upsertArticles } from './store.js'
import type { Article, Source, Topic } from './types.js'

const parser = new Parser({
  timeout: 12000,
  headers: {
    'User-Agent':
      'Mozilla/5.0 (compatible; TopicRadar/0.1; +https://localhost; RSS reader)',
    Accept: 'application/rss+xml, application/atom+xml, application/xml, text/xml, */*',
  },
})

async function fetchWithTimeout(url: string, ms = 12000): Promise<string> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), ms)
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        'User-Agent':
          'Mozilla/5.0 (compatible; TopicRadar/0.1; +https://localhost; RSS reader)',
        Accept: 'application/rss+xml, application/atom+xml, application/xml, text/xml, */*',
      },
    })
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    return await response.text()
  } finally {
    clearTimeout(timer)
  }
}

function normalizeText(value: string): string {
  return value.toLowerCase().replace(/\s+/g, ' ').trim()
}

function stripHtml(value: string): string {
  return value.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
}

function matchKeywords(text: string, keywords: string[]): string[] {
  const haystack = normalizeText(text)
  return keywords.filter((keyword) => haystack.includes(normalizeText(keyword)))
}

function isTopicScopedFeed(url: string): boolean {
  return /google\.com\/rss\/search|reddit\.com\/r\//i.test(url)
}

function shouldKeepArticle(
  topic: Topic,
  source: Source,
  title: string,
  summary: string,
): { keep: boolean; matched: string[] } {
  const blob = `${title} ${summary}`
  const matched = matchKeywords(blob, topic.keywords)
  if (matched.length > 0) return { keep: true, matched }
  // Search/community feeds are already topic-scoped; keep all items.
  if (isTopicScopedFeed(source.url)) return { keep: true, matched }
  return { keep: false, matched }
}

async function fetchSource(topic: Topic, source: Source): Promise<{ articles: Article[]; error?: string }> {
  try {
    const xml = await fetchWithTimeout(source.url)
    const feed = await parser.parseString(xml)
    const collectedAt = new Date().toISOString()
    const articles: Article[] = []

    for (const item of feed.items || []) {
      const title = (item.title || '').trim()
      const url = (item.link || item.guid || '').trim()
      if (!title || !url) continue

      const summary = stripHtml(item.contentSnippet || item.content || item.summary || '')
      const { keep, matched } = shouldKeepArticle(topic, source, title, summary)
      if (!keep) continue

      articles.push({
        id: createId(),
        topicId: topic.id,
        sourceId: source.id,
        sourceName: source.name,
        title,
        url,
        summary: summary.slice(0, 500),
        publishedAt: item.isoDate || item.pubDate || null,
        collectedAt,
        matchedKeywords: matched,
      })
    }

    return { articles }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    return { articles: [], error: message }
  }
}

export interface CollectResult {
  topicId: string
  topicName: string
  fetched: number
  added: number
  updated: number
  sourceErrors: Array<{ sourceId: string; name: string; error: string }>
}

export async function collectTopic(topicId: string): Promise<CollectResult> {
  const topic = getTopic(topicId)
  if (!topic) {
    throw new Error('Topic not found')
  }

  const enabledSources = topic.sources.filter((source) => source.enabled)
  let fetched = 0
  const collected: Article[] = []
  const sourceErrors: CollectResult['sourceErrors'] = []
  const now = new Date().toISOString()

  for (const source of enabledSources) {
    const result = await fetchSource(topic, source)
    fetched += result.articles.length
    collected.push(...result.articles)

    source.lastFetchedAt = now
    source.lastError = result.error
    if (result.error) {
      sourceErrors.push({ sourceId: source.id, name: source.name, error: result.error })
    }
  }

  const { added, updated } = upsertArticles(collected)
  topic.lastCollectedAt = now
  topic.updatedAt = now
  saveTopic(topic)

  return {
    topicId: topic.id,
    topicName: topic.name,
    fetched,
    added,
    updated,
    sourceErrors,
  }
}

export async function collectAllTopics(): Promise<CollectResult[]> {
  const results: CollectResult[] = []
  for (const topic of listTopics()) {
    results.push(await collectTopic(topic.id))
  }
  return results
}
