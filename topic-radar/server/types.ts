export type SourceType = 'rss'

export interface Source {
  id: string
  name: string
  url: string
  type: SourceType
  enabled: boolean
  lastFetchedAt?: string
  lastError?: string
}

export interface Article {
  id: string
  topicId: string
  sourceId: string
  sourceName: string
  title: string
  url: string
  summary: string
  publishedAt: string | null
  collectedAt: string
  matchedKeywords: string[]
}

export interface Topic {
  id: string
  name: string
  description: string
  keywords: string[]
  sources: Source[]
  createdAt: string
  updatedAt: string
  lastCollectedAt?: string
}

export interface StoreData {
  topics: Topic[]
  articles: Article[]
}
