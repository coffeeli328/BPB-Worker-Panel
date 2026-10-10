export type NicheId =
  | 'tech'
  | 'finance'
  | 'lifestyle'
  | 'creator'
  | 'local'
  | 'custom'

export type DraftStatus =
  | 'pending_review'
  | 'approved'
  | 'rejected'
  | 'published'
  | 'failed'

export type HotTopic = {
  id: string
  title: string
  summary: string
  source: string
  url?: string
  score: number
  language: 'zh' | 'en' | 'mixed'
  fetchedAt: string
}

export type Draft = {
  id: string
  topicId: string
  topicTitle: string
  text: string
  status: DraftStatus
  monetizationHook: string
  createdAt: string
  updatedAt: string
  reviewedAt?: string
  publishedAt?: string
  publishError?: string
  externalPostId?: string
  demo: boolean
}

export type PublicSettings = {
  displayName: string
  niche: NicheId
  customNicheLabel: string
  language: 'zh' | 'en' | 'mixed'
  persona: string
  affiliateUrl: string
  affiliateLabel: string
  ctaTemplate: string
  autoDraft: boolean
  autoPublishApproved: boolean
  pollIntervalMinutes: number
  maxDraftsPerTick: number
  demoMode: boolean
  browser: {
    profileDir: string
    headless: boolean
    slowMoMs: number
    sessionReady: boolean
  }
  openaiCompatible?: {
    enabled: boolean
    baseUrl: string
    apiKey: string
    model: string
  }
}

export type Overview = {
  settings: PublicSettings
  browser: {
    profileDir: string
    headless: boolean
    slowMoMs: number
    sessionReady: boolean
  }
  worker: {
    running: boolean
    lastTickAt: string | null
    pollIntervalMinutes: number
    pendingReview: number
    approved: number
    published: number
    demoMode: boolean
  }
  stats: {
    draftsCreated: number
    published: number
    rejected: number
    ticks: number
  }
  topics: HotTopic[]
  drafts: Draft[]
  logs: Array<{
    id: string
    at: string
    level: 'info' | 'warn' | 'error'
    message: string
  }>
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(
      typeof data.error === 'string'
        ? data.error
        : `请求失败 (${response.status})`,
    )
  }
  return data as T
}

export function fetchOverview() {
  return request<Overview>('/api/overview')
}

export function patchSettings(body: Record<string, unknown>) {
  return request<{ settings: PublicSettings }>('/api/settings', {
    method: 'PATCH',
    body: JSON.stringify(body),
  })
}

export function runTick() {
  return request<{
    topics: number
    drafts: number
    published: number
    errors: string[]
    at: string
  }>('/api/worker/tick', {
    method: 'POST',
    body: JSON.stringify({ reason: 'ui' }),
  })
}

export function approveDraft(id: string, text?: string) {
  return request<{ ok: boolean; draft: Draft; demo?: boolean; postId?: string }>(
    `/api/drafts/${id}/approve`,
    {
      method: 'POST',
      body: JSON.stringify(text ? { text } : {}),
    },
  )
}

export function rejectDraft(id: string) {
  return request<{ draft: Draft }>(`/api/drafts/${id}/reject`, {
    method: 'POST',
    body: '{}',
  })
}

export function updateDraft(id: string, body: { text?: string; status?: DraftStatus }) {
  return request<{ draft: Draft }>(`/api/drafts/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  })
}
