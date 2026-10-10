import { chromium, type BrowserContext, type Page } from 'playwright'
import {
  browserSessionLooksReady,
  resolveProfileDir,
} from './browser-profile.js'
import { getSettings } from './store.js'

export { browserSessionLooksReady, resolveProfileDir }

async function launchContext(options?: {
  headless?: boolean
  slowMoMs?: number
}): Promise<BrowserContext> {
  const settings = getSettings().browser
  const userDataDir = resolveProfileDir(settings.profileDir)
  return chromium.launchPersistentContext(userDataDir, {
    headless: options?.headless ?? settings.headless,
    slowMo: options?.slowMoMs ?? settings.slowMoMs,
    viewport: { width: 1280, height: 900 },
    locale: 'zh-CN',
    args: ['--disable-blink-features=AutomationControlled'],
  })
}

async function ensureLoggedIn(page: Page): Promise<void> {
  await page.goto('https://x.com/home', {
    waitUntil: 'domcontentloaded',
    timeout: 60_000,
  })
  await page.waitForTimeout(1500)
  const url = page.url()
  if (
    url.includes('/login') ||
    url.includes('/i/flow/login') ||
    url.includes('/logout')
  ) {
    throw new Error(
      '浏览器会话未登录 X。请先运行：npm run x:login ，在弹出窗口完成登录后关闭。',
    )
  }
}

async function fillComposer(page: Page, text: string): Promise<void> {
  await page.goto('https://x.com/compose/post', {
    waitUntil: 'domcontentloaded',
    timeout: 60_000,
  })
  await page.waitForTimeout(1200)

  const editor = page
    .locator('[data-testid="tweetTextarea_0"]')
    .or(page.locator('[role="textbox"][contenteditable="true"]'))
    .first()

  await editor.waitFor({ state: 'visible', timeout: 30_000 })
  await editor.click()
  await page.keyboard.press(
    process.platform === 'darwin' ? 'Meta+A' : 'Control+A',
  )
  await page.keyboard.type(text, { delay: 12 })

  const postButton = page
    .locator('[data-testid="tweetButton"]')
    .or(page.locator('[data-testid="tweetButtonInline"]'))
    .first()

  await postButton.waitFor({ state: 'visible', timeout: 15_000 })
  await page.waitForFunction(
    `(() => {
      const buttons = Array.from(document.querySelectorAll(
        '[data-testid="tweetButton"], [data-testid="tweetButtonInline"]'
      ));
      return buttons.some((btn) => {
        const el = btn;
        return !el.disabled && el.getAttribute('aria-disabled') !== 'true';
      });
    })()`,
    { timeout: 20_000 },
  )
  await postButton.click()
  await page.waitForTimeout(2500)
}

export async function postViaBrowser(text: string): Promise<{ id: string }> {
  const settings = getSettings().browser
  const context = await launchContext({
    headless: settings.headless,
    slowMoMs: settings.slowMoMs,
  })
  try {
    const page = context.pages()[0] ?? (await context.newPage())
    await ensureLoggedIn(page)
    await fillComposer(page, text)
    return { id: `browser_${Date.now().toString(36)}` }
  } finally {
    await context.close()
  }
}

/** Open a headed browser so the user can log into X once. */
export async function openLoginWindow(): Promise<void> {
  const context = await launchContext({ headless: false, slowMoMs: 0 })
  const page = context.pages()[0] ?? (await context.newPage())
  await page.goto('https://x.com/login', {
    waitUntil: 'domcontentloaded',
    timeout: 60_000,
  })
  console.log(
    '\n已打开 Chromium。请在窗口内登录你的 X 账号。\n登录成功进入首页后，回到终端按 Enter 关闭并保存会话。\n',
  )

  await new Promise<void>((resolve) => {
    process.stdin.resume()
    process.stdin.once('data', () => resolve())
  })

  await context.close()
  console.log(`会话已保存到：${resolveProfileDir(getSettings().browser.profileDir)}`)
}

export function browserPublicStatus() {
  const settings = getSettings().browser
  const profileDir = resolveProfileDir(settings.profileDir)
  return {
    profileDir,
    headless: settings.headless,
    slowMoMs: settings.slowMoMs,
    sessionReady: browserSessionLooksReady(profileDir),
  }
}
