import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'Home', component: () => import('@/views/Home.vue') },
    // 接入路径须与后台「SSO 客户端」的 entry_path 一致（默认 /sso）
    { path: '/sso', name: 'Sso', component: () => import('@/views/Sso.vue') },
    { path: '/sso-error', name: 'SsoError', component: () => import('@/views/SsoError.vue') }
  ]
})

export default router
