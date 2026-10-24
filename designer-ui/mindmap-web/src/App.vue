<template>
  <n-config-provider
    class="app-root"
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme-overrides="themeOverrides"
  >
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { zhCN, dateZhCN } from 'naive-ui'
import { themeOverrides } from '@/theme'
</script>

<style>
.app-root,
.app-root .n-message-provider,
.app-root .n-dialog-provider {
  height: 100%;
}
</style>
