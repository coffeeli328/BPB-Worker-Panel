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

/** Publish via local Chromium session — no X API */
export type BrowserPublishSettings = {
  /** Absolute or project-relative profile directory */
  profileDir: string
  headless: boolean
  slowMoMs: number
}

export type Settings = {
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
  /** When true, never open a browser — only mark published locally */
  demoMode: boolean
  browser: BrowserPublishSettings
  openaiCompatible?: {
    enabled: boolean
    baseUrl: string
    apiKey: string
    model: string
  }
}

export type WorkerLog = {
  id: string
  at: string
  level: 'info' | 'warn' | 'error'
  message: string
}

export type StoreShape = {
  settings: Settings
  topics: HotTopic[]
  drafts: Draft[]
  logs: WorkerLog[]
  lastTickAt?: string
  stats: {
    draftsCreated: number
    published: number
    rejected: number
    ticks: number
  }
}
