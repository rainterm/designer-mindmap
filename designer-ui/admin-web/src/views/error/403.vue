<template>
  <div class="error-page">
    <n-result status="403" title="403 无访问权限" :description="description">
      <template #footer>
        <n-space justify="center">
          <n-button @click="$router.back()">
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
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const description = computed(
  () => route.query.perm || '当前账号没有访问该页面的权限，请联系管理员分配。'
)
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
