import JSZip from 'jszip'

/**
 * 解析 Skill 压缩包（.zip），提取 SKILL.md 与包内文件清单。
 * 兼容：
 * - 根目录 SKILL.md
 * - 单层目录 skill-name/SKILL.md
 */

const SKILL_MD_RE = /(^|\/)SKILL\.md$/i
const ARCHIVE_EXT_RE = /\.(zip)$/i

export function isSkillArchiveName(name = '') {
  return ARCHIVE_EXT_RE.test(String(name))
}

export function formatPackageSize(size) {
  const n = Number(size) || 0
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(2)} MB`
}

function parseFrontmatter(raw) {
  const text = String(raw || '').replace(/^\uFEFF/, '')
  const m = text.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$/)
  if (!m) {
    return { meta: {}, body: text, content: text }
  }
  const meta = {}
  for (const line of m[1].split(/\r?\n/)) {
    const idx = line.indexOf(':')
    if (idx <= 0) continue
    const key = line.slice(0, idx).trim()
    let val = line.slice(idx + 1).trim()
    if (
      (val.startsWith('"') && val.endsWith('"')) ||
      (val.startsWith("'") && val.endsWith("'"))
    ) {
      val = val.slice(1, -1)
    }
    if (key) meta[key] = val
  }
  return { meta, body: m[2] || '', content: text }
}

function pickSkillEntry(paths) {
  const matches = paths.filter((p) => SKILL_MD_RE.test(p) && !p.includes('__MACOSX'))
  if (!matches.length) return null
  // 优先根目录，其次路径最短
  matches.sort((a, b) => {
    const da = a.split('/').length
    const db = b.split('/').length
    if (da !== db) return da - db
    return a.localeCompare(b)
  })
  return matches[0]
}

function guessCodeFromPath(skillPath, packageName) {
  const parts = skillPath.split('/').filter(Boolean)
  if (parts.length >= 2) return parts[parts.length - 2]
  const base = String(packageName || '')
    .replace(/\.zip$/i, '')
    .replace(/[^a-zA-Z0-9-_]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return base || 'skill'
}

/**
 * @param {File|Blob} file
 * @returns {Promise<{
 *   name: string
 *   code: string
 *   description: string
 *   content: string
 *   packageName: string
 *   packageSize: number
 *   files: string[]
 *   skillPath: string
 * }>}
 */
export async function parseSkillPackage(file) {
  if (!file) throw new Error('请选择压缩包')
  const packageName = file.name || 'skill.zip'
  if (!isSkillArchiveName(packageName)) {
    throw new Error('仅支持 .zip 压缩包')
  }

  let zip
  try {
    zip = await JSZip.loadAsync(file)
  } catch {
    throw new Error('压缩包无法解析，请确认是有效的 .zip 文件')
  }

  const entries = Object.keys(zip.files).filter((p) => {
    const f = zip.files[p]
    return f && !f.dir && !p.includes('__MACOSX') && !p.split('/').pop()?.startsWith('.')
  })
  if (!entries.length) throw new Error('压缩包为空')

  const skillPath = pickSkillEntry(entries)
  if (!skillPath) throw new Error('未找到 SKILL.md，请按 Skill 规范打包')

  const content = await zip.files[skillPath].async('string')
  const { meta } = parseFrontmatter(content)
  const code = String(meta.name || guessCodeFromPath(skillPath, packageName)).trim()
  const description = String(meta.description || '').trim()
  const name = String(meta.title || meta.name || code).trim() || code

  return {
    name,
    code,
    description: description || `从压缩包 ${packageName} 导入`,
    content,
    packageName,
    packageSize: file.size || 0,
    files: entries.sort(),
    skillPath
  }
}
