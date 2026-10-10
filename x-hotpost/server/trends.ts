import Parser from 'rss-parser'
import { createHash } from 'node:crypto'
import { createId, getSettings, upsertTopic } from './store.js'
import type { HotTopic, NicheId } from './types.js'

const parser = new Parser({
  timeout: 12_000,
  headers: {
    'User-Agent': 'XHotPost/0.1 (+local; topic radar)',
  },
})

const nicheQueries: Record<NicheId, { zh: string; en: string }> = {
  tech: { zh: '人工智能 OR 科技 OR 创业', en: 'AI OR startup OR technology' },
  finance: {
    zh: '股市 OR 理财 OR 加密货币',
    en: 'markets OR investing OR crypto',
  },
  lifestyle: {
    zh: '健康 OR 旅行 OR 生活',
    en: 'wellness OR travel OR lifestyle',
  },
  creator: {
    zh: '自媒体 OR 内容创作 OR 副业',
    en: 'creator economy OR content creator',
  },
  local: { zh: '热点新闻', en: 'breaking news' },
  custom: { zh: '热点', en: 'trending' },
}

function topicIdFrom(title: string, source: string): string {
  return createHash('sha1').update(`${source}::${title}`).digest('hex').slice(0, 16)
}

function googleNewsRss(query: string, lang: 'zh-CN' | 'en-US'): string {
  const q = encodeURIComponent(query)
  if (lang === 'zh-CN') {
    return `https://news.google.com/rss/search?q=${q}&hl=zh-CN&gl=CN&ceid=CN:zh-Hans`
  }
  return `https://news.google.com/rss/search?q=${q}&hl=en-US&gl=US&ceid=US:en`
}

const demoTopics: Array<Omit<HotTopic, 'id' | 'fetchedAt'>> = [
  {
    title: '开源模型掀起新一轮 AI 应用潮',
    summary: '开发者用开源模型快速搭产品，成本下降，竞争转向落地场景。',
    source: 'demo',
    score: 96,
    language: 'zh',
    url: 'https://example.com/demo/ai-open',
  },
  {
    title: '创作者开始把长内容拆成可复用短帖矩阵',
    summary: '同一素材多平台分发，审核后定时发布，成为常见变现打法。',
    source: 'demo',
    score: 88,
    language: 'zh',
    url: 'https://example.com/demo/creator-matrix',
  },
  {
    title: '小额付费社群回暖：信任比流量更值钱',
    summary: '高互动小群比大粉更稳定，热点解读 + 会员权益成为组合拳。',
    source: 'demo',
    score: 81,
    language: 'zh',
    url: 'https://example.com/demo/paid-community',
  },
  {
    title: 'Pay-per-use X API changes how indie tools ship',
    summary: 'Indie builders lean on review queues and sparse posting to control cost.',
    source: 'demo',
    score: 74,
    language: 'en',
    url: 'https://example.com/demo/x-api',
  },
]

async function fetchRssTopics(
  query: string,
  language: HotTopic['language'],
  langTag: 'zh-CN' | 'en-US',
): Promise<HotTopic[]> {
  const feed = await parser.parseURL(googleNewsRss(query, langTag))
  const now = new Date().toISOString()
  return (feed.items ?? []).slice(0, 12).map((item, index) => {
    const title = (item.title ?? '未命名热点').trim()
    return {
      id: topicIdFrom(title, feed.title ?? langTag),
      title,
      summary: (item.contentSnippet ?? item.summary ?? title).slice(0, 220),
      source: feed.title ?? 'Google News',
      url: item.link,
      score: Math.max(40, 100 - index * 4),
      language,
      fetchedAt: now,
    }
  })
}

export async function collectHotTopics(): Promise<HotTopic[]> {
  const settings = getSettings()
  const niche = settings.niche
  const queries =
    niche === 'custom' && settings.customNicheLabel.trim()
      ? {
          zh: settings.customNicheLabel,
          en: settings.customNicheLabel,
        }
      : nicheQueries[niche]

  const collected: HotTopic[] = []

  if (settings.demoMode) {
    const now = new Date().toISOString()
    for (const item of demoTopics) {
      const topic: HotTopic = {
        ...item,
        id: topicIdFrom(item.title, item.source),
        fetchedAt: now,
      }
      upsertTopic(topic)
      collected.push(topic)
    }
  }

  try {
    if (settings.language !== 'en') {
      const zh = await fetchRssTopics(queries.zh, 'zh', 'zh-CN')
      for (const topic of zh) {
        upsertTopic(topic)
        collected.push(topic)
      }
    }
    if (settings.language !== 'zh') {
      const en = await fetchRssTopics(queries.en, 'en', 'en-US')
      for (const topic of en) {
        upsertTopic(topic)
        collected.push(topic)
      }
    }
  } catch (error) {
    if (collected.length === 0) {
      const now = new Date().toISOString()
      for (const item of demoTopics) {
        const topic: HotTopic = {
          ...item,
          id: createId(),
          fetchedAt: now,
        }
        upsertTopic(topic)
        collected.push(topic)
      }
      throw error
    }
  }

  // de-dupe by id keeping highest score
  const map = new Map<string, HotTopic>()
  for (const topic of collected) {
    const prev = map.get(topic.id)
    if (!prev || topic.score > prev.score) map.set(topic.id, topic)
  }
  return [...map.values()].sort((a, b) => b.score - a.score)
}
