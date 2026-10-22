<template>
  <div class="tags-view">
    <n-scrollbar x-scrollable>
      <div class="tags-wrap">
        <button
          v-for="tag in appStore.visitedViews"
          :key="tag.path"
          type="button"
          class="view-tab"
          :class="{ 'is-active': tag.path === route.path, 'is-affix': tag.affix }"
          @click="router.push(tag.path)"
          @click.middle.prevent="closeTag(tag)"
        >
          <n-icon class="view-tab-icon" :component="resolveIcon(tag.icon)" :size="14" />
          <span class="view-tab-title">{{ tag.title }}</span>
          <span
            v-if="tag.affix"
            class="view-tab-action is-pin"
            title="固定"
          >
            <n-icon :component="LockClosedOutline" :size="12" />
          </span>
          <span
            v-else
            class="view-tab-action"
            title="关闭"
            @click.stop="closeTag(tag)"
          >
            <n-icon :component="CloseOutline" :size="12" />
          </span>
        </button>
      </div>
    </n-scrollbar>

    <n-dropdown :options="toolOptions" trigger="click" @select="handleTool">
      <n-button class="tags-tool" quaternary >
        <template #icon><n-icon :component="EllipsisHorizontalOutline" /></template>
      </n-button>
    </n-dropdown>
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CloseOutline, EllipsisHorizontalOutline, LockClosedOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/app'
import { resolveIcon } from '@/utils/icon'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const toolOptions = [
  { label: '关闭其它页签', key: 'others' },
  { label: '关闭全部页签', key: 'all' }
]

function syncTag() {
  if (route.meta?.public || route.name === 'Login') return
  appStore.addVisitedView(route)
}

/** 固定页签优先，否则回退到最后一个已打开页签（关全部时用） */
function fallbackPath() {
  const affixed = appStore.visitedViews.find((v) => v.affix)
  if (affixed) return affixed.path
  const last = appStore.visitedViews[appStore.visitedViews.length - 1]
  return last?.path || '/'
}

/** 关闭当前页签后：优先左侧相邻，其次右侧，再回退固定/首页 */
function neighborPath(closedPath) {
  const views = appStore.visitedViews
  const idx = views.findIndex((v) => v.path === closedPath)
  if (idx < 0) return fallbackPath()
  const prev = views[idx - 1]
  const next = views[idx + 1]
  return prev?.path || next?.path || fallbackPath()
}

onMounted(() => {
  appStore.initAffixViews(router.getRoutes())
  syncTag()
})

watch(() => route.path, syncTag)

function closeTag(tag) {
  if (tag.affix) return
  const wasActive = tag.path === route.path
  const to = wasActive ? neighborPath(tag.path) : null
  appStore.removeVisitedView(tag.path)
  if (wasActive) {
    router.push(to)
  }
}

function handleTool(key) {
  if (key === 'others') {
    appStore.removeOtherViews(route.path)
  } else if (key === 'all') {
    appStore.clearViews()
    router.push(fallbackPath())
  }
}
</script>

<style scoped>
.tags-view {
  --tag-active-bg: color-mix(in srgb, var(--color-primary) 10%, var(--color-surface));
  --tag-curve: 10px;

  display: flex;
  align-items: stretch;
  gap: 4px;
  height: var(--layout-tags-height, 36px);
  padding: 0 10px 0 6px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
}

.tags-wrap {
  display: flex;
  align-items: flex-end;
  gap: 0;
  height: 100%;
  white-space: nowrap;
  padding: 5px 8px 0;
}

.tags-view :deep(.n-scrollbar),
.tags-view :deep(.n-scrollbar-container) {
  height: 100%;
  flex: 1;
  min-width: 0;
}

.tags-view :deep(.n-scrollbar-container) {
  scrollbar-width: none;
}

.tags-view :deep(.n-scrollbar-container::-webkit-scrollbar) {
  width: 0;
  height: 0;
  display: none;
}

.tags-view :deep(.n-scrollbar-content) {
  display: flex;
  align-items: stretch;
  height: 100%;
}

.tags-view :deep(.n-scrollbar > .n-scrollbar-rail--horizontal) {
  height: 2px !important;
  bottom: 0;
  right: 0;
  left: 0;
}

.tags-view :deep(.n-scrollbar > .n-scrollbar-rail--horizontal > .n-scrollbar-rail__scrollbar) {
  height: 2px !important;
  border-radius: 0;
  background-color: color-mix(in srgb, var(--color-primary) 40%, transparent);
}

.view-tab {
  position: relative;
  z-index: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  margin-bottom: -1px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px 6px 0 0;
  background: transparent;
  color: var(--color-text);
  font: inherit;
  font-size: 13px;
  line-height: 1;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition:
    color 0.15s ease,
    background-color 0.15s ease;
}

/* 未激活页签之间的分隔线 */
.view-tab:not(.is-active):not(:hover) + .view-tab:not(.is-active):not(:hover)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 1px;
  height: 14px;
  transform: translateY(-50%);
  background: var(--color-border);
}

.view-tab:hover:not(.is-active) {
  color: var(--color-text);
  background: color-mix(in srgb, var(--color-body) 65%, transparent);
}

.view-tab.is-active {
  z-index: 2;
  color: var(--color-primary);
  font-weight: 600;
  background: var(--tag-active-bg);
}

/* 底部凹角：与底边线平滑衔接（Chrome 标签效果） */
.view-tab.is-active::before,
.view-tab.is-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  width: var(--tag-curve);
  height: var(--tag-curve);
  background: transparent;
  pointer-events: none;
}

.view-tab.is-active::before {
  left: calc(var(--tag-curve) * -1);
  border-bottom-right-radius: var(--tag-curve);
  box-shadow: 4px 4px 0 4px var(--tag-active-bg);
}

.view-tab.is-active::after {
  right: calc(var(--tag-curve) * -1);
  border-bottom-left-radius: var(--tag-curve);
  box-shadow: -4px 4px 0 4px var(--tag-active-bg);
}

.view-tab-icon {
  flex-shrink: 0;
  opacity: 0.72;
}

.view-tab.is-active .view-tab-icon {
  opacity: 1;
  color: var(--color-primary);
}

.view-tab-title {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.view-tab-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-left: 2px;
  border-radius: 4px;
  color: inherit;
  opacity: 0.45;
  transition:
    opacity 0.12s ease,
    background-color 0.12s ease,
    color 0.12s ease;
}

.view-tab-action.is-pin {
  opacity: 0.4;
  pointer-events: none;
}

.view-tab:hover .view-tab-action,
.view-tab.is-active .view-tab-action {
  opacity: 0.7;
}

.view-tab-action:not(.is-pin):hover {
  opacity: 1 !important;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.tags-tool {
  align-self: center;
  flex-shrink: 0;
  color: var(--color-text-muted) !important;
}
</style>
