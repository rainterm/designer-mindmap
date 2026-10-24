import axios from 'axios'
import { getToken, clearToken } from '@/utils/auth'
import { message } from '@/utils/feedback'

export const CODE = {
  SUCCESS: 200,
  UNAUTHORIZED: 401
}

let onUnauthorized = null
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000
})

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res == null || typeof res !== 'object' || res.code === undefined) {
      return res
    }
    if (res.code === CODE.SUCCESS) {
      return res
    }
    if (res.code === CODE.UNAUTHORIZED) {
      clearToken()
      message.warning(res.msg || '登录状态已失效，请重新登录')
      onUnauthorized?.()
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    message.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearToken()
      onUnauthorized?.()
      message.warning('登录状态已失效，请重新登录')
    } else {
      message.error(error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
