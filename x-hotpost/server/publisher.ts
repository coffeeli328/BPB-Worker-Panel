import { browserSessionLooksReady } from './browser-profile.js'
import { postViaBrowser } from './browser.js'
import {
  appendLog,
  getDraft,
  getSettings,
  updateStore,
  upsertDraft,
} from './store.js'
import type { Draft } from './types.js'

export type PublishResult = {
  ok: boolean
  draft: Draft
  demo: boolean
  postId?: string
  error?: string
}

export async function publishDraft(draftId: string): Promise<PublishResult> {
  const draft = getDraft(draftId)
  if (!draft) {
    throw new Error('Draft not found')
  }
  if (draft.status !== 'approved') {
    throw new Error('Only approved drafts can be published')
  }

  const settings = getSettings()
  const now = new Date().toISOString()

  if (settings.demoMode) {
    const next: Draft = {
      ...draft,
      status: 'published',
      publishedAt: now,
      updatedAt: now,
      externalPostId: `demo_${draft.id.slice(0, 8)}`,
      demo: true,
      publishError: undefined,
    }
    upsertDraft(next)
    updateStore((s) => {
      s.stats.published += 1
    })
    appendLog('info', `演示模式已“发布”草稿：${draft.topicTitle}`)
    return { ok: true, draft: next, demo: true, postId: next.externalPostId }
  }

  if (!browserSessionLooksReady(settings.browser.profileDir)) {
    const message =
      '尚未保存 X 浏览器登录会话。请先运行 npm run x:login，或临时打开演示模式。'
    const next: Draft = {
      ...draft,
      status: 'failed',
      updatedAt: now,
      publishError: message,
    }
    upsertDraft(next)
    appendLog('error', message)
    return { ok: false, draft: next, demo: false, error: message }
  }

  try {
    const { id } = await postViaBrowser(draft.text)
    const next: Draft = {
      ...draft,
      status: 'published',
      publishedAt: now,
      updatedAt: now,
      externalPostId: id,
      demo: false,
      publishError: undefined,
    }
    upsertDraft(next)
    updateStore((s) => {
      s.stats.published += 1
    })
    appendLog('info', `已通过浏览器发布到 X：${draft.topicTitle} (${id})`)
    return { ok: true, draft: next, demo: false, postId: id }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    const next: Draft = {
      ...draft,
      status: 'failed',
      updatedAt: now,
      publishError: message,
    }
    upsertDraft(next)
    appendLog('error', `浏览器发布失败：${message}`)
    return { ok: false, draft: next, demo: false, error: message }
  }
}

export async function publishApprovedQueue(): Promise<PublishResult[]> {
  const { listDrafts } = await import('./store.js')
  const approved = listDrafts('approved')
  const results: PublishResult[] = []
  for (const draft of approved) {
    results.push(await publishDraft(draft.id))
  }
  return results
}
