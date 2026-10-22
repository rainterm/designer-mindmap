import request from '@/api/request'

/**
 * 通用文件上传，dir 为业务子目录（如 avatar）
 *
 * @param {File} file
 * @param {string} [dir]
 * @param {{ configId?: number|string, publicOnly?: boolean }} [options]
 *        configId 指定存储配置（留空用默认）；publicOnly 要求必须是公开配置，
 *        用于登录页 Logo 这类匿名访客要能访问的图片
 */
export const uploadFile = (file, dir = 'common', options = {}) => {
  const form = new FormData()
  form.append('file', file)
  form.append('dir', dir)
  if (options.configId != null && options.configId !== '') {
    form.append('configId', options.configId)
  }
  if (options.publicOnly) {
    form.append('publicOnly', 'true')
  }
  return request.post('/admin-api/system/file/upload', form)
}

/** 上传可选的公开存储配置（不含密钥字段，仅启用的公开项） */
export const listPublicStorageOptions = () => request.get('/admin-api/system/file/storage-options')

/** 文件分页列表 */
export const pageFiles = (params) => request.get('/admin-api/system/file/page', { params })

/** 文件详情 */
export const getFile = (id) => request.get(`/admin-api/system/file/${id}`)

/** 批量删除文件 */
export const deleteFiles = (ids) =>
  request.delete('/admin-api/system/file/remove', { params: { ids: ids.join(',') } })
