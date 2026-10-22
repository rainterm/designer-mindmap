<template>
  <div class="page-container">
    <n-card :bordered="false">
      <n-space vertical :size="16">
        <div>
          <h2 class="title">Demo 示例模块</h2>
          <p class="desc">
            这是二次开发模板页。复制 <code>src/modules/demo</code> 为新业务模块即可，菜单
            <code>component</code> 填 <code>demo/index</code>（对应本文件路径）。
          </p>
        </div>
        <n-space>
          <n-button type="primary" :loading="loading" @click="onPing">
            调用 /admin-api/demo/ping
          </n-button>
        </n-space>
        <n-code v-if="result" :code="result" language="json" word-wrap />
      </n-space>
    </n-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { pingDemo } from '@/modules/demo/api'
import { message } from '@/utils/feedback'

const loading = ref(false)
const result = ref('')

async function onPing() {
  loading.value = true
  try {
    const data = await pingDemo()
    result.value = JSON.stringify(data, null, 2)
    message.success('Demo 模块响应正常')
  } catch (e) {
    message.error(e?.message || '请求失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 内容卡片铺满一屏：容器纵向 flex，卡片占满剩余高度 */
.page-container {
  display: flex;
  flex-direction: column;
}
.page-container > .n-card {
  flex: 1 1 auto;
}

.title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}
.desc {
  margin: 0;
  color: var(--n-text-color-3);
  line-height: 1.6;
}
code {
  padding: 0 4px;
  border-radius: var(--radius);
  background: rgba(128, 128, 128, 0.12);
}
</style>
