<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { exchangeSsoTicket } from '@/api/auth'
import { setToken } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const status = ref('正在通过 SSO 登录…')
const failed = ref('')

onMounted(async () => {
  const ticket = route.query.ticket
  if (!ticket) {
    status.value = ''
    failed.value = '缺少登录票据（?ticket=），请从后台跳转进入'
    return
  }
  try {
    const res = await exchangeSsoTicket(String(ticket))
    setToken(res.data.token)
    const redirect = String(route.query.redirect || '/')
    status.value = '登录成功，正在跳转…'
    router.replace(redirect.startsWith('/') ? redirect : '/')
  } catch (e) {
    status.value = ''
    failed.value = e?.message || '票据换取失败（可能已过期或已使用）'
  }
})
</script>

<template>
  <div class="card">
    <h2>SSO 登录</h2>
    <p v-if="status" class="muted">{{ status }}</p>
    <p v-if="failed" class="err">{{ failed }}</p>
    <div v-if="failed" class="actions">
      <RouterLink to="/"><button>返回首页</button></RouterLink>
    </div>
  </div>
</template>
