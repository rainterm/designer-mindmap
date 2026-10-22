<template>
  <div class="error-page">
    <n-result status="404" title="404 页面不存在" description="抱歉，你访问的页面走丢了">
      <template #footer>
        <n-space justify="center">
          <n-button @click="goBack">
            返回上一页
          </n-button>
          <n-button type="primary" @click="$router.push('/')">
            回到首页
          </n-button>
        </n-space>
      </template>
    </n-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--layout-nav-height, 44px) - var(--layout-tags-height, 36px));
  padding: 48px 16px;
  box-sizing: border-box;
  background-color: var(--color-body);
}
</style>
