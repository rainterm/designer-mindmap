<template>
  <div class="sso-error-page">
    <n-result status="error" title="单点登录失败" :description="description">
      <template #footer>
        <n-space justify="center">
          <n-button v-if="retryUrl" type="primary" @click="retry">
            重试回跳
          </n-button>
          <n-button @click="goHome">
            进入管理后台
          </n-button>
          <n-button quaternary @click="goLogin">
            重新登录
          </n-button>
        </n-space>
      </template>
    </n-result>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { continueSsoReturn } from '@/utils/sso'

const route = useRoute()
const router = useRouter()

const description = computed(
  () =>
    String(route.query.msg || '') ||
    '无法完成跨应用登录回跳。请检查「系统管理 → SSO客户端」中的 Origin 白名单，或联系管理员。'
)

const retryUrl = computed(() => {
  const r = route.query.redirect
  return r ? String(r) : ''
})

async function retry() {
  if (!retryUrl.value) return
  try {
    if (await continueSsoReturn(retryUrl.value)) return
    router.replace({
      path: '/sso-error',
      query: { msg: '回跳地址无效或不在白名单中', redirect: retryUrl.value }
    })
  } catch (e) {
    router.replace({
      path: '/sso-error',
      query: {
        msg: e?.message || '重试失败，请检查 SSO 客户端配置',
        redirect: retryUrl.value
      }
    })
  }
}

function goHome() {
  router.replace('/')
}

function goLogin() {
  router.replace({ path: '/login', query: retryUrl.value ? { redirect: retryUrl.value } : {} })
}
</script>

<style scoped>
.sso-error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 48px 16px;
  box-sizing: border-box;
  background-color: var(--color-body);
}
</style>
