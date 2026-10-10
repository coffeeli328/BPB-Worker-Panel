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

export type XCredentials = {
  /** OAuth 1.0a app key */
  apiKey: string
  apiSecret: string
  /** User access token pair */
  accessToken: string
  accessTokenSecret: string
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
  demoMode: boolean
  x: XCredentials
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
