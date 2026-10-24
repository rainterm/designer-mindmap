import request from './request'

export const pageProjects = (params) => request.get('/admin-api/mindmap/project/page', { params })
export const getProject = (id) => request.get(`/admin-api/mindmap/project/${id}`)
export const createProject = (data) => request.post('/admin-api/mindmap/project/add', data || {})
export const updateProject = (data) => request.put('/admin-api/mindmap/project/update', data)
export const deleteProjects = (ids) =>
  request.delete('/admin-api/mindmap/project/remove', { params: { ids: ids.join(',') } })
