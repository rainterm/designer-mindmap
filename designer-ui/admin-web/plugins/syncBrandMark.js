import { writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { brandMarkSvg } from '../src/theme/brandMark.js'

/**
 * 用主题 radius / primary 同步写入 favicon 与静态 logo.svg
 * （浏览器标签页加载的 favicon 无法读页面 CSS 变量，只能写进文件）
 */
function writeBrandAssets(root) {
  const svg = brandMarkSvg()
  writeFileSync(resolve(root, 'public/favicon.svg'), svg)
  writeFileSync(resolve(root, 'src/assets/logo.svg'), svg)
}

export function syncBrandMark() {
  return {
    name: 'sync-brand-mark',
    buildStart() {
      writeBrandAssets(process.cwd())
    },
    configureServer(server) {
      writeBrandAssets(process.cwd())
      const themeFile = resolve(process.cwd(), 'src/theme/index.js')
      server.watcher.add(themeFile)
      server.watcher.on('change', (file) => {
        if (file === themeFile || file.endsWith('/theme/index.js')) {
          writeBrandAssets(process.cwd())
        }
      })
    }
  }
}
