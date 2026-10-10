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
  tech: '科技',
  finance: '财经',
  lifestyle: '生活',
  creator: '创作',
  local: '热点',
  custom: '赛道',
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
  return ''
}

function usableSignal(summary?: string): string | null {
  const s = (summary ?? '').trim()
  if (!s) return null
  if (s.includes('X 热搜') || (s.includes('第 ') && s.includes('名'))) return null
  if (s.includes('打开可看') || s.includes('适合结合')) return null
  return s.slice(0, 120)
}

function hashIndex(seed: string, mod: number): number {
  let h = 0
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) | 0
  return Math.abs(h) % mod
}

function templateDraft(topic: HotTopic, settings: Settings): string {
  const title = topic.title.trim()
  const signal = usableSignal(topic.summary)
  const niche = settings.niche
  const idx = hashIndex(`${topic.id}:${niche}:${settings.persona.slice(0, 16)}`, 8)
  const extra = signal ? `（${signal.slice(0, 42)}）` : ''

  const zhBanks: Record<string, string[]> = {
    tech: [
      `大家都在聊 ${title}。\n我更关心：谁已经把它接到真实产品/工作流里了？概念帖太多，落地帖太少。`,
      `${title} 很火。\n做科技内容别只会喊「颠覆」——写清：换了什么输入，得到什么输出。`,
      `关于 ${title}：技术叙事很会讲故事。\n你要问的是成本、可靠性、谁买单。`,
      `${title}\n一句话标准：能不能复现？不能复现，就先当营销。`,
    ],
    finance: [
      `${title} 上热搜了。\n这种时候最容易被情绪带着走——仓位比观点诚实。`,
      `看到 ${title}，先分清：这是信息，还是叙事？\n叙事能涨粉，信息才能决策。`,
      `${title}\n提醒自己：热搜≠催化剂。没有传导路径，就别急着下结论。`,
      `关于 ${title}：把「可能」和「已经」写清楚，少一半焦虑。`,
    ],
    creator: [
      `${title} 可以追，但别当复读机。\n你的增量是什么：角度、案例，还是方法？`,
      `热搜 ${title} 来了。\n创作者真正该问：我的读者为什么要听我讲这个？`,
      `${title}\n同一热点：一条观点、一条拆解、一条复盘。矩阵比单发强。`,
      `别只会蹭 ${title}。\n把热闹翻译成你自己的选题系统，才算变现能力。`,
    ],
    lifestyle: [
      `${title} 刷屏时，我更想问：它会不会真的改变你明天的选择？\n会，就认真看；不会，就滑走。`,
      `关于 ${title}：生活类热点最怕鸡汤。\n给一个小到能做的动作，比口号有用。`,
      `${title}\n少一点「你应该」，多一点「我试过」。`,
      `看见 ${title}，先对自己诚实：是好奇，还是焦虑？`,
    ],
    local: [
      `${title} 又上热搜了。\n说真的，先别站队，先看一手信息。`,
      `短评 ${title}：热闹不重要，判断要具体。`,
      `${title}${extra}\n我就一句话——别当复读机。`,
      `刷到 ${title}……\n要不要跟？取决于你有没有自己的看法。`,
    ],
    custom: [
      `${title} 在涨。\n我只记可核对的点，其他当背景噪声。`,
      `热搜：${title}\n发言前先过两关：来源？增量？`,
      `${title}——热闹过后，留下判断的人更少。我想做后者。`,
      `看到 ${title}。\n先观察，再开口。`,
    ],
  }

  const enBanks: string[] = [
    `${title} is trending.\nHot takes are cheap. A clear, falsifiable claim isn’t.`,
    `Everyone’s on ${title}.\nIf I can’t explain why it matters in one line, I don’t post.`,
    `${title}\nDon’t amplify the headline. Add a judgment.`,
    `Quick on ${title}: don’t be a RT machine.`,
  ]

  const useEn = topic.language === 'en' && settings.language !== 'zh'
  let body: string
  if (useEn) {
    body = enBanks[idx % enBanks.length]
  } else {
    const bank = zhBanks[niche] ?? zhBanks.local
    body = bank[idx % bank.length]
  }

  const hook = buildHook(settings)
  if (hook && body.length <= 200) body = `${body}\n\n${hook}`
  return stripExcess(body)
}

async function aiDraft(
  topic: HotTopic,
  settings: Settings,
): Promise<string | null> {
  const cfg = settings.openaiCompatible
  if (!cfg?.enabled || !cfg.apiKey) return null

  const hook = buildHook(settings)
  const niche =
    settings.niche === 'custom' && settings.customNicheLabel
      ? settings.customNicheLabel
      : nicheLabel[settings.niche]
  const signal = usableSignal(topic.summary)
  const system = `你在写 X 短帖，不是作文。
人设（勿写入正文）：${settings.persona}
赛道：${niche}
规则：只输出正文；80～180 字为佳，最多 280；要有具体判断；禁止【热点】/值得关注/复读标题；少 emoji。
${hook ? `文末可自然带：${hook}` : '不要硬广。'}
好例子：「大家都在聊 #AI。我更关心谁已经把它接到付费流程里了。」`

  const user = `热搜/话题：${topic.title}
${signal ? `背景：${signal}\n` : ''}写一条可直接发的短帖。`

  const base = cfg.baseUrl.replace(/\/$/, '')
  const response = await fetch(`${base}/chat/completions`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${cfg.apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: cfg.model,
      temperature: 0.9,
      max_tokens: 280,
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
