import request from './request'

/** 一次性票据换 token（免登录）；resolve 为 { code, msg, data:LoginVO } */
export const exchangeSsoTicket = (ticket) =>
  request.post('/admin-api/system/auth/sso/exchange', { ticket })

/** 当前登录用户信息（验证 SSO 换来的 token 有效） */
export const getUserInfo = () => request.get('/admin-api/system/auth/info')
