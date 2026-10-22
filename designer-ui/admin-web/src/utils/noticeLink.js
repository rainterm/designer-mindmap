/** 解析通知跳转路径：优先 link / remark 中的 link: 前缀 */
export function resolveNoticeLink(item) {
  if (!item) return ''
  if (item.link && String(item.link).startsWith('/')) {
    return String(item.link)
  }
  const remark = String(item.remark || '')
  if (remark.startsWith('link:')) {
    const path = remark.slice(5).trim()
    if (path.startsWith('/')) return path
  }
  return ''
}
