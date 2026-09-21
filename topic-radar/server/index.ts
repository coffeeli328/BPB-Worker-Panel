import express from 'express'
import cors from 'cors'
import { z } from 'zod'
import { collectAllTopics, collectTopic } from './collector.js'
import {
  createId,
  deleteTopic,
  getTopic,
  listArticles,
  listTopics,
  saveTopic,
} from './store.js'
import type { Source, Topic } from './types.js'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { existsSync } from 'node:fs'

const app = express()
const PORT = Number(process.env.PORT || 8787)
const __dirname = dirname(fileURLToPath(import.meta.url))
const distDir = join(__dirname, '..', 'dist')

app.use(cors())
app.use(express.json({ limit: '1mb' }))

const topicInputSchema = z.object({
  name: z.string().trim().min(1).max(80),
  description: z.string().trim().max(500).optional().default(''),
  keywords: z.array(z.string().trim().min(1)).min(1).max(40),
})

const sourceInputSchema = z.object({
  name: z.string().trim().min(1).max(120),
  url: z.string().url(),
  type: z.literal('rss').optional().default('rss'),
  enabled: z.boolean().optional().default(true),
})

app.get('/api/health', (_req, res) => {
  res.json({ ok: true, service: 'topic-radar' })
})

app.get('/api/topics', (_req, res) => {
  const topics = listTopics().map((topic) => ({
    ...topic,
    articleCount: listArticles(topic.id).length,
  }))
  res.json({ topics })
})

app.get('/api/topics/:id', (req, res) => {
  const topic = getTopic(req.params.id)
  if (!topic) {
    res.status(404).json({ error: 'Topic not found' })
    return
  }
  res.json({
    topic: {
      ...topic,
      articleCount: listArticles(topic.id).length,
    },
  })
})

app.post('/api/topics', (req, res) => {
  const parsed = topicInputSchema.safeParse(req.body)
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() })
    return
  }

  const now = new Date().toISOString()
  const topic: Topic = {
    id: createId(),
    name: parsed.data.name,
    description: parsed.data.description,
    keywords: parsed.data.keywords,
    sources: [],
    createdAt: now,
    updatedAt: now,
  }
  saveTopic(topic)
  res.status(201).json({ topic })
})

app.patch('/api/topics/:id', (req, res) => {
  const topic = getTopic(req.params.id)
  if (!topic) {
    res.status(404).json({ error: 'Topic not found' })
    return
  }

  const parsed = topicInputSchema.partial().safeParse(req.body)
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() })
    return
  }

  const next: Topic = {
    ...topic,
    ...parsed.data,
    keywords: parsed.data.keywords ?? topic.keywords,
    updatedAt: new Date().toISOString(),
  }
  saveTopic(next)
  res.json({ topic: next })
})

app.delete('/api/topics/:id', (req, res) => {
  const ok = deleteTopic(req.params.id)
  if (!ok) {
    res.status(404).json({ error: 'Topic not found' })
    return
  }
  res.status(204).end()
})

app.get('/api/topics/:id/articles', (req, res) => {
  const topic = getTopic(req.params.id)
  if (!topic) {
    res.status(404).json({ error: 'Topic not found' })
    return
  }
  const q = String(req.query.q || '').trim().toLowerCase()
  let articles = listArticles(topic.id)
  if (q) {
    articles = articles.filter(
      (article) =>
        article.title.toLowerCase().includes(q) ||
        article.summary.toLowerCase().includes(q) ||
        article.sourceName.toLowerCase().includes(q),
    )
  }
  res.json({ articles })
})

app.post('/api/topics/:id/sources', (req, res) => {
  const topic = getTopic(req.params.id)
  if (!topic) {
    res.status(404).json({ error: 'Topic not found' })
    return
  }

  const parsed = sourceInputSchema.safeParse(req.body)
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() })
    return
  }

  const source: Source = {
    id: createId(),
    name: parsed.data.name,
    url: parsed.data.url,
    type: parsed.data.type,
    enabled: parsed.data.enabled,
  }
  topic.sources.push(source)
  topic.updatedAt = new Date().toISOString()
  saveTopic(topic)
  res.status(201).json({ source, topic })
})

app.patch('/api/topics/:id/sources/:sourceId', (req, res) => {
  const topic = getTopic(req.params.id)
  if (!topic) {
    res.status(404).json({ error: 'Topic not found' })
    return
  }

  const source = topic.sources.find((item) => item.id === req.params.sourceId)
  if (!source) {
    res.status(404).json({ error: 'Source not found' })
    return
  }

  const enabled = req.body?.enabled
  if (typeof enabled === 'boolean') {
    source.enabled = enabled
  }
  topic.updatedAt = new Date().toISOString()
  saveTopic(topic)
  res.json({ source, topic })
})

app.delete('/api/topics/:id/sources/:sourceId', (req, res) => {
  const topic = getTopic(req.params.id)
  if (!topic) {
    res.status(404).json({ error: 'Topic not found' })
    return
  }

  const before = topic.sources.length
  topic.sources = topic.sources.filter((source) => source.id !== req.params.sourceId)
  if (topic.sources.length === before) {
    res.status(404).json({ error: 'Source not found' })
    return
  }
  topic.updatedAt = new Date().toISOString()
  saveTopic(topic)
  res.json({ topic })
})

app.post('/api/topics/:id/collect', async (req, res) => {
  try {
    const result = await collectTopic(req.params.id)
    res.json({ result })
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    res.status(404).json({ error: message })
  }
})

app.post('/api/collect', async (_req, res) => {
  const results = await collectAllTopics()
  res.json({ results })
})

if (existsSync(distDir)) {
  app.use(express.static(distDir))
  app.get(/.*/, (_req, res) => {
    res.sendFile(join(distDir, 'index.html'))
  })
}

app.listen(PORT, () => {
  console.log(`Topic Radar API listening on http://127.0.0.1:${PORT}`)
})
