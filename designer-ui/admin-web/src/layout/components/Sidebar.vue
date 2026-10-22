<template>
  <n-menu
    ref="menuRef"
    v-model:expanded-keys="expandedKeys"
    :value="activeKey"
    :mode="mode"
    :collapsed="isCollapsed"
    :collapsed-width="64"
    :collapsed-icon-size="20"
    :options="menuOptions"
    :indent="20"
    :root-indent="mode === 'horizontal' ? 12 : 18"
    :accordion="mode !== 'horizontal'"
    :responsive="mode === 'horizontal'"
    :dropdown-props="horizontalDropdownProps"
    @update:value="handleSelect"
  />
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAppStore } from '@/store/app'
import { useSidebarMenu } from '@/composables/useSidebarMenu'

const props = defineProps({
  /** vertical | horizontal */
  mode: { type: String, default: 'vertical' },
  /** 覆盖折叠态；不传则跟随 store */
  collapsed: { type: Boolean, default: undefined },
  /** full=完整树；top=仅一级；children=当前一级的子菜单 */
  scope: { type: String, default: 'full' }
})

const appStore = useAppStore()
const collapsedOverride = computed(() =>
  props.collapsed !== undefined ? props.collapsed : appStore.collapsed
)

const scopeRef = computed(() => props.scope)

const isCollapsed = computed(() => (props.mode === 'horizontal' ? false : collapsedOverride.value))

const { activeKey, expandedKeys, menuOptions, handleSelect } = useSidebarMenu({
  collapsed: collapsedOverride,
  scope: scopeRef,
  // 折叠成图标栏后标题区被隐藏，红点改挂到图标上
  iconBadge: isCollapsed
})

const menuRef = ref(null)

const horizontalDropdownProps = computed(() =>
  props.mode === 'horizontal'
    ? {
        scrollable: true,
        menuProps: () => ({
          style: { maxHeight: '70vh' }
        })
      }
    : undefined
)

function syncResponsive() {
  if (props.mode !== 'horizontal') return
  nextTick(() => {
    menuRef.value?.deriveResponsiveState?.()
  })
}

let resizeObserver = null

function resolveEl(inst) {
  const el = inst?.$el ?? inst
  return el instanceof Element ? el : null
}

onMounted(() => {
  if (props.mode !== 'horizontal') return
  window.addEventListener('resize', syncResponsive)
  nextTick(() => {
    syncResponsive()
    const target = resolveEl(menuRef.value)
    const parent = target?.parentElement ?? null
    if (typeof ResizeObserver === 'undefined' || !target) return
    resizeObserver = new ResizeObserver(() => syncResponsive())
    resizeObserver.observe(target)
    if (parent instanceof Element) resizeObserver.observe(parent)
  })
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', syncResponsive)
})

watch(menuOptions, () => syncResponsive(), { deep: true })
watch(activeKey, () => syncResponsive())
</script>

<style scoped>
:deep(.n-menu-item-content) {
  position: relative;
}
:deep(.n-menu-item-content-header__extra) {
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  line-height: 1;
  pointer-events: none;
}
/* 父级菜单右侧还有展开箭头，徽标要让开它（结构是 .n-submenu > .n-menu-item > .n-menu-item-content） */
:deep(.n-submenu > .n-menu-item > .n-menu-item-content .n-menu-item-content-header__extra) {
  right: 38px;
}
/* 顶栏菜单每个 item 按内容撑宽，徽标钉在右上角会压到文字，改为跟在标题后面 */
:deep(.n-menu--horizontal .n-menu-item-content-header__extra),
:deep(.n-menu--horizontal .n-submenu > .n-menu-item > .n-menu-item-content .n-menu-item-content-header__extra) {
  position: static;
  right: auto;
  top: auto;
  transform: none;
}
/* 间距加在徽标本体上：没有待办时徽标不渲染，也就不会留下多余空隙 */
:deep(.n-menu--horizontal .n-menu-item-content-header__extra .n-badge-sup) {
  margin-left: 6px;
}
:deep(.n-menu-item-content-header__extra .n-badge) {
  display: inline-flex;
}

/* 水平菜单：宽度跟容器走，才能触发 responsive 溢出折叠 */
:deep(.n-menu--horizontal) {
  width: 100%;
  max-width: 100%;
  height: var(--layout-nav-height, 44px);
}
:deep(.n-menu--horizontal.n-menu--responsive) {
  overflow: hidden;
}
:deep(.n-menu--horizontal .n-menu-item-content) {
  padding: 0 12px !important;
}
</style>
