<template>
  <transition name="page-loading-fade">
    <div v-if="loading" class="page-loading-mask">
      <div class="page-loading-box">
        <div class="page-loading-spinner"></div>
        <div class="page-loading-text">加载中…</div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { pageLoading as loading } from '@/utils/pageLoading'
</script>

<style scoped>
.page-loading-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--color-body) 75%, transparent);
  backdrop-filter: blur(2px);
}

.page-loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.page-loading-spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 3px solid color-mix(in srgb, var(--color-primary) 18%, transparent);
  border-top-color: var(--color-primary);
  animation: page-loading-spin 0.8s linear infinite;
}

.page-loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  letter-spacing: 1px;
}

@keyframes page-loading-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 遮罩淡入淡出 */
.page-loading-fade-enter-active,
.page-loading-fade-leave-active {
  transition: opacity 0.25s ease;
}

.page-loading-fade-enter-from,
.page-loading-fade-leave-to {
  opacity: 0;
}
</style>
