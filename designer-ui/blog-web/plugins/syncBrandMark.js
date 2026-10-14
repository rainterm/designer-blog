import { writeFileSync, mkdirSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { brandMarkSvg } from '../src/config/brandMark.js'

function writeBrandAssets(root) {
  const svg = brandMarkSvg()
  const favicon = resolve(root, 'public/favicon.svg')
  const logo = resolve(root, 'src/assets/logo.svg')
  mkdirSync(dirname(favicon), { recursive: true })
  mkdirSync(dirname(logo), { recursive: true })
  writeFileSync(favicon, svg)
  writeFileSync(logo, svg)
}

export function syncBrandMark() {
  return {
    name: 'sync-brand-mark',
    buildStart() {
      writeBrandAssets(process.cwd())
    },
    configureServer(server) {
      writeBrandAssets(process.cwd())
      const themeFile = resolve(process.cwd(), 'src/config/theme.js')
      server.watcher.add(themeFile)
      server.watcher.on('change', (file) => {
        if (file === themeFile || file.endsWith('/config/theme.js')) {
          writeBrandAssets(process.cwd())
        }
      })
    }
  }
}
