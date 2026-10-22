import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getNoticeInbox,
  getNoticeUnreadCount,
  markNoticeRead,
  markNoticeReadAll
} from '@/modules/system/api/system'
import { getToken } from '@/utils/auth'
import { notification } from '@/utils/feedback'
import { resolveNoticeLink } from '@/utils/noticeLink'

/**
 * 顶部消息中心：未读数 + 收件箱 + SSE 实时推送
 */
export const useNoticeStore = defineStore('notice', () => {
  const unreadCount = ref(0)
  const inboxLoading = ref(false)
  const inboxRows = ref([])
  const inboxTotal = ref(0)
  const inboxQuery = ref({ pageNum: 1, pageSize: 10 })
  /** 通知条点击：携带消息体，MessageInbox 打开详情弹窗 */
  const detailRequest = ref(null)
  /** 跳转菜单请求：MessageInbox / 布局侧消费 */
  const navigateRequest = ref(null)

  let eventSource = null
  let reconnectTimer = null
  let visibilityHandler = null
  let sseErrorCount = 0
  let drawerOpenGetter = () => false

  function setDrawerOpenGetter(fn) {
    drawerOpenGetter = typeof fn === 'function' ? fn : () => false
  }

  async function loadUnread() {
    try {
      const res = await getNoticeUnreadCount()
      unreadCount.value = Number(res.data || 0)
    } catch {
      unreadCount.value = 0
    }
  }

  async function loadInbox(reset = false) {
    if (reset) {
      inboxQuery.value.pageNum = 1
    }
    inboxLoading.value = true
    try {
      const res = await getNoticeInbox({
        pageNum: inboxQuery.value.pageNum,
        pageSize: inboxQuery.value.pageSize
      })
      inboxRows.value = res.data?.records || []
      inboxTotal.value = res.data?.total || 0
    } finally {
      inboxLoading.value = false
    }
  }

  async function markRead(id) {
    await markNoticeRead(id)
    const row = inboxRows.value.find((r) => r.id === id)
    if (row && !row.readFlag) {
      row.readFlag = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } else {
      await loadUnread()
    }
  }

  async function markAllRead() {
    await markNoticeReadAll()
    inboxRows.value.forEach((r) => {
      r.readFlag = true
    })
    unreadCount.value = 0
  }

  function requestOpenDetail(item) {
    if (!item || !item.id) return
    detailRequest.value = {
      id: item.id,
      title: item.title || '新消息',
      content: item.content || '',
      createTime: item.createTime || '',
      link: resolveNoticeLink(item),
      readFlag: false,
      _t: Date.now()
    }
  }

  function requestNavigate(item) {
    const link = resolveNoticeLink(item)
    if (!link) return false
    navigateRequest.value = { path: link, _t: Date.now() }
    return true
  }

  function handleNoticeEvent(raw) {
    let data = raw
    if (typeof raw === 'string') {
      try {
        data = JSON.parse(raw)
      } catch {
        return
      }
    }
    if (!data || !data.id) return

    unreadCount.value += 1
    const link = resolveNoticeLink(data)
    const n = notification.create({
      type: 'info',
      title: data.title || '新消息',
      content: truncate(data.content || '你有一条新的站内消息', 80),
      duration: 6000,
      keepAliveOnHover: true,
      onClick: () => {
        n.destroy()
        if (link) {
          requestNavigate(data)
          if (!data.readFlag) {
            markRead(data.id).catch(() => {})
          }
        } else {
          requestOpenDetail(data)
        }
      }
    })

    if (drawerOpenGetter()) {
      loadInbox(true)
    }
  }

  function truncate(text, max) {
    const s = String(text).replace(/\s+/g, ' ').trim()
    return s.length > max ? `${s.slice(0, max)}…` : s
  }

  function connectSse() {
    const token = getToken()
    if (!token || typeof EventSource === 'undefined') {
      return
    }
    disconnectSse()
    sseErrorCount = 0
    const url = `/admin-api/system/notice/sse?Authorization=${encodeURIComponent(token)}`
    eventSource = new EventSource(url)

    eventSource.addEventListener('notice', (e) => {
      sseErrorCount = 0
      handleNoticeEvent(e.data)
    })

    eventSource.addEventListener('ping', () => {
      // 心跳，保持连接
    })

    eventSource.addEventListener('connected', () => {
      // 订阅成功后校正未读数
      sseErrorCount = 0
      loadUnread()
    })

    eventSource.onerror = () => {
      sseErrorCount += 1
      // 浏览器会自动重连；若已 CLOSED 则稍后手动重连
      if (eventSource && eventSource.readyState === EventSource.CLOSED) {
        disconnectSse()
        scheduleReconnect()
        return
      }
      // 服务端会因登出 / 被踢 / token 过期主动断开，浏览器随后每 3 秒重试一次。
      // 连续失败时用普通请求确认会话：401 由 axios 拦截器清理 token 并跳登录，
      // 同时停掉 EventSource，避免无休止重试。
      if (sseErrorCount === 2) {
        verifySession()
      }
    }
  }

  async function verifySession() {
    try {
      await getNoticeUnreadCount()
      sseErrorCount = 0
    } catch {
      if (!getToken()) {
        disconnectSse()
      }
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) return
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null
      if (getToken()) {
        connectSse()
      }
    }, 5000)
  }

  function disconnectSse() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }

  /** 布局挂载：拉未读 + 开 SSE */
  function startWatch() {
    loadUnread()
    connectSse()
    // 标签页切回前台时补一次：SSE 长连接可能已被服务端/浏览器回收
    if (!visibilityHandler && typeof document !== 'undefined') {
      visibilityHandler = () => {
        if (document.visibilityState !== 'visible') return
        loadUnread()
        if (!eventSource || eventSource.readyState === EventSource.CLOSED) {
          connectSse()
        }
      }
      document.addEventListener('visibilitychange', visibilityHandler)
    }
  }

  function stopWatch() {
    disconnectSse()
    if (visibilityHandler) {
      document.removeEventListener('visibilitychange', visibilityHandler)
      visibilityHandler = null
    }
  }

  return {
    unreadCount,
    inboxLoading,
    inboxRows,
    inboxTotal,
    inboxQuery,
    detailRequest,
    navigateRequest,
    loadUnread,
    loadInbox,
    markRead,
    markAllRead,
    requestOpenDetail,
    requestNavigate,
    setDrawerOpenGetter,
    startWatch,
    stopWatch
  }
})
