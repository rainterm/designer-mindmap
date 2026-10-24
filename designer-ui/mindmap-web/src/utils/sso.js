import { adminWebUrl } from '@/settings'

export function safeRedirect(path) {
  const raw = String(path || '/')
  if (!raw.startsWith('/') || raw.startsWith('//')) return '/'
  return raw
}

export function goAdminSsoLogin(redirectPath = '/') {
  const back = `${window.location.origin}/sso?redirect=${encodeURIComponent(safeRedirect(redirectPath))}`
  window.location.href = `${adminWebUrl}/login?redirect=${encodeURIComponent(back)}`
}
