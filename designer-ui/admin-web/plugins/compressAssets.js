import { promises as fs } from 'node:fs'
import { constants, gzip } from 'node:zlib'
import { promisify } from 'node:util'
import { extname, join, resolve } from 'node:path'
import { minify } from 'vite'

const gzipAsync = promisify(gzip)

/**
 * 构建期预压缩的扩展名白名单。
 *
 * <p>只压文本类资源：png / jpg / woff2 自己就是压缩格式，再 gzip 一遍不但
 * 省不下体积，还常常比原文件更大，只是白占镜像空间。</p>
 */
const COMPRESSIBLE_EXTENSIONS = new Set(['.html', '.css', '.js', '.mjs', '.json', '.svg', '.xml', '.txt'])

/** 低于这个体积的文件 gzip 后只剩几百字节，不值得多一份文件和一条 404 分支 */
const MIN_COMPRESS_SIZE = 1024

/**
 * 生成 `.gz` 供 nginx 的 `gzip_static` 直接下发（见 deploy/nginx/default.conf）。
 *
 * <p>为什么不交给 nginx 现场压：`gzip on` 是每个请求现算，缓存未命中时全部压在
 * worker 的 CPU 上；而这些文件名带内容哈希，内容永远不变，压一次就够了。
 * `closeBundle` 里跑是为了等 public/ 资源也复制进 dist 之后再扫。</p>
 */
export function compressAssets() {
  let outDir = ''
  let logger = console
  let enabled = true

  return {
    name: 'compress-assets',
    apply: 'build',
    configResolved(config) {
      outDir = resolve(config.root, config.build.outDir)
      logger = config.logger
      // `vite build --write false`（只生成不落盘）时没有文件可压
      enabled = config.build.write !== false
    },
    async closeBundle() {
      if (!enabled) return

      let files
      try {
        files = await listFiles(outDir)
      } catch {
        logger.warn(`[compress-assets] 未找到产物目录 ${outDir}，跳过 gzip 预压缩`)
        return
      }

      let count = 0
      let rawBytes = 0
      let gzBytes = 0

      for (const file of files) {
        if (!COMPRESSIBLE_EXTENSIONS.has(extname(file).toLowerCase())) continue

        const content = await fs.readFile(file)
        if (content.length < MIN_COMPRESS_SIZE) continue

        const compressed = await gzipAsync(content, { level: constants.Z_BEST_COMPRESSION })
        // 已经压不动的文件不落地：nginx 找不到 .gz 会回落到 `gzip on`，行为不变
        if (compressed.length >= content.length) continue

        await fs.writeFile(`${file}.gz`, compressed)
        count += 1
        rawBytes += content.length
        gzBytes += compressed.length
      }

      if (count > 0) {
        logger.info(`[compress-assets] 预压缩 ${count} 个文件：${formatSize(rawBytes)} → ${formatSize(gzBytes)}（gzip -9）`)
      }
    }
  }
}

/**
 * 生产构建时压缩 index.html。
 *
 * <p>Vite 只压缩 JS / CSS（以及 HTML 里的内联 `<style>`），`index.html` 本身的
 * 换行、缩进和注释是原样输出的；内联 `<script>` 也不会被碰。这里补上：
 * 脚本交给 Vite 导出的 Oxc minifier，其余部分折叠空白并去掉注释。</p>
 */
export function minifyHtml() {
  return {
    name: 'minify-html',
    apply: 'build',
    transformIndexHtml: {
      // post：等 Vite 把入口 script / stylesheet / modulepreload 都注入完再压
      order: 'post',
      handler: minifyHtmlString
    }
  }
}

const INLINE_SCRIPT_RE = /<script([^>]*)>([\s\S]*?)<\/script>/gi
/** `<style>` 里 Vite 已经压过，但统一抠出来，避免下面的空白折叠动到代码内容 */
const STYLE_RE = /<style\b[^>]*>[\s\S]*?<\/style>/gi
/** 空白有语义的容器，折叠前必须原样保留 */
const WHITESPACE_SENSITIVE_RE = /<(pre|textarea)\b[^>]*>[\s\S]*?<\/\1>/gi

/**
 * 块级标签两侧的空白对渲染没有影响，可以安全删掉。
 * 内联标签（a / span / img …）之间的空白是词间距，必须保留 —— 这是与
 * "无脑删掉所有 > 与 < 之间的空白" 的区别。
 */
const BLOCK_TAGS =
  'address|article|aside|blockquote|body|div|dl|dt|dd|fieldset|figcaption|figure|footer|form|h[1-6]|head|header|hr|html|li|main|nav|ol|p|section|table|tbody|td|tfoot|th|thead|title|tr|ul|script|style|link|meta|template'
const AFTER_BLOCK_TAG_RE = new RegExp(`(</?(?:${BLOCK_TAGS})\\b[^>]*>)\\s+(?=<)`, 'gi')
const BEFORE_BLOCK_TAG_RE = new RegExp(`>\\s+(?=<(?:${BLOCK_TAGS})\\b)`, 'gi')

const COMMENT_RE = /<!--(?!\[if)[\s\S]*?-->/g

async function minifyHtmlString(html) {
  const stash = []
  // S = script/style（块级、两侧空白无意义）；P = pre/textarea（周围空白可能影响布局）
  const hide = (kind) => (block) => {
    stash.push(block)
    return `\uE000${kind}${stash.length - 1}\uE001`
  }

  // 1. 内联脚本用 Oxc 压缩。`//` 注释靠换行结束，直接折叠空白会把它后面的代码吞掉，
  //    所以必须走真正的 minifier，不能只做字符串替换。
  let output = await replaceAsync(html, INLINE_SCRIPT_RE, async (match, attrs, code) => {
    if (/\bsrc\s*=/i.test(attrs) || !code.trim() || !isJavaScriptType(attrs)) return match
    const result = await minify('inline-script.js', code)
    return `<script${attrs}>${result.code.trim()}</script>`
  })

  // 2. 把代码块和 pre/textarea 换成占位符，后面才能安全地全局折叠空白
  output = output.replace(INLINE_SCRIPT_RE, hide('S'))
  output = output.replace(STYLE_RE, hide('S'))
  output = output.replace(WHITESPACE_SENSITIVE_RE, hide('P'))

  // 3. 去注释 + 折叠空白。普通文本节点里连续空白与单个空格等价，不会丢信息
  output = output
    .replace(COMMENT_RE, '')
    .replace(/\s+/g, ' ')
    .replace(AFTER_BLOCK_TAG_RE, '$1')
    .replace(BEFORE_BLOCK_TAG_RE, '>')
    // 第 2 步换了占位符，`(?=<)` / `(?=<block)` 看不到它，两侧空白在这里补删
    .replace(/\s*(\uE000S\d+\uE001)\s*/g, '$1')
    .trim()

  return output.replace(/\uE000([SP])(\d+)\uE001/g, (_, _kind, index) => stash[Number(index)])
}

/** `<script type>` 为空 / module / JavaScript MIME 时才适合用 JS minifier */
function isJavaScriptType(attrs) {
  const match = /\btype\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))/i.exec(attrs)
  if (!match) return true
  const type = (match[1] ?? match[2] ?? match[3] ?? '').trim().toLowerCase()
  return type === '' || type === 'module' || type === 'text/javascript' || type === 'application/javascript'
}

/** String.prototype.replace 不支持 async 回调，这里按匹配位置逐段拼接 */
async function replaceAsync(input, regex, replacer) {
  const parts = []
  let lastIndex = 0
  for (const match of input.matchAll(regex)) {
    parts.push(input.slice(lastIndex, match.index))
    parts.push(await replacer(...match))
    lastIndex = match.index + match[0].length
  }
  parts.push(input.slice(lastIndex))
  return parts.join('')
}

async function listFiles(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const fullPath = join(dir, entry.name)
    if (entry.isDirectory()) files.push(...(await listFiles(fullPath)))
    else if (entry.isFile()) files.push(fullPath)
  }
  return files
}

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
