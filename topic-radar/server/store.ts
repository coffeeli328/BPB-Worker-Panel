import { mkdirSync, readFileSync, renameSync, writeFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { v4 as uuid } from 'uuid'
import type { Article, StoreData, Topic } from './types.js'
import { createSeedStore } from './seed.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
export const DATA_DIR = join(__dirname, '..', 'data')
export const STORE_PATH = join(DATA_DIR, 'store.json')

function ensureStore(): StoreData {
  mkdirSync(DATA_DIR, { recursive: true })
  if (!existsSync(STORE_PATH)) {
    const seed = createSeedStore()
    writeStore(seed)
    return seed
  }
  const raw = readFileSync(STORE_PATH, 'utf8')
  return JSON.parse(raw) as StoreData
}

export function readStore(): StoreData {
  return ensureStore()
}

export function writeStore(data: StoreData): void {
  mkdirSync(DATA_DIR, { recursive: true })
  const temp = `${STORE_PATH}.${process.pid}.tmp`
  writeFileSync(temp, JSON.stringify(data, null, 2), 'utf8')
  renameSync(temp, STORE_PATH)
}

export function listTopics(): Topic[] {
  return readStore().topics.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
}

export function getTopic(id: string): Topic | undefined {
  return readStore().topics.find((topic) => topic.id === id)
}

export function saveTopic(topic: Topic): Topic {
  const store = readStore()
  const index = store.topics.findIndex((item) => item.id === topic.id)
  if (index === -1) {
    store.topics.push(topic)
  } else {
    store.topics[index] = topic
  }
  writeStore(store)
  return topic
}

export function deleteTopic(id: string): boolean {
  const store = readStore()
  const before = store.topics.length
  store.topics = store.topics.filter((topic) => topic.id !== id)
  store.articles = store.articles.filter((article) => article.topicId !== id)
  writeStore(store)
  return store.topics.length < before
}

export function listArticles(topicId?: string): Article[] {
  const articles = readStore().articles
  const filtered = topicId ? articles.filter((article) => article.topicId === topicId) : articles
  return filtered.sort((a, b) => {
    const aTime = a.publishedAt || a.collectedAt
    const bTime = b.publishedAt || b.collectedAt
    return bTime.localeCompare(aTime)
  })
}

export function upsertArticles(articles: Article[]): { added: number; updated: number } {
  const store = readStore()
  let added = 0
  let updated = 0

  for (const article of articles) {
    const existingIndex = store.articles.findIndex(
      (item) => item.topicId === article.topicId && item.url === article.url,
    )
    if (existingIndex === -1) {
      store.articles.push(article)
      added += 1
    } else {
      store.articles[existingIndex] = {
        ...store.articles[existingIndex],
        ...article,
        id: store.articles[existingIndex].id,
        collectedAt: store.articles[existingIndex].collectedAt,
      }
      updated += 1
    }
  }

  writeStore(store)
  return { added, updated }
}

export function createId(): string {
  return uuid()
}
