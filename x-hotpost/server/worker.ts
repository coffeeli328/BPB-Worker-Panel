import { generateDraftsFromTopics } from './draft.js'
import { publishApprovedQueue } from './publisher.js'
import {
  appendLog,
  getSettings,
  readStore,
  updateStore,
} from './store.js'
import { collectHotTopics } from './trends.js'

export type TickResult = {
  topics: number
  drafts: number
  published: number
  errors: string[]
  at: string
}

let timer: NodeJS.Timeout | null = null
let running = false

export async function runTick(reason = 'manual'): Promise<TickResult> {
  if (running) {
    return {
      topics: 0,
      drafts: 0,
      published: 0,
      errors: ['已有任务在执行'],
      at: new Date().toISOString(),
    }
  }

  running = true
  const errors: string[] = []
  let topicsCount = 0
  let draftsCount = 0
  let publishedCount = 0

  try {
    appendLog('info', `后台任务开始（${reason}）`)
    const settings = getSettings()

    try {
      const topics = await collectHotTopics()
      topicsCount = topics.length
      appendLog('info', `抓取热点 ${topicsCount} 条`)
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      errors.push(message)
      appendLog('warn', `热点抓取部分失败：${message}`)
    }

    if (settings.autoDraft) {
      const drafts = await generateDraftsFromTopics(settings.maxDraftsPerTick)
      draftsCount = drafts.length
      if (draftsCount > 0) {
        appendLog('info', `生成待审核草稿 ${draftsCount} 条`)
      }
    }

    if (settings.autoPublishApproved) {
      const published = await publishApprovedQueue()
      publishedCount = published.filter((p) => p.ok).length
      if (publishedCount > 0) {
        appendLog('info', `自动发布已通过草稿 ${publishedCount} 条`)
      }
    }

    updateStore((s) => {
      s.lastTickAt = new Date().toISOString()
      s.stats.ticks += 1
    })

    return {
      topics: topicsCount,
      drafts: draftsCount,
      published: publishedCount,
      errors,
      at: readStore().lastTickAt!,
    }
  } finally {
    running = false
  }
}

export function startWorker(): void {
  const schedule = () => {
    const minutes = Math.max(5, getSettings().pollIntervalMinutes || 30)
    if (timer) clearInterval(timer)
    timer = setInterval(
      () => {
        void runTick('schedule').catch((error) => {
          appendLog(
            'error',
            `定时任务异常：${error instanceof Error ? error.message : String(error)}`,
          )
        })
      },
      minutes * 60_000,
    )
  }

  schedule()
  // refresh schedule periodically in case settings change
  setInterval(schedule, 60_000)
  appendLog('info', '后台调度已启动（抓热点 → 写草稿 → 等你审核 → 自动发布）')
}

export function workerStatus() {
  const store = readStore()
  return {
    running,
    lastTickAt: store.lastTickAt ?? null,
    pollIntervalMinutes: store.settings.pollIntervalMinutes,
    pendingReview: store.drafts.filter((d) => d.status === 'pending_review')
      .length,
    approved: store.drafts.filter((d) => d.status === 'approved').length,
    published: store.stats.published,
    demoMode: store.settings.demoMode,
  }
}
