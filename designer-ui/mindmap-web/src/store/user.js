import { defineStore } from 'pinia'
import { logout as logoutApi, getUserInfo, exchangeSsoTicket } from '@/api/auth'
import { getToken, setToken, clearToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null,
    roles: [],
    perms: []
  }),

  getters: {
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || '未登录',
    avatar: (state) => state.userInfo?.avatar || ''
  },

  actions: {
    applyLogin(data) {
      this.token = data.token
      setToken(data.token)
    },

    async exchangeSso(ticket) {
      const res = await exchangeSsoTicket(ticket)
      this.applyLogin(res.data)
      return res.data
    },

    async loadUserInfo() {
      const res = await getUserInfo()
      this.userInfo = res.data
      this.roles = res.data.roles || []
      this.perms = res.data.perms || []
      return this.userInfo
    },

    hasPerm(perm) {
      if (!perm) return true
      if (this.perms.includes('*')) return true
      return this.perms.includes(perm)
    },

    async logout() {
      try {
        await logoutApi()
      } catch {
        // ignore
      }
      this.reset()
    },

    reset() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.perms = []
      clearToken()
    }
  }
})
