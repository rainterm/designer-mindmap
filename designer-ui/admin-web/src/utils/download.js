import { ref } from 'vue'
import request from '@/api/request'
import { getToken } from '@/utils/auth'
import { message } from '@/utils/feedback'

/** 全局下载中（导出 / 模板），供按钮 :loading 绑定，防止重复点击 */
export const fileDownloading = ref(false)

/**
 * 下载二进制文件（Excel 等）
 * @param {string} url
 * @param {object} [params]
 * @param {string} [fallbackName]
 * @returns {Promise<boolean>} 是否成功触发下载
 */
export async function downloadFile(url, params = {}, fallbackName = 'export.xlsx') {
  if (fileDownloading.value) {
    message.warning('文件正在下载，请稍候')
    return false
  }
  fileDownloading.value = true
  try {
    const res = await request.get(url, {
      params,
      responseType: 'blob',
      timeout: 120000
    })
    const blob = res instanceof Blob ? res : new Blob([res])
    // 后端若返回 JSON 错误，blob 里是文本
    if (blob.type && blob.type.includes('application/json')) {
      const text = await blob.text()
      try {
        const json = JSON.parse(text)
        message.error(json.msg || '下载失败')
      } catch {
        message.error('下载失败')
      }
      return false
    }
    const link = document.createElement('a')
    const href = URL.createObjectURL(blob)
    link.href = href
    link.download = fallbackName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(href)
    return true
  } catch {
    // 错误提示已由请求拦截器处理
    return false
  } finally {
    fileDownloading.value = false
  }
}

/**
 * 上传 Excel 并解析统一响应
 */
export async function uploadExcel(url, file) {
  const form = new FormData()
  form.append('file', file)
  return request.post(url, form, {
    headers: { 'Content-Type': 'multipart/form-data', Authorization: getToken() },
    timeout: 120000
  })
}
