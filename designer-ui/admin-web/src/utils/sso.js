import { createSsoTicket } from '@/api/auth'
import { getSsoClientByKey } from '@/modules/system/api/system'

export function appendTicket(url, ticket) {
  const u = new URL(url)
  u.searchParams.set('ticket', ticket)
  return u.toString()
}

function isSameOriginRedirect(redirect) {
  try {
    const u = new URL(String(redirect), window.location.origin)
    return u.origin === window.location.origin
  } catch {
    return true
  }
}

/** 站内相对路径才可 router 跳转（防开放重定向） */
export function safeInternalRedirect(path) {
  const raw = String(path || '/')
  if (!raw.startsWith('/') || raw.startsWith('//')) return '/'
  return raw
}

/**
 * 合法跨站 SSO 回跳：由服务端白名单校验后签发 ticket 并离开本站。
 * 同站 / 非法地址返回 false。
 */
export async function continueSsoReturn(redirect) {
  if (!redirect) return false
  if (isSameOriginRedirect(redirect)) return false
  let absolute
  try {
    absolute = new URL(String(redirect)).toString()
  } catch {
    return false
  }
  const ticketRes = await createSsoTicket({ redirect: absolute })
  window.location.href = appendTicket(absolute, ticketRes.data.ticket)
  return true
}

function buildSsoEntryUrl(client, path = '/') {
  const openUrl = String(client.openUrl || client.origin || '').replace(/\/$/, '')
  const entry = String(client.entryPath || '/sso')
  const entryPath = entry.startsWith('/') ? entry : `/${entry}`
  const redirect = path.startsWith('/') ? path : `/${path}`
  const u = new URL(`${openUrl}${entryPath}`)
  u.searchParams.set('redirect', redirect)
  return u
}

async function goSsoError(msg) {
  const { default: router } = await import('@/router')
  await router.push({
    path: '/sso-error',
    query: { msg: msg || 'SSO 客户端不可用，请检查「系统管理 → SSO客户端」配置' }
  })
}

/** 按客户端标识打开发前端并带 SSO ticket */
export async function openSsoClient(clientKey = '', path = '/') {
  try {
    const res = await getSsoClientByKey(clientKey)
    const client = res.data
    if (!client?.openUrl && !client?.origin) {
      throw new Error(`SSO 客户端未配置打开地址：${clientKey}`)
    }
    const entryUrl = buildSsoEntryUrl(client, path)
    // 打开地址来自后台白名单配置，发票无需再带 redirect
    const ticketRes = await createSsoTicket()
    entryUrl.searchParams.set('ticket', ticketRes.data.ticket)
    window.open(entryUrl.toString(), '_blank', 'noopener')
  } catch (e) {
    await goSsoError(e?.message || `SSO 客户端不可用：${clientKey}`)
  }
}
