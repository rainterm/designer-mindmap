<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
const msg = String(route.query.msg || '单点登录失败')
</script>

<template>
  <div class="card">
    <h2>单点登录失败</h2>
    <p class="err">{{ msg }}</p>
    <div class="actions">
      <RouterLink to="/"><button>返回首页</button></RouterLink>
    </div>
  </div>
</template>
