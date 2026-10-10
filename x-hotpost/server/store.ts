import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { v4 as uuidv4 } from 'uuid'
import type { Draft, HotTopic, Settings, StoreShape, WorkerLog } from './types.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
const dataDir = join(__dirname, '..', 'data')
const storePath = join(dataDir, 'store.json')

export const defaultSettings = (): Settings => ({
  displayName: '热帖',
  niche: 'tech',
  customNicheLabel: '',
  language: 'zh',
  persona:
    '你是一位务实的中文创作者，擅长把热点讲清楚，语气真诚、不夸张，偶尔带一点洞察。',
  affiliateUrl: '',
  affiliateLabel: '了解更多',
  ctaTemplate: '对这个话题感兴趣的话，可以看看：{link}',
  autoDraft: true,
  autoPublishApproved: true,
  pollIntervalMinutes: 30,
  maxDraftsPerTick: 3,
  demoMode: true,
  x: {
    apiKey: '',
    apiSecret: '',
    accessToken: '',
    accessTokenSecret: '',
  },
  openaiCompatible: {
    enabled: false,
    baseUrl: 'https://api.openai.com/v1',
    apiKey: '',
    model: 'gpt-4o-mini',
  },
})

function defaultStore(): StoreShape {
  return {
    settings: defaultSettings(),
    topics: [],
    drafts: [],
    logs: [],
    stats: {
      draftsCreated: 0,
      published: 0,
      rejected: 0,
      ticks: 0,
    },
  }
}

function ensureStore(): StoreShape {
  if (!existsSync(dataDir)) {
    mkdirSync(dataDir, { recursive: true })
  }
  if (!existsSync(storePath)) {
    const initial = defaultStore()
    writeFileSync(storePath, JSON.stringify(initial, null, 2), 'utf8')
    return initial
  }
  const raw = readFileSync(storePath, 'utf8')
  const parsed = JSON.parse(raw) as StoreShape
  return {
    ...defaultStore(),
    ...parsed,
    settings: { ...defaultSettings(), ...parsed.settings },
    stats: { ...defaultStore().stats, ...parsed.stats },
  }
}

let cache: StoreShape | null = null

export function createId(): string {
  return uuidv4()
}

export function readStore(): StoreShape {
  if (!cache) {
    cache = ensureStore()
  }
  return cache
}

export function writeStore(next: StoreShape): void {
  cache = next
  if (!existsSync(dataDir)) {
    mkdirSync(dataDir, { recursive: true })
  }
  writeFileSync(storePath, JSON.stringify(next, null, 2), 'utf8')
}

export function updateStore(mutator: (store: StoreShape) => void): StoreShape {
  const store = structuredClone(readStore())
  mutator(store)
  writeStore(store)
  return store
}

export function getSettings(): Settings {
  return readStore().settings
}

export function saveSettings(partial: Partial<Settings>): Settings {
  const store = updateStore((s) => {
    s.settings = {
      ...s.settings,
      ...partial,
      x: { ...s.settings.x, ...(partial.x ?? {}) },
      openaiCompatible: {
        ...s.settings.openaiCompatible!,
        ...(partial.openaiCompatible ?? {}),
      },
    }
  })
  return store.settings
}

export function listTopics(): HotTopic[] {
  return [...readStore().topics].sort((a, b) => b.score - a.score)
}

export function listDrafts(status?: Draft['status']): Draft[] {
  const drafts = readStore().drafts
  const filtered = status ? drafts.filter((d) => d.status === status) : drafts
  return [...filtered].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
}

export function getDraft(id: string): Draft | undefined {
  return readStore().drafts.find((d) => d.id === id)
}

export function upsertTopic(topic: HotTopic): void {
  updateStore((s) => {
    const idx = s.topics.findIndex((t) => t.id === topic.id)
    if (idx >= 0) s.topics[idx] = topic
    else s.topics.unshift(topic)
    s.topics = s.topics.slice(0, 80)
  })
}

export function upsertDraft(draft: Draft): void {
  updateStore((s) => {
    const idx = s.drafts.findIndex((d) => d.id === draft.id)
    if (idx >= 0) s.drafts[idx] = draft
    else s.drafts.unshift(draft)
  })
}

export function appendLog(
  level: WorkerLog['level'],
  message: string,
): WorkerLog {
  const log: WorkerLog = {
    id: createId(),
    at: new Date().toISOString(),
    level,
    message,
  }
  updateStore((s) => {
    s.logs.unshift(log)
    s.logs = s.logs.slice(0, 120)
  })
  return log
}

export function publicSettings(settings: Settings = getSettings()) {
  return {
    ...settings,
    x: {
      apiKey: settings.x.apiKey ? mask(settings.x.apiKey) : '',
      apiSecret: settings.x.apiSecret ? '••••••••' : '',
      accessToken: settings.x.accessToken ? mask(settings.x.accessToken) : '',
      accessTokenSecret: settings.x.accessTokenSecret ? '••••••••' : '',
      configured: Boolean(
        settings.x.apiKey &&
          settings.x.apiSecret &&
          settings.x.accessToken &&
          settings.x.accessTokenSecret,
      ),
    },
    openaiCompatible: settings.openaiCompatible
      ? {
          ...settings.openaiCompatible,
          apiKey: settings.openaiCompatible.apiKey
            ? '••••••••'
            : '',
        }
      : undefined,
  }
}

function mask(value: string): string {
  if (value.length <= 8) return '••••••••'
  return `${value.slice(0, 4)}…${value.slice(-4)}`
}
