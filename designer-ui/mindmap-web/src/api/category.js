import request from './request'

export const listCategories = () => request.get('/admin-api/mindmap/category/list')

export const createCategory = (data) => request.post('/admin-api/mindmap/category/add', data || {})

export const updateCategory = (data) => request.put('/admin-api/mindmap/category/update', data)

export const deleteCategory = (id) => request.delete(`/admin-api/mindmap/category/remove/${id}`)
