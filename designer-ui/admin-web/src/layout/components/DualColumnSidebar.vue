<template>
  <div class="dual-sidebar" :class="{ 'is-solo': !showSubs }">
    <div class="dual-sidebar__icons">
      <div v-if="showLogo" class="dual-sidebar__logo">
        <BrandLogo
          class="dual-sidebar__logo-img"
          :src="settingStore.logoUrl"
          :radius="settingStore.logoRadius"
        />
      </div>
      <button
        v-for="item in topOptions"
        :key="item.key"
        type="button"
        class="dual-icon-item"
        :class="{ 'is-active': item.key === activeTopKey }"
        :title="typeof item.label === 'string' ? item.label : ''"
        @click="selectTop(item)"
      >
        <span class="dual-icon-item__icon">
          <IconSlot v-if="item.icon" :fn="item.icon" />
          <!-- 一级图标栏也要能看到待办数：这里没有文字标题，只能把徽标压在图标右上角 -->
          <span v-if="item.extra" class="dual-icon-item__badge">
            <IconSlot :fn="item.extra" />
          </span>
        </span>
        <span class="dual-icon-item__label">{{ item.label }}</span>
      </button>
    </div>
    <div v-if="showSubs && subOptions.length" class="dual-sidebar__subs">
      <div class="dual-sidebar__title">{{ activeTopLabel }}</div>
      <n-scrollbar class="dual-sidebar__menu-scroll">
        <n-menu
          v-model:expanded-keys="expandedKeys"
          :value="activeKey"
          :options="subOptions"
          :indent="18"
          :root-indent="12"
          accordion
          @update:value="handleSelect"
        />
      </n-scrollbar>
    </div>
  </div>
</template>

<script setup>
import { computed, defineComponent, ref, watch } from 'vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { useSidebarMenu } from '@/composables/useSidebarMenu'
import { useSettingStore } from '@/store/setting'

const settingStore = useSettingStore()

defineProps({
  showLogo: { type: Boolean, default: false },
  /** 是否展示右侧二级菜单列；侧边导航只保留一级栏 */
  showSubs: { type: Boolean, default: true }
})

/** n-menu 的 icon 是 render 函数，这里直接调用 */
const IconSlot = defineComponent({
  props: { fn: { type: Function, required: true } },
  setup(props) {
    return () => props.fn?.()
  }
})

const { route, activeKey, expandedKeys, menuOptions, handleSelect, findTopLevel } =
  useSidebarMenu({ collapsed: computed(() => true) })

const activeTopKey = ref('')

const topOptions = computed(() =>
  (menuOptions.value || []).map((opt) => ({
    key: opt.key,
    label: opt.label,
    icon: opt.icon,
    extra: opt.extra,
    path: opt.path,
    meta: opt.meta,
    component: opt.component,
    children: opt.children
  }))
)

const activeTop = computed(() => topOptions.value.find((o) => o.key === activeTopKey.value) || null)

const activeTopLabel = computed(() => {
  const label = activeTop.value?.label
  return typeof label === 'string' ? label : '菜单'
})

const subOptions = computed(() => {
  const top = activeTop.value
  if (!top) return []
  if (top.children?.length) return top.children
  // 一级叶子：二级栏展示自身，避免出现空白列
  return [
    {
      label: top.label,
      key: top.key,
      icon: top.icon,
      extra: top.extra,
      meta: top.meta,
      component: top.component
    }
  ]
})

function selectTop(item) {
  activeTopKey.value = item.key
  if (item.children?.length) {
    const leaf = findFirstLeaf(item.children)
    if (leaf?.key) handleSelect(leaf.key)
    return
  }
  handleSelect(item.key)
}

function findFirstLeaf(options) {
  for (const opt of options || []) {
    const key = String(opt.key || '')
    const isLeafKey =
      key.startsWith('/') || key.startsWith('http://') || key.startsWith('https://')
    if (!opt.children?.length && isLeafKey) return opt
    const nested = findFirstLeaf(opt.children)
    if (nested) return nested
  }
  return null
}

watch(
  [() => route.path, menuOptions],
  () => {
    const top = findTopLevel(menuOptions.value, route.path)
    if (top) activeTopKey.value = top.key
  },
  { immediate: true }
)
</script>

<style scoped>
.dual-sidebar {
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--color-surface);
}

.dual-sidebar__icons {
  flex: 0 0 80px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
  min-height: 0;
  padding: 0 6px 8px;
  overflow-x: hidden;
  overflow-y: auto;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
}

.dual-sidebar__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: calc(100% + 12px);
  margin: 0 -6px 4px;
  height: var(--layout-nav-height, 44px);
  border-bottom: 1px solid var(--color-border);
}

.dual-sidebar__logo-img {
  width: 28px;
  height: 28px;
}

.dual-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  min-height: 56px;
  padding: 8px 4px;
  border: none;
  border-radius: var(--radius);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
}

.dual-icon-item:hover {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 8%, transparent);
}

.dual-icon-item.is-active {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.dual-icon-item :deep(.n-icon) {
  font-size: 20px;
  line-height: 1;
}

.dual-icon-item__icon {
  position: relative;
  display: inline-flex;
  line-height: 0;
}

/* 徽标压在图标右上角：图标栏只有 20px 图标，放在标题旁会把「模块名」挤掉 */
.dual-icon-item__badge {
  position: absolute;
  top: -8px;
  left: 12px;
  line-height: 0;
  pointer-events: none;
}

.dual-icon-item__badge :deep(.n-badge) {
  display: inline-flex;
  line-height: 0;
}

.dual-icon-item__badge :deep(.n-badge-sup) {
  font-size: 11px;
  height: 16px;
  line-height: 16px;
  min-width: 16px;
  padding: 0 4px;
  border-radius: 8px;
}

.dual-icon-item__label {
  max-width: 100%;
  font-size: 12px;
  line-height: 1.2;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dual-sidebar.is-solo .dual-sidebar__icons {
  border-right: none;
}

.dual-sidebar__subs {
  flex: 0 0 160px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
}

.dual-sidebar__title {
  flex-shrink: 0;
  height: 40px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
}

.dual-sidebar__menu-scroll {
  flex: 1 1 0;
  min-height: 0;
}

.dual-sidebar__subs :deep(.n-scrollbar) {
  flex: 1 1 0;
  min-height: 0;
  max-height: none;
}

.dual-sidebar__subs :deep(.n-menu-item-content) {
  border-radius: var(--radius) !important;
}

/* 与经典侧栏一致：徽标靠右对齐，不挤占菜单标题 */
.dual-sidebar__subs :deep(.n-menu-item-content-header__extra) {
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  line-height: 1;
  pointer-events: none;
}

.dual-sidebar__subs :deep(.n-menu-item-content-header__extra .n-badge) {
  display: inline-flex;
}
</style>
