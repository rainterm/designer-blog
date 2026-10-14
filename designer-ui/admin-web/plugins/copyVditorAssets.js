import { cpSync, existsSync, mkdirSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'

/** Vditor 运行时需要通过 cdn 前缀动态加载的资源（其余为按需渲染的图表引擎，用不到） */
const ASSETS = ['js/lute', 'js/highlight.js', 'js/i18n', 'js/icons', 'css/content-theme', 'images/emoji']

function resolveVditorDist() {
  try {
    const require = createRequire(import.meta.url)
    return resolve(dirname(require.resolve('vditor/package.json')), 'dist')
  } catch {
    return null
  }
}

function copyVditorAssets(root) {
  const dist = resolveVditorDist()
  if (!dist) return
  const dest = resolve(root, 'public/vditor/dist')
  ASSETS.forEach((rel) => {
    const from = resolve(dist, rel)
    if (!existsSync(from)) return
    const to = resolve(dest, rel)
    mkdirSync(dirname(to), { recursive: true })
    cpSync(from, to, { recursive: true })
  })
}

/**
 * Vditor 默认从 unpkg 拉取 lute 引擎 / 代码高亮 / 图标 / 语言包等静态资源，
 * 内网或国内网络下易失败导致编辑器不可用。这里把它们复制到 public/vditor，
 * 配合 Vditor 的 `cdn: '/vditor'` 使用（public/vditor 已 gitignore，构建时自动生成）。
 */
export function copyVditorAssetsPlugin() {
  return {
    name: 'copy-vditor-assets',
    buildStart() {
      copyVditorAssets(process.cwd())
    },
    configureServer() {
      copyVditorAssets(process.cwd())
    }
  }
}
