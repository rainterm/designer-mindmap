const env = import.meta.env

export const appTitle = env.VITE_APP_TITLE || '思维导图'
export const apiBase = env.VITE_API_BASE || ''
export const adminWebUrl = (env.VITE_ADMIN_WEB_URL || 'http://localhost:5173').replace(/\/$/, '')
