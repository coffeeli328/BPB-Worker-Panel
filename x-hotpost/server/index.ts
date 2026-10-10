import cors from 'cors'
import express from 'express'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { z } from 'zod'
import { generateDraftsFromTopics } from './draft.js'
import { publishDraft } from './publisher.js'
import {
  appendLog,
  getDraft,
  getSettings,
  listDrafts,
  listTopics,
  publicSettings,
  readStore,
  saveSettings,
  updateStore,
  upsertDraft,
} from './store.js'
import { collectHotTopics } from './trends.js'
import type { Draft, Settings } from './types.js'
import { runTick, startWorker, workerStatus } from './worker.js'

const app = express()
const PORT = Number(process.env.PORT || 8788)
const __dirname = dirname(fileURLToPath(import.meta.url))
const distDir = join(__dirname, '..', 'dist')

app.use(cors())
app.use(express.json({ limit: '1mb' }))

const settingsSchema = z.object({
  displayName: z.string().trim().min(1).max(40).optional(),
  niche: z
    .enum(['tech', 'finance', 'lifestyle', 'creator', 'local', 'custom'])
    .optional(),
  customNicheLabel: z.string().trim().max(80).optional(),
  language: z.enum(['zh', 'en', 'mixed']).optional(),
  persona: z.string().trim().max(800).optional(),
  affiliateUrl: z.string().trim().max(500).optional(),
  affiliateLabel: z.string().trim().max(80).optional(),
  ctaTemplate: z.string().trim().max(280).optional(),
  autoDraft: z.boolean().optional(),
  autoPublishApproved: z.boolean().optional(),
  pollIntervalMinutes: z.number().int().min(5).max(240).optional(),
  maxDraftsPerTick: z.number().int().min(1).max(10).optional(),
  demoMode: z.boolean().optional(),
  x: z
    .object({
      apiKey: z.string().optional(),
      apiSecret: z.string().optional(),
      accessToken: z.string().optional(),
      accessTokenSecret: z.string().optional(),
    })
    .optional(),
  openaiCompatible: z
    .object({
      enabled: z.boolean().optional(),
      baseUrl: z.string().optional(),
      apiKey: z.string().optional(),
      model: z.string().optional(),
    })
    .optional(),
})

app.get('/api/health', (_req, res) => {
  res.json({ ok: true, service: 'x-hotpost' })
})

app.get('/api/overview', (_req, res) => {
  const store = readStore()
  res.json({
    settings: publicSettings(store.settings),
    worker: workerStatus(),
    stats: store.stats,
    topics: listTopics().slice(0, 12),
    drafts: listDrafts().slice(0, 40),
    logs: store.logs.slice(0, 30),
  })
})

app.get('/api/settings', (_req, res) => {
  res.json({ settings: publicSettings() })
})

app.patch('/api/settings', (req, res) => {
  const parsed = settingsSchema.safeParse(req.body)
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() })
    return
  }

  const current = getSettings()
  const body = parsed.data
  const next: Partial<Settings> = {
    displayName: body.displayName,
    niche: body.niche,
    customNicheLabel: body.customNicheLabel,
    language: body.language,
    persona: body.persona,
    affiliateUrl: body.affiliateUrl,
    affiliateLabel: body.affiliateLabel,
    ctaTemplate: body.ctaTemplate,
    autoDraft: body.autoDraft,
    autoPublishApproved: body.autoPublishApproved,
    pollIntervalMinutes: body.pollIntervalMinutes,
    maxDraftsPerTick: body.maxDraftsPerTick,
    demoMode: body.demoMode,
  }

  if (body.x) {
    next.x = {
      apiKey: body.x.apiKey?.includes('…')
        ? current.x.apiKey
        : (body.x.apiKey ?? current.x.apiKey),
      apiSecret:
        body.x.apiSecret === '••••••••' || !body.x.apiSecret
          ? current.x.apiSecret
          : body.x.apiSecret,
      accessToken: body.x.accessToken?.includes('…')
        ? current.x.accessToken
        : (body.x.accessToken ?? current.x.accessToken),
      accessTokenSecret:
        body.x.accessTokenSecret === '••••••••' || !body.x.accessTokenSecret
          ? current.x.accessTokenSecret
          : body.x.accessTokenSecret,
    }
  }

  if (body.openaiCompatible) {
    next.openaiCompatible = {
      enabled: body.openaiCompatible.enabled ?? current.openaiCompatible!.enabled,
      baseUrl: body.openaiCompatible.baseUrl ?? current.openaiCompatible!.baseUrl,
      model: body.openaiCompatible.model ?? current.openaiCompatible!.model,
      apiKey:
        body.openaiCompatible.apiKey === '••••••••' ||
        !body.openaiCompatible.apiKey
          ? current.openaiCompatible!.apiKey
          : body.openaiCompatible.apiKey,
    }
  }

  const settings = saveSettings(next)
  appendLog('info', '设置已更新')
  res.json({ settings: publicSettings(settings) })
})

app.get('/api/topics', (_req, res) => {
  res.json({ topics: listTopics() })
})

app.post('/api/topics/refresh', async (_req, res) => {
  try {
    const topics = await collectHotTopics()
    res.json({ topics })
  } catch (error) {
    res.status(502).json({
      error: error instanceof Error ? error.message : String(error),
      topics: listTopics(),
    })
  }
})

app.get('/api/drafts', (req, res) => {
  const status = req.query.status as Draft['status'] | undefined
  res.json({ drafts: listDrafts(status) })
})

app.post('/api/drafts/generate', async (_req, res) => {
  const drafts = await generateDraftsFromTopics()
  res.json({ drafts })
})

app.patch('/api/drafts/:id', (req, res) => {
  const draft = getDraft(req.params.id)
  if (!draft) {
    res.status(404).json({ error: 'Draft not found' })
    return
  }

  const schema = z.object({
    text: z.string().trim().min(1).max(280).optional(),
    status: z
      .enum(['pending_review', 'approved', 'rejected', 'published', 'failed'])
      .optional(),
  })
  const parsed = schema.safeParse(req.body)
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() })
    return
  }

  const now = new Date().toISOString()
  const next: Draft = {
    ...draft,
    text: parsed.data.text ?? draft.text,
    status: parsed.data.status ?? draft.status,
    updatedAt: now,
    reviewedAt:
      parsed.data.status && parsed.data.status !== draft.status
        ? now
        : draft.reviewedAt,
  }

  if (parsed.data.status === 'rejected' && draft.status !== 'rejected') {
    updateStore((s) => {
      s.stats.rejected += 1
    })
  }

  upsertDraft(next)
  res.json({ draft: next })
})

app.post('/api/drafts/:id/approve', async (req, res) => {
  const draft = getDraft(req.params.id)
  if (!draft) {
    res.status(404).json({ error: 'Draft not found' })
    return
  }
  const text =
    typeof req.body?.text === 'string' && req.body.text.trim()
      ? req.body.text.trim().slice(0, 280)
      : draft.text
  const now = new Date().toISOString()
  const next: Draft = {
    ...draft,
    text,
    status: 'approved',
    reviewedAt: now,
    updatedAt: now,
  }
  upsertDraft(next)

  if (getSettings().autoPublishApproved) {
    const result = await publishDraft(next.id)
    res.json(result)
    return
  }

  res.json({ ok: true, draft: next, demo: getSettings().demoMode })
})

app.post('/api/drafts/:id/reject', (req, res) => {
  const draft = getDraft(req.params.id)
  if (!draft) {
    res.status(404).json({ error: 'Draft not found' })
    return
  }
  const now = new Date().toISOString()
  const next: Draft = {
    ...draft,
    status: 'rejected',
    reviewedAt: now,
    updatedAt: now,
  }
  upsertDraft(next)
  updateStore((s) => {
    s.stats.rejected += 1
  })
  res.json({ draft: next })
})

app.post('/api/drafts/:id/publish', async (req, res) => {
  try {
    const result = await publishDraft(req.params.id)
    res.json(result)
  } catch (error) {
    res.status(400).json({
      error: error instanceof Error ? error.message : String(error),
    })
  }
})

app.post('/api/worker/tick', async (req, res) => {
  const reason =
    typeof req.body?.reason === 'string' ? req.body.reason : 'manual'
  const result = await runTick(reason)
  res.json(result)
})

app.get('/api/worker/status', (_req, res) => {
  res.json(workerStatus())
})

if (existsSync(distDir)) {
  app.use(express.static(distDir))
  app.get(/.*/, (_req, res) => {
    res.sendFile(join(distDir, 'index.html'))
  })
}

app.listen(PORT, () => {
  console.log(`x-hotpost server on http://127.0.0.1:${PORT}`)
  startWorker()
  // bootstrap once shortly after boot so empty installs get data
  setTimeout(() => {
    void runTick('boot').catch((error) => {
      console.error('boot tick failed', error)
    })
  }, 1500)
})
