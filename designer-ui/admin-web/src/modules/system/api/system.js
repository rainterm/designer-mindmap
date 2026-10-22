import request from '@/api/request'

// ---------------------- 用户 ----------------------
export const pageUsers = (params) => request.get('/admin-api/system/user/page', { params })
/** 人员选择：仅返回 id/账号/昵称/部门 */
export const pageUserOptions = (params) => request.get('/admin-api/system/user/options', { params })
export const checkUsername = (params) => request.get('/admin-api/system/user/check-username', { params })
export const createUser = (data) => request.post('/admin-api/system/user/add', data)
export const updateUser = (data) => request.put('/admin-api/system/user/update', data)
export const deleteUsers = (ids) => request.delete('/admin-api/system/user/remove', { params: { ids: ids.join(',') } })
export const resetUserPassword = (data) => request.put('/admin-api/system/user/password', data)
export const changeUserStatus = (id, status) =>
  request.put(`/admin-api/system/user/${id}/status`, null, { params: { status } })
export const exportUsers = (params) =>
  import('@/utils/download').then(({ downloadFile }) =>
    downloadFile('/admin-api/system/user/export', params, '用户数据.xlsx')
  )
export const downloadUserImportTemplate = () =>
  import('@/utils/download').then(({ downloadFile }) =>
    downloadFile('/admin-api/system/user/importTemplate', {}, '用户导入模板.xlsx')
  )
export const importUsers = (file) =>
  import('@/utils/download').then(({ uploadExcel }) => uploadExcel('/admin-api/system/user/import', file))

// ---------------------- 角色 ----------------------
export const pageRoles = (params) => request.get('/admin-api/system/role/page', { params })
export const getRoleOptions = () => request.get('/admin-api/system/role/options')
export const createRole = (data) => request.post('/admin-api/system/role/add', data)
export const updateRole = (data) => request.put('/admin-api/system/role/update', data)
export const deleteRoles = (ids) => request.delete('/admin-api/system/role/remove', { params: { ids: ids.join(',') } })
export const getRoleMenuIds = (id) => request.get(`/admin-api/system/role/${id}/menus`)
export const getRoleDeptIds = (id) => request.get(`/admin-api/system/role/${id}/depts`)
export const assignRoleMenus = (id, menuIds) => request.put(`/admin-api/system/role/${id}/menus`, menuIds)

// ---------------------- 岗位 ----------------------
export const pagePosts = (params) => request.get('/admin-api/system/post/page', { params })
export const getPostOptions = () => request.get('/admin-api/system/post/options')
export const createPost = (data) => request.post('/admin-api/system/post/add', data)
export const updatePost = (data) => request.put('/admin-api/system/post/update', data)
export const deletePosts = (ids) => request.delete('/admin-api/system/post/remove', { params: { ids: ids.join(',') } })

// ---------------------- SSO 客户端 ----------------------
export const pageSsoClients = (params) => request.get('/admin-api/system/sso-client/page', { params })
export const listEnabledSsoClients = () => request.get('/admin-api/system/sso-client/enabled')
export const getSsoClientByKey = (clientKey) =>
  request.get(`/admin-api/system/sso-client/by-key/${encodeURIComponent(clientKey)}`)
export const createSsoClient = (data) => request.post('/admin-api/system/sso-client/add', data)
export const updateSsoClient = (data) => request.put('/admin-api/system/sso-client/update', data)
export const deleteSsoClients = (ids) =>
  request.delete('/admin-api/system/sso-client/remove', { params: { ids: ids.join(',') } })

// ---------------------- OAuth2 开放接口 Scope ----------------------
export const pageOauthScopes = (params) => request.get('/admin-api/system/oauth-scope/page', { params })
export const listOauthScopeOptions = () => request.get('/admin-api/system/oauth-scope/options')
export const createOauthScope = (data) => request.post('/admin-api/system/oauth-scope/add', data)
export const updateOauthScope = (data) => request.put('/admin-api/system/oauth-scope/update', data)
export const deleteOauthScopes = (ids) =>
  request.delete('/admin-api/system/oauth-scope/remove', { params: { ids: ids.join(',') } })

// ---------------------- OAuth2 客户端 ----------------------
export const pageOauthClients = (params) => request.get('/admin-api/system/oauth-client/page', { params })
export const createOauthClient = (data) => request.post('/admin-api/system/oauth-client/add', data)
export const updateOauthClient = (data) => request.put('/admin-api/system/oauth-client/update', data)
export const resetOauthClientSecret = (id) =>
  request.post(`/admin-api/system/oauth-client/${id}/reset-secret`)
export const deleteOauthClients = (ids) =>
  request.delete('/admin-api/system/oauth-client/remove', { params: { ids: ids.join(',') } })

// ---------------------- 菜单 ----------------------
export const getMenuTree = (params) => request.get('/admin-api/system/menu/tree', { params })
export const getMenuOptions = () => request.get('/admin-api/system/menu/options')
export const createMenu = (data) => request.post('/admin-api/system/menu/add', data)
export const updateMenu = (data) => request.put('/admin-api/system/menu/update', data)
export const deleteMenu = (id) => request.delete(`/admin-api/system/menu/${id}`)

// ---------------------- 字典 ----------------------
export const pageDictTypes = (params) => request.get('/admin-api/system/dict/type/page', { params })
export const getDictTypeOptions = () => request.get('/admin-api/system/dict/type/options')
export const createDictType = (data) => request.post('/admin-api/system/dict/type', data)
export const updateDictType = (data) => request.put('/admin-api/system/dict/type', data)
export const deleteDictTypes = (ids) => request.delete('/admin-api/system/dict/type', { params: { ids: ids.join(',') } })

export const pageDictData = (params) => request.get('/admin-api/system/dict/data/page', { params })
/** 按类型取启用字典项（下拉） */
export const getDictDataByType = (dictType) => request.get(`/admin-api/system/dict/data/${dictType}`)
export const getDictDataBatch = (types) =>
  request.get('/admin-api/system/dict/data/batch', { params: { types: types.join(',') } })
export const createDictData = (data) => request.post('/admin-api/system/dict/data', data)
export const updateDictData = (data) => request.put('/admin-api/system/dict/data', data)
export const deleteDictData = (ids) => request.delete('/admin-api/system/dict/data', { params: { ids: ids.join(',') } })
export const exportDictTypes = (params) =>
  import('@/utils/download').then(({ downloadFile }) =>
    downloadFile('/admin-api/system/dict/type/export', params, '字典类型.xlsx')
  )
export const downloadDictTypeImportTemplate = () =>
  import('@/utils/download').then(({ downloadFile }) =>
    downloadFile('/admin-api/system/dict/type/importTemplate', {}, '字典类型导入模板.xlsx')
  )
export const importDictTypes = (file) =>
  import('@/utils/download').then(({ uploadExcel }) => uploadExcel('/admin-api/system/dict/type/import', file))
export const exportDictData = (params) =>
  import('@/utils/download').then(({ downloadFile }) =>
    downloadFile('/admin-api/system/dict/data/export', params, '字典数据.xlsx')
  )
export const downloadDictDataImportTemplate = () =>
  import('@/utils/download').then(({ downloadFile }) =>
    downloadFile('/admin-api/system/dict/data/importTemplate', {}, '字典数据导入模板.xlsx')
  )
export const importDictData = (file) =>
  import('@/utils/download').then(({ uploadExcel }) => uploadExcel('/admin-api/system/dict/data/import', file))

// ---------------------- 日志 ----------------------
export const pageOperLogs = (params) => request.get('/admin-api/system/log/oper/page', { params })
export const pageLoginLogs = (params) => request.get('/admin-api/system/log/login/page', { params })

// ---------------------- 站内消息（顶部消息中心）----------------------
export const getNoticeUnreadCount = () => request.get('/admin-api/system/notice/unread-count')
export const getNoticeInbox = (params) => request.get('/admin-api/system/notice/inbox', { params })
export const markNoticeRead = (id) => request.post(`/admin-api/system/notice/${id}/read`)
export const markNoticeReadAll = () => request.post('/admin-api/system/notice/read-all')

// ---------------------- 存储配置 ----------------------
export const listStorageTypes = () => request.get('/admin-api/system/storage/types')
export const pageStorageConfigs = (params) => request.get('/admin-api/system/storage/page', { params })
export const getStorageConfig = (id) => request.get(`/admin-api/system/storage/${id}`)
export const createStorageConfig = (data) => request.post('/admin-api/system/storage/add', data)
export const updateStorageConfig = (data) => request.put('/admin-api/system/storage/update', data)
export const deleteStorageConfigs = (ids) =>
  request.delete('/admin-api/system/storage/remove', { params: { ids: ids.join(',') } })
export const setDefaultStorageConfig = (id) => request.put(`/admin-api/system/storage/${id}/default`)
export const changeStorageConfigStatus = (id, status) =>
  request.put(`/admin-api/system/storage/${id}/status`, null, { params: { status } })

// ---------------------- 消息通道 ----------------------
export const listMessageChannels = () => request.get('/admin-api/system/message/types')
export const pageMessageConfigs = (params) => request.get('/admin-api/system/message/page', { params })
export const getMessageConfig = (id) => request.get(`/admin-api/system/message/${id}`)
export const createMessageConfig = (data) => request.post('/admin-api/system/message/add', data)
export const updateMessageConfig = (data) => request.put('/admin-api/system/message/update', data)
export const deleteMessageConfigs = (ids) =>
  request.delete('/admin-api/system/message/remove', { params: { ids: ids.join(',') } })
export const setDefaultMessageConfig = (id) => request.put(`/admin-api/system/message/${id}/default`)
export const changeMessageConfigStatus = (id, status) =>
  request.put(`/admin-api/system/message/${id}/status`, null, { params: { status } })
export const sendMessage = (data) => request.post('/admin-api/system/message/send', data)

// ---------------------- 消息记录 ----------------------
export const pageMessageRecords = (params) => request.get('/admin-api/system/msgrecord/page', { params })
export const deleteMessageRecords = (ids) =>
  request.delete('/admin-api/system/msgrecord/remove', { params: { ids: ids.join(',') } })
export const clearMessageRecords = () => request.delete('/admin-api/system/msgrecord/clear')

// ---------------------- 定时任务 ----------------------
export const pageJobs = (params) => request.get('/admin-api/system/job/page', { params })
export const getJob = (id) => request.get(`/admin-api/system/job/${id}`)
export const createJob = (data) => request.post('/admin-api/system/job/add', data)
export const updateJob = (data) => request.put('/admin-api/system/job/update', data)
export const deleteJobs = (ids) => request.delete('/admin-api/system/job/remove', { params: { ids: ids.join(',') } })
export const changeJobStatus = (id, status) =>
  request.put(`/admin-api/system/job/${id}/status`, null, { params: { status } })
export const runJob = (id) => request.put(`/admin-api/system/job/${id}/run`)

export const pageJobLogs = (params) => request.get('/admin-api/system/joblog/page', { params })
export const deleteJobLogs = (ids) =>
  request.delete('/admin-api/system/joblog/remove', { params: { ids: ids.join(',') } })
export const clearJobLogs = () => request.delete('/admin-api/system/joblog/clear')
export const clearJobLogsByJob = (jobId) => request.delete(`/admin-api/system/joblog/clear/${jobId}`)

// ---------------------- 参数配置 ----------------------
export const pageConfigs = (params) => request.get('/admin-api/system/config/page', { params })
export const getConfig = (id) => request.get(`/admin-api/system/config/${id}`)
export const createConfig = (data) => request.post('/admin-api/system/config/add', data)
export const updateConfig = (data) => request.put('/admin-api/system/config/update', data)
export const deleteConfigs = (ids) =>
  request.delete('/admin-api/system/config/remove', { params: { ids: ids.join(',') } })
export const refreshConfigCache = () => request.delete('/admin-api/system/config/refresh')

// ---------------------- 系统设置（系统名称 / Logo / 默认偏好）----------------------
export const getSystemSetting = () => request.get('/admin-api/system/setting')
export const updateSystemSetting = (data) => request.put('/admin-api/system/setting', data)
/** 公开系统设置（免登录）：登录页展示系统名称与 Logo */
export const getPublicSystemSetting = () => request.get('/admin-api/system/setting/public')

// ---------------------- 部门 ----------------------
export const getDeptTree = (params) => request.get('/admin-api/system/dept/tree', { params })
export const getDeptOptions = () => request.get('/admin-api/system/dept/options')
export const createDept = (data) => request.post('/admin-api/system/dept/add', data)
export const updateDept = (data) => request.put('/admin-api/system/dept/update', data)
export const deleteDept = (id) => request.delete(`/admin-api/system/dept/${id}`)

// ---------------------- 在线用户 ----------------------
export const pageOnlineUsers = (params) => request.get('/admin-api/system/online/page', { params })
export const forceLogoutOnline = (tokenId) => request.delete(`/admin-api/system/online/${encodeURIComponent(tokenId)}`)
export const forceLogoutOnlineBatch = (tokenIds) =>
  request.delete('/admin-api/system/online/batch', { params: { tokenIds: tokenIds.join(',') } })
