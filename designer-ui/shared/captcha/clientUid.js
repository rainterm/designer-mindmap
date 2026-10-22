/**
 * 浏览器的稳定标识，用于 AJ-Captcha 的频控维度。
 *
 * <p>必须传且必须稳定：插件的频控（`app.yml` 里的 `aj.captcha.req-*`）以它为维度，
 * 取不到该值时才会退化成「IP + User-Agent」—— 生产环境所有请求都来自 nginx，
 * 退化的口径会把同一出口 IP 后的所有访客算成一个客户端，一个刷子就能把大家锁在门外。</p>
 *
 * <p>存储键不带站点前缀：两个前端域名不同（www / admin），localStorage 本来就互不相干，
 * 用同一个键名反而少一处需要记住的差异。</p>
 */

/** 浏览器标识的存储键 */
const CLIENT_UID_KEY = 'captcha:clientUid'

/** localStorage 不可用时的内存态兜底 */
let memoryUid = ''

/** 32 位十六进制随机串，生成策略与 commenterKey 一致 */
function randomUid() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '')
  }
  const bytes = new Uint8Array(16)
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256)
  }
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}

/**
 * 取当前浏览器的稳定标识；首次调用时生成并持久化。
 *
 * @returns {string}
 */
export function clientUid() {
  if (typeof window === 'undefined') return ''
  try {
    const cached = window.localStorage.getItem(CLIENT_UID_KEY)
    if (cached) return cached
    const uid = randomUid()
    window.localStorage.setItem(CLIENT_UID_KEY, uid)
    return uid
  } catch {
    // 隐私模式 / 禁用存储：退化成内存态，至少同一次会话内保持稳定
    if (!memoryUid) memoryUid = randomUid()
    return memoryUid
  }
}
