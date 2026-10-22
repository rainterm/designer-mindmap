import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo, getRouters } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/utils/auth'
import { useAppStore } from '@/store/app'
import { useNoticeStore } from '@/store/notice'

/**
 * 用户状态：令牌、用户信息、角色与权限
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null,
    roles: [],
    perms: [],
    /** 后端返回的动态菜单路由树（侧边栏 / 面包屑数据源） */
    routers: [],
    /** 动态路由是否已注册 */
    routesLoaded: false
  }),

  getters: {
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || '未登录',
    avatar: (state) => state.userInfo?.avatar || '',
  },

  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      setToken(res.data.token)
      return res.data
    },

    async loadUserInfo() {
      const res = await getUserInfo()
      this.userInfo = res.data
      this.roles = res.data.roles || []
      this.perms = res.data.perms || []
      return this.userInfo
    },

    async loadRouters() {
      const res = await getRouters()
      this.routers = res.data || []
      this.routesLoaded = true
      return this.routers
    },

    /** 权限判断：超级管理员拥有 * 通配权限 */
    hasPerm(perm) {
      if (!perm) return true
      if (this.perms.includes('*')) return true
      return this.perms.includes(perm)
    },

    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 令牌可能已失效，忽略异常继续清理本地状态
      }
      this.reset()
    },

    reset() {
      useNoticeStore().stopWatch()
      useAppStore().resetLayout()
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.perms = []
      // 注意：不要在这里清空 routers。登出时布局仍在挂载中，清空会让「双列」布局的
      // 侧栏因菜单为空而收起（showSider 变 false），从而切换 layout 分支、导致当前页面
      // 被重挂载并重新发起请求（令牌已清除 → 401「登录状态已过期」）。
      // 菜单数据在下次登录时由 loadRouters 重新拉取。
      this.routesLoaded = false
      clearToken()
    }
  }
})