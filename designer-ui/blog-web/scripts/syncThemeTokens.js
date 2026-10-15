import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

import { themeCssText } from '../app/config/theme.js'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')

/** 生成物路径：由 index.css @import 引入，同时提交进仓库（便于 diff 配色变更） */
export const TOKENS_FILE = resolve(ROOT, 'app/assets/styles/tokens.css')

/**
 * 把 theme.js 的设计变量写成静态 CSS。
 *
 * <p>SSR 下没法再靠 JS 往 documentElement 上塞变量（服务端无 DOM），
 * 所以配色必须在构建期就变成真正的样式表。</p>
 */
export function syncThemeTokens() {
  mkdirSync(dirname(TOKENS_FILE), { recursive: true })
  writeFileSync(TOKENS_FILE, themeCssText())
}
