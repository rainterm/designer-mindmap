import request from '@/api/request'

/** Demo 模块 API：连通性探测 */
export const pingDemo = () => request.get('/admin-api/demo/ping')
