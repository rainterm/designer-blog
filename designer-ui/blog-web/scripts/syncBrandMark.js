import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

import { brandMarkSvg } from '../app/config/brandMark.js'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')

/** 浏览器标签图标：未配置站点 Logo 时的兜底 */
export const FAVICON_FILE = resolve(ROOT, 'public/favicon.svg')

/**
 * 由 theme.js 的品牌色生成内置 favicon。
 *
 * <p>与 {@link ./syncThemeTokens.js} 同样属于「构建期把配置变成静态产物」的做法：
 * 页面里的 `<link rel="icon">` 指向这个文件，站点配置了 Logo 时再由
 * `app/plugins/favicon.client.js` 在客户端覆盖成对应图片。</p>
 */
export function syncBrandMark() {
  mkdirSync(dirname(FAVICON_FILE), { recursive: true })
  writeFileSync(FAVICON_FILE, brandMarkSvg())
}
