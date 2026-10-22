import axios from 'axios'
import { getToken, clearToken } from '@/utils/auth'
import { message } from '@/utils/feedback'

/** 统一响应状态码 */
export const CODE = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  // 429：被限流（应用层 @RateLimit 或 nginx limit_req，两层语义一致）
  TOO_MANY_REQUESTS: 429,
  ERROR: 500
}

/** 后端返回 401 时通知路由跳转登录页 */
let onUnauthorized = null
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000
})

// ------------------------------------------------------------------
// 请求拦截
// ------------------------------------------------------------------
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = token
    }
    // FormData 交由浏览器自动带 multipart boundary，勿强制 json
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      if (config.headers) {
        delete config.headers['Content-Type']
        delete config.headers['content-type']
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ------------------------------------------------------------------
// 响应拦截
// ------------------------------------------------------------------
service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 非统一响应结构（如文件流），直接返回
    if (res == null || typeof res !== 'object' || res.code === undefined) {
      return res
    }

    if (res.code === CODE.SUCCESS) {
      return res
    }

    if (res.code === CODE.UNAUTHORIZED) {
      clearToken()
      message.warning(res.msg || '登录状态已失效，请重新登录')
      if (onUnauthorized) {
        onUnauthorized()
      }
      return Promise.reject(new Error(res.msg || '未登录'))
    }

    // silent：后台轮询类请求（如菜单红点）失败时不弹提示，避免反复打扰
    if (!response.config.silent) {
      message.error(res.msg || '请求失败')
    }
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      clearToken()
      if (onUnauthorized) {
        onUnauthorized()
      }
      message.warning('登录状态已失效，请重新登录')
    } else if (!error.config?.silent) {
      message.error(error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
