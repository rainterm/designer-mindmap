import { brandMarkSvg } from '@/theme/brandMark'

/**
 * 浏览器标签图标（favicon）跟随系统设置里的 Logo 与圆角。
 *
 * <p>标签图标是浏览器根据 `<link rel="icon">` 的 href 单独取的，不走页面 CSS，
 * 所以无法像 `BrandLogo.vue` 那样靠 `var(--color-primary)` 或 `img` 自适应：
 * 必须在这里主动写 href —— 自定义 Logo 走 canvas 裁圆角后导出 PNG，
 * 未配置 Logo 时用主题色重绘内置 SVG。</p>
 *
 * <p>动态替换页面 favicon：
 * 保持两个站点的标签图标观感一致。</p>
 */

/** favicon 画布尺寸 */
const FAVICON_SIZE = 64

/** Logo 展示尺寸：用于把「px 圆角」等比换算到 favicon 画布（与侧边栏 / 顶栏的 28px 一致） */
const LOGO_DISPLAY_SIZE = 28

/** 生成序号，避免异步裁剪的旧图覆盖新图 */
let token = 0

function ensureIconLink() {
  let link = document.querySelector('link[rel="icon"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  // index.html 里写的是 type="image/svg+xml"，而这里可能写入 PNG data URL，
  // 类型交给浏览器嗅探，避免类型与实际内容不符导致不显示
  link.removeAttribute('type')
  return link
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    // 允许 canvas 绘制（跨域未开 CORS 时会加载失败，由调用方回退）
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('图片加载失败'))
    img.src = src
  })
}

function roundRectPath(ctx, size, radius) {
  const r = Math.max(0, Math.min(radius, size / 2))
  ctx.beginPath()
  ctx.moveTo(r, 0)
  ctx.arcTo(size, 0, size, size, r)
  ctx.arcTo(size, size, 0, size, r)
  ctx.arcTo(0, size, 0, 0, r)
  ctx.arcTo(0, 0, size, 0, r)
  ctx.closePath()
}

/** 圆角换算：设置值基于 Logo 展示尺寸（28px），换算到 favicon 画布（64px） */
function toCanvasRadius(radius) {
  const scaled = ((Number(radius) || 0) / LOGO_DISPLAY_SIZE) * FAVICON_SIZE
  return Math.max(0, Math.min(scaled, FAVICON_SIZE / 2))
}

/** 内置 Logo 的 favicon：按主题色与圆角生成 SVG data URL（必须转义 # 等字符） */
function builtinFaviconDataUrl(radius, primaryColor) {
  const svg = brandMarkSvg({ fill: primaryColor, rx: toCanvasRadius(radius) })
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

/** 把自定义 Logo 按圆角裁剪并等比缩放，导出为 favicon 的 PNG data URL */
async function buildRoundedFavicon(src, radius) {
  const img = await loadImage(src)
  const canvas = document.createElement('canvas')
  canvas.width = FAVICON_SIZE
  canvas.height = FAVICON_SIZE
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('canvas 不可用')

  // 圆角随画布等比放大（与顶栏 Logo 的观感一致），上限为半边长（圆形）
  roundRectPath(ctx, FAVICON_SIZE, toCanvasRadius(radius))
  ctx.clip()

  // 等比缩放并居中，等价于 Logo 的 object-fit: contain
  const naturalW = img.naturalWidth || img.width
  const naturalH = img.naturalHeight || img.height
  if (!naturalW || !naturalH) throw new Error('图片尺寸无效')

  const scale = Math.min(FAVICON_SIZE / naturalW, FAVICON_SIZE / naturalH)
  const w = naturalW * scale
  const h = naturalH * scale
  ctx.drawImage(img, (FAVICON_SIZE - w) / 2, (FAVICON_SIZE - h) / 2, w, h)

  return canvas.toDataURL('image/png')
}

/**
 * 应用 favicon。
 *
 * @param {string} [logoUrl]      自定义 Logo 地址；为空则用内置图形
 * @param {number} [logoRadius]   Logo 圆角（px）
 * @param {string} [primaryColor] 主题色，用于绘制内置图形（如 #16A34A）
 */
export function applyFavicon(logoUrl, logoRadius, primaryColor) {
  if (typeof document === 'undefined') return
  const link = ensureIconLink()

  // 未配置 Logo：用内置图形，并按主题色 + 圆角重绘
  if (!logoUrl) {
    token++
    link.href = builtinFaviconDataUrl(logoRadius, primaryColor)
    return
  }

  const current = ++token

  // 直角：直接用原图，省掉一次 canvas 裁剪
  if (!(logoRadius > 0)) {
    link.href = logoUrl
    return
  }

  buildRoundedFavicon(logoUrl, logoRadius)
    .then((dataUrl) => {
      if (current === token) link.href = dataUrl
    })
    .catch(() => {
      // 跨域图片会污染 canvas 导致导出失败：退回原始图片，至少保证有图标
      if (current === token) link.href = logoUrl
    })
}
