import request from './request'

/** 登录 */
export const login = (data) => request.post('/admin-api/system/auth/login', data)

/**
 * 询问该账号本次登录是否需要人机验证。
 *
 * <p>只返回 `{ enabled }` —— 滑块本身由 AJ-Captcha 的 `/app-api/captcha/get` 下发
 * （见 `@/api/captcha`）。</p>
 */
export const getCaptcha = (username) =>
  request.get('/admin-api/system/auth/captcha', { params: { username: username || '' } })

/** 退出登录 */
export const logout = () => request.post('/admin-api/system/auth/logout')

/** 当前用户信息与权限 */
export const getUserInfo = () => request.get('/admin-api/system/auth/info')

/** 当前用户动态路由 */
export const getRouters = () => request.get('/admin-api/system/auth/routers')

/** 已登录签发一次性 SSO 票据；data.redirect 可选，服务端按白名单校验 */
export const createSsoTicket = (data) => request.post('/admin-api/system/auth/sso/ticket', data || {})

/** 一次性票据换 token */
export const exchangeSsoTicket = (ticket) =>
  request.post('/admin-api/system/auth/sso/exchange', { ticket })

/** 个人中心资料：查询无参；修改走 JSON 请求体（ProfileUpdateDTO） */
export const getProfile = () => request.get('/admin-api/system/profile')
export const updateProfile = (data) => request.put('/admin-api/system/profile/update', data)

/** 改密：与资料修改一致，走 JSON 请求体（PasswordUpdateDTO），避免密码出现在 URL 查询串里被访问日志记录 */
export const changePassword = (data) => request.put('/admin-api/system/profile/password', data)
