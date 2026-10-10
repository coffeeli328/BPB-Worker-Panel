import { existsSync, mkdirSync, readdirSync } from 'node:fs'
import { dirname, isAbsolute, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const rootDir = join(__dirname, '..')

export function resolveProfileDir(profileDir: string): string {
  const absolute = isAbsolute(profileDir)
    ? profileDir
    : join(rootDir, profileDir)
  if (!existsSync(absolute)) {
    mkdirSync(absolute, { recursive: true })
  }
  return absolute
}

export function browserSessionLooksReady(profileDir: string): boolean {
  const dir = resolveProfileDir(profileDir)
  try {
    const entries = readdirSync(dir)
    return entries.some((name) =>
      ['Default', 'Cookies', 'Network', 'Local State', 'Preferences'].includes(
        name,
      ),
    )
  } catch {
    return false
  }
}
