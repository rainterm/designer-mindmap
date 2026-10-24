import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { setUnauthorizedHandler } from '@/api/request'
import { goAdminSsoLogin } from '@/utils/sso'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      redirect: (to) => ({ path: '/sso', query: { redirect: to.query.redirect || '/' } })
    },
    {
      path: '/sso',
      name: 'Sso',
      component: () => import('@/views/Sso.vue'),
      meta: { title: '单点登录', public: true }
    },
    {
      path: '/sso-error',
      name: 'SsoError',
      component: () => import('@/views/SsoError.vue'),
      meta: { title: '单点登录失败', public: true }
    },
    {
      path: '/',
      name: 'ProjectList',
      component: () => import('@/views/ProjectList.vue'),
      meta: { title: '导图项目' }
    },
    {
      path: '/edit',
      name: 'ProjectEdit',
      component: () => import('@/views/ProjectEdit.vue'),
      meta: { title: '导图编辑', perm: 'mindmap:project:edit' }
    }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()

  if (to.meta?.public) {
    return true
  }

  if (!userStore.token) {
    return { path: '/sso', query: { redirect: to.fullPath } }
  }

  if (!userStore.userInfo) {
    try {
      await userStore.loadUserInfo()
    } catch {
      userStore.reset()
      return { path: '/sso', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta?.perm && !userStore.hasPerm(to.meta.perm)) {
    return '/'
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta?.title
  document.title = title ? `${title} · 思维导图` : '思维导图'
})

setUnauthorizedHandler(() => {
  const userStore = useUserStore()
  userStore.reset()
  const current = router.currentRoute.value
  if (current.meta?.public) return
  goAdminSsoLogin(current.fullPath)
})

export default router
