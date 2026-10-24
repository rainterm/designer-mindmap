<template>
  <div class="sso-error-page">
    <n-result status="error" title="单点登录失败" :description="description">
      <template #footer>
        <n-space justify="center">
          <n-button type="primary" @click="retry">
            重新接入登录
          </n-button>
          <n-button quaternary @click="openAdmin">
            打开管理后台
          </n-button>
        </n-space>
      </template>
    </n-result>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { adminWebUrl } from '@/settings'
import { goAdminSsoLogin, safeRedirect } from '@/utils/sso'

const route = useRoute()

const description = computed(
  () =>
    String(route.query.msg || '') ||
    '登录票据无效、已过期或校验失败。请重新从管理后台进入，或再次发起单点登录。'
)

const redirect = computed(() => safeRedirect(route.query.redirect))

function retry() {
  goAdminSsoLogin(redirect.value)
}

function openAdmin() {
  window.open(adminWebUrl, '_blank', 'noopener')
}
</script>

<style scoped>
.sso-error-page {
  height: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  box-sizing: border-box;
}
</style>
