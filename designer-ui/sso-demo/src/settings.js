/**
 * 环境配置（.env 需 VITE_ 前缀）
 */
const env = import.meta.env

/** 接口前缀（开发期走 Vite 代理保持同源，默认空） */
export const apiBase = env.VITE_API_BASE || ''

/** 未登录时跳转的后台登录页地址 */
export const adminLoginUrl = env.VITE_ADMIN_LOGIN_URL || 'http://localhost:5173/login'

export default { apiBase, adminLoginUrl }
