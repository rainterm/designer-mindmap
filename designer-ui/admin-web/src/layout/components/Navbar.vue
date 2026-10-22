<template>
  <div class="navbar" :class="{ 'navbar--with-logo': showLogo }">
    <div class="navbar-left">
      <div v-if="showLogo" class="navbar-brand">
        <BrandLogo
          class="navbar-brand__logo"
          :src="settingStore.logoUrl"
          :radius="settingStore.logoRadius"
        />
        <span class="navbar-brand__text">{{ settingStore.systemName }}</span>
      </div>

      <n-button v-if="showCollapse" quaternary circle @click="appStore.toggleCollapsed()">
        <template #icon>
          <n-icon :component="appStore.collapsed ? ChevronForwardOutline : ChevronBackOutline" />
        </template>
      </n-button>

      <Breadcrumb v-if="showBreadcrumb" />

      <div v-if="showHorizontalMenu" class="navbar-menu">
        <Sidebar mode="horizontal" :scope="menuScope" />
      </div>
    </div>

    <div class="navbar-right">
      <n-tooltip :show-arrow="false">
        <template #trigger>
          <n-button quaternary circle @click="appStore.openPreference()">
            <template #icon>
              <n-icon :component="SettingsOutline" :size="20" />
            </template>
          </n-button>
        </template>
        偏好设置
      </n-tooltip>

      <MessageInbox />
      <n-dropdown :options="userOptions" trigger="click" @select="handleUserAction">
        <div class="user-info">
          <n-avatar round :size="28" :src="userStore.avatar || undefined">
            <!-- 有默认插槽内容时 Naive 会忽略 src，故文字只在无头像时渲染 -->
            <template v-if="!userStore.avatar">{{ shortName }}</template>
            <template #fallback>{{ shortName }}</template>
          </n-avatar>
          <span class="user-name">{{ userStore.nickname }}</span>
          <n-icon :component="ChevronDownOutline" :size="14" />
        </div>
      </n-dropdown>
    </div>

    <!-- 个人中心 / 修改密码弹框：由右上角用户下拉菜单唤起 -->
    <ProfileDialog v-model:show="profileVisible" />
    <PasswordDialog v-model:show="passwordVisible" />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, useDialog } from 'naive-ui'
import {
  ChevronBackOutline,
  ChevronForwardOutline,
  ChevronDownOutline,
  PersonOutline,
  LogOutOutline,
  KeyOutline,
  SettingsOutline
} from '@vicons/ionicons5'
import BrandLogo from '@/components/BrandLogo.vue'
import Breadcrumb from './Breadcrumb.vue'
import MessageInbox from './MessageInbox.vue'
import PasswordDialog from './PasswordDialog.vue'
import ProfileDialog from './ProfileDialog.vue'
import Sidebar from './Sidebar.vue'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import { useSettingStore } from '@/store/setting'

defineProps({
  showLogo: { type: Boolean, default: false },
  showCollapse: { type: Boolean, default: true },
  showBreadcrumb: { type: Boolean, default: true },
  showHorizontalMenu: { type: Boolean, default: false },
  /** 顶栏菜单范围：full 含下拉子项；top 仅一级 */
  menuScope: { type: String, default: 'full' }
})

const router = useRouter()
const dialog = useDialog()
const appStore = useAppStore()
const userStore = useUserStore()
const settingStore = useSettingStore()

const profileVisible = ref(false)
const passwordVisible = ref(false)

const shortName = computed(() => (userStore.nickname || '未').slice(0, 1))

const userOptions = [
  { label: '个人中心', key: 'profile', icon: () => h(NIcon, null, { default: () => h(PersonOutline) }) },
  { label: '修改密码', key: 'password', icon: () => h(NIcon, null, { default: () => h(KeyOutline) }) },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout', icon: () => h(NIcon, null, { default: () => h(LogOutOutline) }) }
]

function handleUserAction(key) {
  if (key === 'profile') {
    profileVisible.value = true
    return
  }
  if (key === 'password') {
    passwordVisible.value = true
    return
  }
  if (key === 'logout') {
    dialog.warning({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      positiveText: '确定退出',
      negativeText: '取消',
      positiveButtonProps: { size: 'medium' },
      negativeButtonProps: { size: 'medium' },
      onPositiveClick: async () => {
        await userStore.logout()
        router.replace({ path: '/login' })
      }
    })
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--layout-nav-height, 44px);
  padding: 0 14px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  overflow: visible;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  margin-right: 4px;
}

.navbar-brand__logo {
  width: 28px;
  height: 28px;
}

.navbar-brand__text {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text);
  white-space: nowrap;
}

.navbar-menu {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* 强制水平菜单吃满可用宽度，超出由 n-menu responsive 收进「···」 */
.navbar-menu :deep(.n-menu) {
  width: 100%;
  max-width: 100%;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 100%;
  flex-shrink: 0;
  overflow: visible;
}

.navbar-right :deep(.n-button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.navbar-right :deep(.n-dropdown-trigger),
.navbar-right :deep(.n-tooltip) {
  display: inline-flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 10px 0 4px;
  border-radius: var(--radius);
  cursor: pointer;
  line-height: 1;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: color-mix(in srgb, var(--color-primary) 6%, transparent);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

@media (max-width: 720px) {
  .navbar-brand__text,
  .user-name {
    display: none;
  }
}
</style>
