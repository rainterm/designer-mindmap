<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN" :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
        <PageLoading />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { computed, watch } from 'vue'
import { zhCN, dateZhCN } from 'naive-ui'
import PageLoading from '@/layout/components/PageLoading.vue'
import { useAppStore } from '@/store/app'
import { applyThemeCssVars, buildThemeOverrides } from '@/theme'

const appStore = useAppStore()

const themeOverrides = computed(() => buildThemeOverrides(appStore.primaryColor))

watch(
  () => appStore.primaryColor,
  (color) => applyThemeCssVars(color),
  { immediate: true }
)
</script>
