export interface Source {
  id: string
  name: string
  url: string
  type: 'rss'
  enabled: boolean
  lastFetchedAt?: string
  lastError?: string
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
  articleCount?: number
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

export interface CollectResult {
  topicId: string
  topicName: string
  fetched: number
  added: number
  updated: number
  sourceErrors: Array<{ sourceId: string; name: string; error: string }>
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {}),
    },
    ...init,
  })
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const body = await response.json()
      if (body?.error) message = typeof body.error === 'string' ? body.error : message
    } catch {
      // ignore
    }
    throw new Error(message)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  listTopics: () => request<{ topics: Topic[] }>('/api/topics'),
  getArticles: (topicId: string, q = '') =>
    request<{ articles: Article[] }>(`/api/topics/${topicId}/articles?q=${encodeURIComponent(q)}`),
  createTopic: (payload: { name: string; description?: string; keywords: string[] }) =>
    request<{ topic: Topic }>('/api/topics', { method: 'POST', body: JSON.stringify(payload) }),
  addSource: (topicId: string, payload: { name: string; url: string }) =>
    request<{ source: Source; topic: Topic }>(`/api/topics/${topicId}/sources`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  toggleSource: (topicId: string, sourceId: string, enabled: boolean) =>
    request<{ source: Source; topic: Topic }>(`/api/topics/${topicId}/sources/${sourceId}`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    }),
  removeSource: (topicId: string, sourceId: string) =>
    request<{ topic: Topic }>(`/api/topics/${topicId}/sources/${sourceId}`, { method: 'DELETE' }),
  collectTopic: (topicId: string) =>
    request<{ result: CollectResult }>(`/api/topics/${topicId}/collect`, { method: 'POST' }),
}
