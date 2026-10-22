import axios from 'axios'
import { getToken, clearToken } from '@/utils/auth'
import { apiBase } from '@/settings'

const service = axios.create({
  baseURL: apiBase,
  timeout: 20000
})

// 请求头 Authorization 放 token 原值（本项目约定，无 Bearer 前缀）
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

// 后端统一响应体 { code, msg, data }：直接返回该结构，非 200 走 reject
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res == null || typeof res !== 'object' || res.code === undefined) {
      return res
    }
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      clearToken()
    }
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => Promise.reject(error)
)

export default service
