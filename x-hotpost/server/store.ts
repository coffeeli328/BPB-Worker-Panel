import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { v4 as uuidv4 } from 'uuid'
import { browserSessionLooksReady } from './browser-profile.js'
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
  browser: {
    profileDir: 'data/browser-profile',
    headless: true,
    slowMoMs: 50,
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

function normalizeSettings(raw: Partial<Settings> & { x?: unknown }): Settings {
  const base = defaultSettings()
  const { x: _legacyX, ...rest } = raw
  return {
    ...base,
    ...rest,
    browser: {
      ...base.browser,
      ...(raw.browser ?? {}),
    },
    openaiCompatible: {
      ...base.openaiCompatible!,
      ...(raw.openaiCompatible ?? {}),
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
    settings: normalizeSettings(parsed.settings ?? {}),
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
  writeStoreFile(next)
}

function writeStoreFile(next: StoreShape): void {
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
    s.settings = normalizeSettings({
      ...s.settings,
      ...partial,
      browser: {
        ...s.settings.browser,
        ...(partial.browser ?? {}),
      },
      openaiCompatible: {
        ...s.settings.openaiCompatible!,
        ...(partial.openaiCompatible ?? {}),
      },
    })
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
    browser: {
      ...settings.browser,
      sessionReady: browserSessionLooksReady(settings.browser.profileDir),
    },
    openaiCompatible: settings.openaiCompatible
      ? {
          ...settings.openaiCompatible,
          apiKey: settings.openaiCompatible.apiKey ? '••••••••' : '',
        }
      : undefined,
  }
}
