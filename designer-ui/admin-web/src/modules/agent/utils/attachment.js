const IMAGE_EXTS = new Set(['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg', 'ico', 'avif'])

/** 是否按图片展示（contentType 或文件名/URL 后缀）。 */
export function isImageAttachment(file) {
  if (!file) return false
  const type = String(file.contentType || file.type || '').toLowerCase()
  if (type.startsWith('image/')) return true
  const name = String(file.name || file.url || '')
  const path = name.split('?')[0].split('#')[0]
  const ext = path.includes('.') ? path.split('.').pop().toLowerCase() : ''
  return IMAGE_EXTS.has(ext)
}
