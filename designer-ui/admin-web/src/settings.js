/**
 * 前端环境配置（来自 .env / .env.[mode]，需 VITE_ 前缀）
 */
const env = import.meta.env

export const appTitle = env.VITE_APP_TITLE || 'Designer Admin'
export const apiBase = env.VITE_API_BASE || ''

export default {
  appTitle,
  apiBase
}
