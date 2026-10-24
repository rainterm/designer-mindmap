<template>
  <div class="sso-page">
    <n-spin size="large" :show="true">
      <div class="sso-tip">正在接入登录状态…</div>
    </n-spin>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { goAdminSsoLogin, safeRedirect } from '@/utils/sso'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function fail(msg) {
  userStore.reset()
  router.replace({
    path: '/sso-error',
    query: {
      msg: msg || '单点登录失败',
      redirect: safeRedirect(route.query.redirect)
    }
  })
}

onMounted(async () => {
  const redirect = safeRedirect(route.query.redirect)
  const ticket = route.query.ticket ? String(route.query.ticket) : ''

  if (ticket) {
    try {
      await userStore.exchangeSso(ticket)
      await userStore.loadUserInfo()
      router.replace(redirect)
      return
    } catch (e) {
      fail(e?.message || '登录票据无效或已过期')
      return
    }
  }

  if (userStore.token) {
    try {
      if (!userStore.userInfo) await userStore.loadUserInfo()
      router.replace(redirect)
      return
    } catch (e) {
      fail(e?.message || '登录状态已失效')
      return
    }
  }

  // 无票据且未登录：跳转管理后台统一登录（非校验失败）
  goAdminSsoLogin(redirect)
})
</script>

<style scoped>
.sso-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sso-tip {
  padding: 48px;
  color: var(--color-text-muted);
}
</style>
