import request from './request'

/** 服务监控（CPU / 内存 / JVM / 磁盘） */
export const getServerMonitor = () => request.get('/admin-api/system/monitor/server')
