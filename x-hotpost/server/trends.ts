import { createHash } from 'node:crypto'
import { createId, getSettings, upsertTopic } from './store.js'
import type { HotTopic, NicheId } from './types.js'

/**
 * Collect what's hot on X via public trends mirror (getdaytrends).
 * No official X API / no Google News.
 */

const nicheKeywords: Record<NicheId, string[]> = {
  tech: [
    'ai',
    'chatgpt',
    'gpt',
    'openai',
    'llm',
    '科技',
    '人工智能',
    '大模型',
    'apple',
    'google',
    'nvidia',
    'crypto',
    'bitcoin',
    '开源',
    'github',
  ],
  finance: [
    'stock',
    'market',
    'fed',
    '股市',
    '理财',
    'crypto',
    'bitcoin',
    '美联储',
    '投资',
    '通胀',
  ],
  lifestyle: ['health', 'travel', 'food', '旅行', '健康', '生活', 'fashion', 'wellness'],
  creator: [
    'creator',
    'influencer',
    'youtube',
    'tiktok',
    '自媒体',
    '副业',
    'content',
    '变现',
    '粉丝',
  ],
  local: [],
  custom: [],
}

function topicIdFrom(title: string, source: string): string {
  return createHash('sha1').update(`${source}::${title}`).digest('hex').slice(0, 16)
}

function xSearchUrl(query: string): string {
  return `https://x.com/search?q=${encodeURIComponent(query)}&src=typed_query&f=live`
}

const regionSlugs: Record<string, string> = {
  'united-states': '美国',
  'united-kingdom': '英国',
  japan: '日本',
  singapore: '新加坡',
  india: '印度',
}

const demoTopics: Array<Omit<HotTopic, 'id' | 'fetchedAt'>> = [
  {
    title: '#OpenSourceAI',
    summary: 'X 热搜演示：开源模型话题正在讨论区发酵。',
    source: 'X 热搜 · demo',
    score: 96,
    language: 'en',
    url: xSearchUrl('#OpenSourceAI'),
  },
  {
    title: '创作者经济',
    summary: 'X 热搜演示：创作者在讨论短帖矩阵与审核发帖。',
    source: 'X 热搜 · demo',
    score: 88,
    language: 'zh',
    url: xSearchUrl('创作者经济'),
  },
  {
    title: '#BuildInPublic',
    summary: 'X 热搜演示：公开构建成为常见打法。',
    source: 'X 热搜 · demo',
    score: 81,
    language: 'en',
    url: xSearchUrl('#BuildInPublic'),
  },
]

function resolveRegions(language: SettingsLanguage): string[] {
  if (language === 'zh') return ['japan', 'singapore', 'united-states']
  if (language === 'en') return ['united-states', 'united-kingdom']
  return ['united-states', 'japan', 'singapore']
}

type SettingsLanguage = 'zh' | 'en' | 'mixed'

function parseGetDayTrends(html: string, regionSlug: string, now: string): HotTopic[] {
  const label = regionSlugs[regionSlug] ?? regionSlug
  const re = /<td class="main"><a[^>]*>([^<]+)<\/a><\/td>/gi
  const names: string[] = []
  const seen = new Set<string>()
  let m: RegExpExecArray | null
  while ((m = re.exec(html)) !== null && names.length < 50) {
    const name = m[1].trim()
    const key = name.toLowerCase()
    if (!name || seen.has(key)) continue
    seen.add(key)
    names.push(name)
  }
  return names.map((name, index) => {
    const hasCjk = /[\u4e00-\u9fff\u3040-\u30ff]/.test(name)
    return {
      id: topicIdFrom(name, `x-${regionSlug}`),
      title: name,
      summary: '',
      source: `X 热搜 · ${label} · #${index + 1}`,
      url: xSearchUrl(name),
      score: Math.max(40, 100 - index),
      language: hasCjk ? 'zh' : 'en',
      fetchedAt: now,
    }
  })
}

async function fetchRegion(regionSlug: string, now: string): Promise<HotTopic[]> {
  const res = await fetch(`https://getdaytrends.com/${regionSlug}/`, {
    headers: {
      'User-Agent':
        'Mozilla/5.0 (compatible; XHotPost/0.2; +local; X trend radar)',
      'Accept-Language': 'en-US,en;q=0.9,zh-CN;q=0.8',
    },
    signal: AbortSignal.timeout(15_000),
  })
  if (!res.ok) throw new Error(`X trends ${res.status}`)
  const html = await res.text()
  return parseGetDayTrends(html, regionSlug, now)
}

function rankForNiche(topics: HotTopic[], niche: NicheId, customLabel: string): HotTopic[] {
  let keywords = nicheKeywords[niche] ?? []
  if (niche === 'custom' && customLabel.trim()) {
    keywords = customLabel
      .split(/[,，\s]+/)
      .map((s) => s.trim().toLowerCase())
      .filter((s) => s.length >= 2)
  }
  if (keywords.length === 0) return topics

  const matched: HotTopic[] = []
  const rest: HotTopic[] = []
  for (const topic of topics) {
    const hay = topic.title.toLowerCase()
    if (keywords.some((k) => hay.includes(k))) {
      matched.push({ ...topic, score: Math.min(120, topic.score + 25) })
    } else {
      rest.push(topic)
    }
  }
  return [...matched, ...rest.slice(0, 12)]
}

export async function collectHotTopics(): Promise<HotTopic[]> {
  const settings = getSettings()
  const now = new Date().toISOString()
  const collected: HotTopic[] = []

  if (settings.demoMode) {
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

  const regions = resolveRegions(settings.language)
  for (const region of regions) {
    try {
      const batch = await fetchRegion(region, now)
      for (const topic of batch) {
        upsertTopic(topic)
        collected.push(topic)
      }
    } catch {
      // try next region
    }
  }

  if (collected.length === 0) {
    for (const item of demoTopics) {
      const topic: HotTopic = {
        ...item,
        id: createId(),
        fetchedAt: now,
        source: 'X 热搜 · fallback',
      }
      upsertTopic(topic)
      collected.push(topic)
    }
  }

  const ranked = rankForNiche(collected, settings.niche, settings.customNicheLabel)
  const map = new Map<string, HotTopic>()
  for (const topic of ranked) {
    const key = topic.title.trim().toLowerCase().replace(/^#/, '')
    const prev = map.get(key)
    if (!prev || topic.score > prev.score) map.set(key, topic)
  }
  return [...map.values()].sort((a, b) => b.score - a.score).slice(0, 40)
}

export { parseGetDayTrends, xSearchUrl }
