<template>
  <n-drawer
    :show="appStore.preferenceVisible"
    :width="420"
    placement="right"
    :trap-focus="false"
    @update:show="onVisible"
  >
    <n-drawer-content :native-scrollbar="false" closable class="pref-drawer">
      <template #header>
        <div class="pref-header">
          <div class="pref-header__text">
            <div class="pref-header__title">偏好设置</div>
            <div class="pref-header__sub">自定义偏好设置 & 实时预览</div>
          </div>
          <div class="pref-header__actions">
            <n-tooltip :show-arrow="false">
              <template #trigger>
                <n-button quaternary circle @click="resetPreference">
                  <template #icon>
                    <n-icon :component="RefreshOutline" :size="18" />
                  </template>
                </n-button>
              </template>
              重置偏好
            </n-tooltip>
          </div>
        </div>
      </template>

      <div class="pref-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="pref-tab"
          :class="{ 'is-active': activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="activeTab === 'appearance'" class="pref-section">
        <div class="pref-section__title">主题色</div>
        <ThemeColorPicker
          :model-value="appStore.primaryColor"
          @update:model-value="appStore.setPrimaryColor"
        />
      </div>

      <div v-else-if="activeTab === 'layout'" class="pref-section">
        <div class="pref-section__title">布局</div>
        <LayoutPicker
          :model-value="appStore.layoutMode"
          @update:model-value="appStore.setLayoutMode"
        />
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { RefreshOutline } from '@vicons/ionicons5'
import LayoutPicker from '@/components/LayoutPicker.vue'
import ThemeColorPicker from '@/components/ThemeColorPicker.vue'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
const activeTab = ref('appearance')

const tabs = [
  { key: 'appearance', label: '外观' },
  { key: 'layout', label: '布局' }
]

function onVisible(show) {
  if (show) appStore.openPreference()
  else appStore.closePreference()
}

function resetPreference() {
  appStore.resetPreference()
}
</script>

<style scoped>
.pref-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding-right: 28px;
}

.pref-header__title {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--color-text);
}

.pref-header__sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.pref-header__actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.pref-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px;
  margin-bottom: 20px;
  border-radius: var(--radius);
  background: color-mix(in srgb, var(--color-body) 85%, var(--color-border));
}

.pref-tab {
  flex: 1 1 auto;
  min-width: 56px;
  height: 32px;
  padding: 0 10px;
  border: none;
  border-radius: calc(var(--radius) - 1px);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s, box-shadow 0.15s;
}

.pref-tab:hover {
  color: var(--color-text);
}

.pref-tab.is-active {
  background: var(--color-surface);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.pref-section__title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}
</style>
