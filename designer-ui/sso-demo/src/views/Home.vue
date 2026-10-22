<script setup>
import { onMounted, ref } from 'vue'
import { getUserInfo } from '@/api/auth'
import { getToken, clearToken } from '@/utils/auth'
import { adminLoginUrl } from '@/settings'

const token = ref(getToken())
const user = ref(null)
const error = ref('')
const loading = ref(false)

async function loadUser() {
  if (!token.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await getUserInfo()
    user.value = res.data
  } catch (e) {
    error.value = e?.message || '获取用户信息失败（token 可能已失效）'
  } finally {
    loading.value = false
  }
}

function logout() {
  clearToken()
  token.value = ''
  user.value = null
  error.value = ''
}

/** 未登录：跳后台登录页；登录成功后由后台签发票据回跳本应用 /sso 入口 */
function goAdminLogin() {
  const back = `${window.location.origin}/sso`
  window.location.href = `${adminLoginUrl}?redirect=${encodeURIComponent(back)}`
}

onMounted(loadUser)
</script>

<template>
  <div class="card">
    <h2>SSO 客户端 Demo</h2>

    <template v-if="token">
      <p class="ok">已通过 SSO 登录</p>
      <p class="muted">token：<code>{{ token.slice(0, 12) }}…</code></p>

      <div v-if="loading">加载用户信息…</div>
      <ul v-else-if="user">
        <li>用户：{{ user.nickname || user.username }}（{{ user.username }}）</li>
        <li>角色：{{ (user.roles || []).join('、') || '无' }}</li>
        <li>权限：{{ (user.perms || []).length }} 项{{ user.superAdmin ? '（超级管理员 *）' : '' }}</li>
      </ul>
      <p v-if="error" class="err">{{ error }}</p>

      <div class="actions">
        <button @click="loadUser">刷新用户信息</button>
        <button @click="logout">退出（清本地 token）</button>
      </div>
    </template>

    <template v-else>
      <p class="muted">当前未登录。</p>
      <ol class="steps">
        <li>点击下方按钮跳转到后台登录页（redirect 指向本应用 <code>/sso</code>）。</li>
        <li>登录成功后，后台签发一次性票据并跳回本应用（<code>/sso?ticket=…</code>）。</li>
        <li>本应用用票据换取 token，完成免二次登录。</li>
      </ol>
      <div class="actions">
        <button class="primary" @click="goAdminLogin">去后台登录（SSO）</button>
      </div>
    </template>
  </div>
</template>
