import request from '@/api/request'

/** 导图项目分页 */
export const pageProjects = (params) =>
  request.get('/admin-api/mindmap/project/page', { params })

/** 导图项目详情 */
export const getProject = (id) => request.get(`/admin-api/mindmap/project/${id}`)

/** 新增导图项目 */
export const createProject = (data) =>
  request.post('/admin-api/mindmap/project/add', data || {})

/** 修改导图项目 */
export const updateProject = (data) =>
  request.put('/admin-api/mindmap/project/update', data)

/** 删除导图项目 */
export const deleteProjects = (ids) =>
  request.delete('/admin-api/mindmap/project/remove', {
    params: { ids: ids.join(',') }
  })

/** 分类树（含未分类数量） */
export const listCategories = () => request.get('/admin-api/mindmap/category/list')

/** 新增分类 */
export const createCategory = (data) =>
  request.post('/admin-api/mindmap/category/add', data || {})

/** 修改分类 */
export const updateCategory = (data) =>
  request.put('/admin-api/mindmap/category/update', data)

/** 删除分类 */
export const deleteCategory = (id) =>
  request.delete(`/admin-api/mindmap/category/remove/${id}`)
