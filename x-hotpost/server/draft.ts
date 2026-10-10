import {
  appendLog,
  createId,
  getSettings,
  listDrafts,
  listTopics,
  upsertDraft,
  updateStore,
} from './store.js'
import type { Draft, HotTopic, Settings } from './types.js'

const nicheLabel: Record<Settings['niche'], string> = {
  tech: '科技 / AI',
  finance: '财经投资',
  lifestyle: '生活方式',
  creator: '自媒体变现',
  local: '综合热点',
  custom: '自定义赛道',
}

function stripExcess(text: string): string {
  return text
    .replace(/[^\S\n]+/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
    .slice(0, 280)
}

function buildHook(settings: Settings): string {
  if (settings.affiliateUrl.trim()) {
    return settings.ctaTemplate.replaceAll('{link}', settings.affiliateUrl.trim())
  }
  return '关注我，下一条继续拆这个热点怎么变现。'
}

function templateDraft(topic: HotTopic, settings: Settings): string {
  const niche =
    settings.niche === 'custom' && settings.customNicheLabel
      ? settings.customNicheLabel
      : nicheLabel[settings.niche]
  const hook = buildHook(settings)

  if (topic.language === 'en' && settings.language !== 'zh') {
    return stripExcess(
      `${topic.title}\n\nWhy it matters for ${niche}: ${topic.summary}\n\nTakeaway: spot the signal early, then ship a clear POV.\n\n${hook}`,
    )
  }

  return stripExcess(
    `【热点】${topic.title}\n\n一句话：${topic.summary}\n\n对「${niche}」创作者意味着：别只转发标题，给出你的判断与下一步动作。\n\n${hook}`,
  )
}

async function aiDraft(
  topic: HotTopic,
  settings: Settings,
): Promise<string | null> {
  const cfg = settings.openaiCompatible
  if (!cfg?.enabled || !cfg.apiKey) return null

  const hook = buildHook(settings)
  const system = `${settings.persona}\n输出一条适合发在 X 的短帖，不超过 260 字，不要使用 Markdown，不要加引号包裹全文。结尾可自然带上变现钩子。`
  const user = `热点标题：${topic.title}\n摘要：${topic.summary}\n赛道：${nicheLabel[settings.niche]}\n变现钩子参考：${hook}`

  const base = cfg.baseUrl.replace(/\/$/, '')
  const response = await fetch(`${base}/chat/completions`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${cfg.apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: cfg.model,
      temperature: 0.7,
      messages: [
        { role: 'system', content: system },
        { role: 'user', content: user },
      ],
    }),
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(`AI draft failed: ${response.status} ${body.slice(0, 180)}`)
  }

  const json = (await response.json()) as {
    choices?: Array<{ message?: { content?: string } }>
  }
  const content = json.choices?.[0]?.message?.content?.trim()
  return content ? stripExcess(content) : null
}

export async function generateDraftForTopic(
  topic: HotTopic,
): Promise<Draft | null> {
  const settings = getSettings()
  const existing = listDrafts().find(
    (d) =>
      d.topicId === topic.id &&
      (d.status === 'pending_review' ||
        d.status === 'approved' ||
        d.status === 'published'),
  )
  if (existing) return null

  let text: string
  try {
    text = (await aiDraft(topic, settings)) ?? templateDraft(topic, settings)
  } catch (error) {
    appendLog(
      'warn',
      `AI 草稿失败，改用模板：${error instanceof Error ? error.message : String(error)}`,
    )
    text = templateDraft(topic, settings)
  }

  const now = new Date().toISOString()
  const draft: Draft = {
    id: createId(),
    topicId: topic.id,
    topicTitle: topic.title,
    text,
    status: 'pending_review',
    monetizationHook: buildHook(settings),
    createdAt: now,
    updatedAt: now,
    demo: settings.demoMode,
  }

  upsertDraft(draft)
  updateStore((s) => {
    s.stats.draftsCreated += 1
  })
  return draft
}

export async function generateDraftsFromTopics(
  limit = getSettings().maxDraftsPerTick,
): Promise<Draft[]> {
  const topics = listTopics().slice(0, 20)
  const created: Draft[] = []
  for (const topic of topics) {
    if (created.length >= limit) break
    const draft = await generateDraftForTopic(topic)
    if (draft) created.push(draft)
  }
  return created
}
