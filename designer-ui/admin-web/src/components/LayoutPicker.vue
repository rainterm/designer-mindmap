<template>
  <div class="layout-grid">
    <button
      v-for="opt in LAYOUT_OPTIONS"
      :key="opt.value"
      type="button"
      class="layout-card"
      :class="{ 'is-active': modelValue === opt.value }"
      @click="emit('update:modelValue', opt.value)"
    >
      <div class="layout-card__preview" :data-mode="opt.value">
        <span class="lp-sider" />
        <span class="lp-sider2" />
        <span class="lp-header" />
        <span class="lp-body">
          <i /><i /><i />
        </span>
      </div>
      <div class="layout-card__label">
        <span>{{ opt.label }}</span>
        <n-tooltip :show-arrow="false" placement="top">
          <template #trigger>
            <n-icon class="layout-card__tip" :component="HelpCircleOutline" :size="14" />
          </template>
          {{ opt.tip }}
        </n-tooltip>
      </div>
    </button>
  </div>
</template>

<script setup>
import { HelpCircleOutline } from '@vicons/ionicons5'
import { LAYOUT_OPTIONS } from '@/constants/layout'

/**
 * 布局选择器：卡片缩略图 + 悬停提示。
 *
 * <p>「偏好设置」面板与「系统设置」的默认布局共用同一个组件，
 * 避免两处的缩略图几何各写一套、改一处漏一处。</p>
 */
defineProps({
  /** 当前选中的布局模式（见 constants/layout 的 LAYOUT_MODES） */
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.layout-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  /* 放进 n-form-item 时，容器是 display:flex 且 flex-grow 为 0 的 blank，
     grid 会缩成内容宽度（三列各 66px）。这里主动撑满：
     块级父容器（偏好设置抽屉）看 width，flex 父容器（表单）看 flex-grow。 */
  width: 100%;
  flex: 1 1 auto;
  min-width: 0;
}

.layout-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.layout-card__preview {
  position: relative;
  height: 64px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.layout-card:hover .layout-card__preview {
  border-color: color-mix(in srgb, var(--color-primary) 45%, var(--color-border));
}

.layout-card.is-active .layout-card__preview {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--color-primary) 25%, transparent);
}

.layout-card__label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.layout-card.is-active .layout-card__label {
  color: var(--color-primary);
  font-weight: 600;
}

.layout-card__tip {
  color: var(--color-text-muted);
  opacity: 0.85;
}

/* —— 布局缩略图几何 —— */
.lp-sider,
.lp-sider2,
.lp-header,
.lp-body {
  position: absolute;
  display: block;
  border-radius: 2px;
}

.lp-sider {
  display: none;
  background: var(--color-primary);
}

.lp-sider2 {
  display: none;
  background: color-mix(in srgb, var(--color-primary) 35%, #dfe5e1);
}

.lp-header {
  display: none;
  background: var(--color-primary);
}

.lp-body {
  inset: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0;
}

.lp-body i {
  display: block;
  height: 8px;
  border-radius: 2px;
  background: #d7ddd9;
}

.lp-body i:nth-child(2) {
  width: 72%;
}

.lp-body i:nth-child(3) {
  width: 88%;
  flex: 1;
  min-height: 14px;
}

/* 垂直：左侧菜单 */
.layout-card__preview[data-mode='vertical'] .lp-sider {
  display: block;
  left: 6px;
  top: 6px;
  bottom: 6px;
  width: 16px;
}
.layout-card__preview[data-mode='vertical'] .lp-header {
  display: block;
  left: 26px;
  right: 6px;
  top: 6px;
  height: 10px;
  background: #cfd6d2;
}
.layout-card__preview[data-mode='vertical'] .lp-body {
  left: 26px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 双列 */
.layout-card__preview[data-mode='dual-column'] .lp-sider {
  display: block;
  left: 5px;
  top: 6px;
  bottom: 6px;
  width: 10px;
}
.layout-card__preview[data-mode='dual-column'] .lp-sider2 {
  display: block;
  left: 17px;
  top: 6px;
  bottom: 6px;
  width: 12px;
}
.layout-card__preview[data-mode='dual-column'] .lp-header {
  display: block;
  left: 33px;
  right: 6px;
  top: 6px;
  height: 10px;
  background: #cfd6d2;
}
.layout-card__preview[data-mode='dual-column'] .lp-body {
  left: 33px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 水平 */
.layout-card__preview[data-mode='horizontal'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 14px;
}
.layout-card__preview[data-mode='horizontal'] .lp-body {
  left: 6px;
  right: 6px;
  top: 24px;
  bottom: 6px;
}

/* 侧边导航（细图标栏） */
.layout-card__preview[data-mode='side-nav'] .lp-sider {
  display: block;
  left: 6px;
  top: 20px;
  bottom: 6px;
  width: 10px;
}
.layout-card__preview[data-mode='side-nav'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 10px;
}
.layout-card__preview[data-mode='side-nav'] .lp-body {
  left: 20px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 混合垂直 */
.layout-card__preview[data-mode='mixed-vertical'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 10px;
}
.layout-card__preview[data-mode='mixed-vertical'] .lp-sider {
  display: block;
  left: 6px;
  top: 20px;
  bottom: 6px;
  width: 16px;
  background: #cfd6d2;
}
.layout-card__preview[data-mode='mixed-vertical'] .lp-body {
  left: 26px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 混合双列 */
.layout-card__preview[data-mode='mixed-dual'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 10px;
}
.layout-card__preview[data-mode='mixed-dual'] .lp-sider {
  display: block;
  left: 6px;
  top: 20px;
  bottom: 6px;
  width: 10px;
}
.layout-card__preview[data-mode='mixed-dual'] .lp-sider2 {
  display: block;
  left: 18px;
  top: 20px;
  bottom: 6px;
  width: 12px;
}
.layout-card__preview[data-mode='mixed-dual'] .lp-body {
  left: 34px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 内容全屏 */
.layout-card__preview[data-mode='content-full'] .lp-body {
  inset: 8px;
}
</style>
