<template>
  <n-layout
    class="admin-layout"
    :has-sider="showSider && !headerFull"
    :class="[`layout--${layoutMode}`]"
  >
    <!-- 经典垂直 / 双列：侧栏在外层 -->
    <n-layout-sider
      v-if="showSider && !headerFull"
      bordered
      collapse-mode="width"
      :collapsed-width="siderCollapsedWidth"
      :width="siderWidth"
      :collapsed="siderCollapsed"
      :show-trigger="false"
      :native-scrollbar="true"
      :content-style="siderContentStyle"
    >
      <div class="sider-inner">
        <div v-if="!isDual" class="logo">
          <BrandLogo
            class="logo-img"
            :src="settingStore.logoUrl"
            :radius="settingStore.logoRadius"
          />
          <span v-show="!siderCollapsed" class="logo-text">{{ settingStore.systemName }}</span>
        </div>
        <div class="sidebar-menu" :class="{ 'sidebar-menu--dual': isDual || iconOnly }">
          <DualColumnSidebar v-if="isDual" show-logo />
          <DualColumnSidebar v-else-if="iconOnly" :show-subs="false" />
          <n-scrollbar v-else style="max-height: 100%">
            <Sidebar :collapsed="siderCollapsed" />
          </n-scrollbar>
        </div>
      </div>
    </n-layout-sider>

    <n-layout class="main-layout">
      <!-- 通栏顶栏（水平 / 侧边导航 / 混合 / 全屏） -->
      <n-layout-header v-if="headerFull" class="app-header app-header--full">
        <Navbar
          :show-logo="true"
          :show-collapse="layoutMode === 'mixed-vertical' && mixedHasChildren"
          :show-breadcrumb="layoutMode !== 'horizontal' && layoutMode !== 'content-full' && layoutMode !== 'mixed-vertical'"
          :show-horizontal-menu="layoutMode === 'horizontal' || layoutMode === 'mixed-vertical'"
          :menu-scope="layoutMode === 'mixed-vertical' ? 'top' : 'full'"
        />
        <TagsView />
      </n-layout-header>

      <n-layout
        v-if="showSider && headerFull"
        has-sider
        class="body-with-sider"
      >
        <n-layout-sider
          bordered
          collapse-mode="width"
          :collapsed-width="siderCollapsedWidth"
          :width="siderWidth"
          :collapsed="siderCollapsed"
          :show-trigger="false"
          :native-scrollbar="true"
          :content-style="siderContentStyle"
        >
          <div class="sider-inner sider-inner--below-header">
            <div class="sidebar-menu" :class="{ 'sidebar-menu--dual': isDual || iconOnly }">
              <DualColumnSidebar v-if="isDual" />
              <DualColumnSidebar v-else-if="iconOnly" :show-subs="false" />
              <n-scrollbar v-else style="max-height: 100%">
                <Sidebar
                  :collapsed="siderCollapsed"
                  :scope="layoutMode === 'mixed-vertical' ? 'children' : 'full'"
                />
              </n-scrollbar>
            </div>
          </div>
        </n-layout-sider>

        <n-layout-content class="app-content" :native-scrollbar="true" :content-style="contentStyle">
          <router-view v-slot="{ Component, route }">
            <transition name="page-fade" mode="out-in" appear>
              <keep-alive :max="12">
                <component :is="Component" :key="route.path" />
              </keep-alive>
            </transition>
          </router-view>
        </n-layout-content>
      </n-layout>

      <!-- 垂直 / 双列：顶栏在内容区上方 -->
      <template v-else-if="!headerFull">
        <n-layout-header class="app-header">
          <Navbar
            :show-collapse="!isDual"
            :show-breadcrumb="true"
          />
          <TagsView />
        </n-layout-header>

        <n-layout-content class="app-content" :native-scrollbar="true" :content-style="contentStyle">
          <router-view v-slot="{ Component, route }">
            <transition name="page-fade" mode="out-in" appear>
              <keep-alive :max="12">
                <component :is="Component" :key="route.path" />
              </keep-alive>
            </transition>
          </router-view>
        </n-layout-content>
      </template>

      <!-- 水平 / 内容全屏：无侧栏 -->
      <n-layout-content
        v-else
        class="app-content"
        :native-scrollbar="true"
        :content-style="contentStyle"
      >
        <router-view v-slot="{ Component, route }">
          <transition name="page-fade" mode="out-in" appear>
            <keep-alive :max="12">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </n-layout-content>
    </n-layout>

    <PreferenceSettings />
  </n-layout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useAppStore } from '@/store/app'
import { useSettingStore } from '@/store/setting'
import { useMenuBadgeStore } from '@/store/menuBadge'
import {
  hasSider,
  isDualColumn,
  isHeaderFull,
  isIconSider,
  LAYOUT_MODES
} from '@/constants/layout'
import { useSidebarMenu } from '@/composables/useSidebarMenu'
import BrandLogo from '@/components/BrandLogo.vue'
import Sidebar from './components/Sidebar.vue'
import DualColumnSidebar from './components/DualColumnSidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'
import PreferenceSettings from './components/PreferenceSettings.vue'

const appStore = useAppStore()
const settingStore = useSettingStore()
const badgeStore = useMenuBadgeStore()
const { currentTop } = useSidebarMenu()

const layoutMode = computed(() => appStore.layoutMode)
const mixedHasChildren = computed(
  () => layoutMode.value === LAYOUT_MODES.MIXED_VERTICAL && !!currentTop.value?.children?.length
)
const headerFull = computed(() => isHeaderFull(layoutMode.value))
const isDual = computed(() => isDualColumn(layoutMode.value))
const iconOnly = computed(() => isIconSider(layoutMode.value))
const showSider = computed(() => {
  if (layoutMode.value === LAYOUT_MODES.MIXED_VERTICAL) return mixedHasChildren.value
  return hasSider(layoutMode.value)
})

const siderCollapsed = computed(() => {
  if (isDual.value || iconOnly.value) return false
  return appStore.collapsed
})

const siderWidth = computed(() => {
  if (isDual.value) return 240 // 80 + 160
  if (iconOnly.value) return 80
  return 220
})

const siderCollapsedWidth = computed(() => 64)

const siderContentStyle = {
  overflow: 'hidden',
  height: '100%',
  display: 'flex',
  flexDirection: 'column',
  padding: '0',
  backgroundColor: 'var(--color-surface)'
}

const contentStyle = {
  padding: '0',
  backgroundColor: 'var(--color-body)'
}

onMounted(() => {
  if (window.innerWidth < 1200 && layoutMode.value === LAYOUT_MODES.VERTICAL) {
    appStore.collapsed = true
  }
  // 菜单红点：布局存活期间按各自间隔轮询（无注册项时为空操作）
  badgeStore.start()
})

onBeforeUnmount(() => {
  badgeStore.stop()
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* Naive 会包一层 scroll-container，必须把它纳入高度约束，否则 flex 子项无效 */
.admin-layout :deep(> .n-layout-scroll-container) {
  height: 100% !important;
  max-height: 100%;
  overflow: hidden !important;
}

.admin-layout :deep(.n-layout-sider) {
  background: var(--color-surface) !important;
}

.admin-layout :deep(.n-layout-sider-scroll-container) {
  height: 100% !important;
  overflow: hidden !important;
  background: var(--color-surface) !important;
}

.sider-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

.sider-inner--below-header {
  height: 100%;
}

.logo {
  flex: 0 0 var(--layout-nav-height, 44px);
  display: flex;
  align-items: center;
  gap: 10px;
  height: var(--layout-nav-height, 44px);
  padding: 0 16px;
  border-bottom: 1px solid var(--color-border);
  overflow: hidden;
  white-space: nowrap;
  background: var(--color-surface);
}

.logo-img {
  width: 28px;
  height: 28px;
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text);
}

.sidebar-menu {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 6px 0 10px;
  background: var(--color-surface);
}

.layout--dual-column .sidebar-menu,
.layout--mixed-dual .sidebar-menu,
.layout--side-nav .sidebar-menu,
.sidebar-menu--dual {
  padding: 0;
}

.sidebar-menu :deep(.n-menu-item-content) {
  border-radius: var(--radius) !important;
}

.sidebar-menu :deep(.n-menu-item-content--selected) {
  font-weight: 600;
}

.sidebar-menu :deep(.n-submenu-children .n-menu-item-content) {
  font-size: 13px;
}

.sidebar-menu :deep(.n-scrollbar > .n-scrollbar-rail) {
  width: 4px !important;
  right: 2px;
}

.sidebar-menu :deep(.n-scrollbar > .n-scrollbar-rail > .n-scrollbar-rail__scrollbar) {
  width: 4px !important;
  border-radius: var(--radius);
  background-color: rgba(0, 0, 0, 0.18);
}

.main-layout {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  background-color: var(--color-body) !important;
}

.main-layout :deep(> .n-layout-scroll-container) {
  display: flex !important;
  flex-direction: column;
  height: 100% !important;
  max-height: 100%;
  overflow: hidden !important;
}

/* 通栏顶栏下：只占剩余高度，勿再 height:100%（会与顶栏叠加超出视口） */
.body-with-sider {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
  background-color: var(--color-body) !important;
}

.body-with-sider :deep(> .n-layout-scroll-container) {
  height: 100% !important;
  max-height: 100%;
  overflow: hidden !important;
}

.app-header {
  flex-shrink: 0;
  background-color: var(--color-surface) !important;
  overflow: visible !important;
}

.app-content {
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
  background-color: var(--color-body) !important;
}

/* 内容区在布局内滚动：未超出不出现滚动条，超出一屏则可滚 */
.app-content :deep(.n-layout-scroll-container) {
  height: 100% !important;
  min-height: 0;
  overflow: auto !important;
  background-color: var(--color-body) !important;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
}
</style>
