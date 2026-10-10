import { buildOAuth1Header } from './oauth1.js'
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

function credentialsReady(): boolean {
  const x = getSettings().x
  return Boolean(x.apiKey && x.apiSecret && x.accessToken && x.accessTokenSecret)
}

async function postToX(text: string): Promise<{ id: string }> {
  const settings = getSettings()
  const url = 'https://api.x.com/2/tweets'
  const authorization = buildOAuth1Header({
    method: 'POST',
    url,
    consumerKey: settings.x.apiKey,
    consumerSecret: settings.x.apiSecret,
    token: settings.x.accessToken,
    tokenSecret: settings.x.accessTokenSecret,
  })

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: authorization,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ text }),
  })

  const bodyText = await response.text()
  if (!response.ok) {
    throw new Error(`X API ${response.status}: ${bodyText.slice(0, 240)}`)
  }

  const json = JSON.parse(bodyText) as { data?: { id?: string } }
  const id = json.data?.id
  if (!id) throw new Error('X API returned no post id')
  return { id }
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

  if (settings.demoMode || !credentialsReady()) {
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
    appendLog(
      'info',
      settings.demoMode
        ? `演示模式已“发布”草稿：${draft.topicTitle}`
        : `未配置 X 凭证，已用演示模式发布：${draft.topicTitle}`,
    )
    return { ok: true, draft: next, demo: true, postId: next.externalPostId }
  }

  try {
    const { id } = await postToX(draft.text)
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
    appendLog('info', `已发布到 X：${draft.topicTitle} (#${id})`)
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
    appendLog('error', `发布失败：${message}`)
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
