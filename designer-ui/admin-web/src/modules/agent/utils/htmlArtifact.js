/**
 * 从助手 Markdown 回复中提取可下载的完整 HTML 文件。
 * 仅接受结构完整、体积足够的文档，避免截断/草稿被当成文件。
 */

const FENCED_HTML_RE =
  /```(?:html|htm)\s*\r?\n([\s\S]*?)```/gi

const MIN_HTML_BYTES = 400

function looksLikeCompleteHtml(text = '') {
  const t = String(text).trim()
  if (!t || t.length < MIN_HTML_BYTES) return false
  const lower = t.toLowerCase()
  const hasDoc =
    lower.includes('<!doctype html') ||
    (/<html[\s>]/i.test(t) && /<\/html>/i.test(t))
  if (!hasDoc) return false
  if (!/<\/html>/i.test(t)) return false
  // 明显截断：围栏内以未闭合标签/字符串尾巴结束
  if (/\{\s*$/.test(t) || /=\s*`[^`]*$/.test(t) || /setProperty\(\s*'[^']*$/.test(t)) {
    return false
  }
  return true
}

function guessName(html, index) {
  const title =
    html.match(/<title[^>]*>([\s\S]*?)<\/title>/i)?.[1]?.replace(/\s+/g, ' ').trim() ||
    html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i)?.[1]?.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  if (title) {
    const safe = title
      .replace(/[\\/:*?"<>|]/g, '')
      .replace(/\s+/g, '-')
      .slice(0, 40)
    if (safe) return `${safe}.html`
  }
  return index === 0 ? 'index.html' : `design-${index + 1}.html`
}

/**
 * @param {string} content
 * @returns {{ name: string, html: string, lang: string }[]}
 */
export function extractHtmlArtifacts(content) {
  const raw = String(content || '')
  if (!raw.trim()) return []

  const items = []
  const seen = new Set()

  for (const m of raw.matchAll(FENCED_HTML_RE)) {
    const html = (m[1] || '').trim()
    if (!looksLikeCompleteHtml(html)) continue
    const key = html.slice(0, 240)
    if (seen.has(key)) continue
    seen.add(key)
    items.push({
      name: guessName(html, items.length),
      html,
      lang: 'html'
    })
  }

  // 无代码围栏时，整段须为完整 HTML 文档
  if (!items.length && looksLikeCompleteHtml(raw)) {
    const html = raw.trim()
    items.push({
      name: guessName(html, 0),
      html,
      lang: 'html'
    })
  }

  return items
}

export function downloadHtmlFile(name, html) {
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = name || 'index.html'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1500)
}

export function previewHtmlFile(html) {
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const win = window.open(url, '_blank', 'noopener,noreferrer')
  if (!win) {
    URL.revokeObjectURL(url)
    throw new Error('浏览器拦截了预览窗口，请允许弹窗后重试')
  }
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
