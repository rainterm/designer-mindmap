import request from './request'

export const logout = () => request.post('/admin-api/system/auth/logout')

export const getUserInfo = () => request.get('/admin-api/system/auth/info')

export const exchangeSsoTicket = (ticket) =>
  request.post('/admin-api/system/auth/sso/exchange', { ticket })
