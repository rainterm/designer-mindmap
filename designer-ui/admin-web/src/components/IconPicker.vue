<template>
  <div class="icon-picker">
    <n-popover
      v-model:show="panelOpen"
      trigger="click"
      placement="bottom-start"
      :show-arrow="false"
      raw
      :style="{ padding: 0 }"
      @update:show="onPanelToggle"
    >
      <template #trigger>
        <button type="button" class="icon-picker__trigger" :class="{ 'is-open': panelOpen }">
          <span class="icon-picker__preview">
            <n-icon v-if="modelValue" :component="currentIcon" :size="18" />
            <span v-else class="icon-picker__placeholder-icon">?</span>
          </span>
          <span class="icon-picker__text">{{ modelValue || '请选择图标' }}</span>
          <n-icon :component="ChevronDownOutline" :size="14" class="icon-picker__caret" />
        </button>
      </template>

      <div class="icon-picker__panel">
        <div class="icon-picker__search">
          <n-input
            v-model:value="keyword"
            clearable
            placeholder="搜索图标名称"
          />
        </div>
        <div class="icon-picker__grid">
          <button
            v-for="name in filteredOptions"
            :key="name"
            type="button"
            class="icon-picker__item"
            :class="{ 'is-active': modelValue === name }"
            :title="name"
            @click="select(name)"
          >
            <n-icon :component="resolveIcon(name)" :size="20" />
            <span class="icon-picker__item-label">{{ name }}</span>
          </button>
          <div v-if="!filteredOptions.length" class="icon-picker__empty">无匹配图标</div>
        </div>
      </div>
    </n-popover>

    <n-button
      v-if="modelValue"
      quaternary
      class="icon-picker__clear"
      @click="clear"
    >
      清除
    </n-button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ChevronDownOutline } from '@vicons/ionicons5'
import { ICON_OPTIONS, resolveIcon } from '@/utils/icon'

const props = defineProps({
  modelValue: { type: String, default: null }
})

const emit = defineEmits(['update:modelValue'])

const panelOpen = ref(false)
const keyword = ref('')

const currentIcon = computed(() => (props.modelValue ? resolveIcon(props.modelValue) : null))

const filteredOptions = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return ICON_OPTIONS
  return ICON_OPTIONS.filter((name) => name.toLowerCase().includes(q))
})

function onPanelToggle(show) {
  if (!show) keyword.value = ''
}

function select(name) {
  emit('update:modelValue', name)
  panelOpen.value = false
}

function clear() {
  emit('update:modelValue', null)
}
</script>

<style scoped>
.icon-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.icon-picker__trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 220px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.icon-picker__trigger:hover,
.icon-picker__trigger.is-open {
  border-color: var(--color-primary);
}

.icon-picker__trigger.is-open {
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary) 18%, transparent);
}

.icon-picker__preview {
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  color: var(--color-primary);
}

.icon-picker__placeholder-icon {
  font-size: 12px;
  color: var(--color-text-muted);
}

.icon-picker__text {
  flex: 1;
  min-width: 0;
  text-align: left;
  font-size: 13px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-picker__caret {
  color: var(--color-text-muted);
}

.icon-picker__clear {
  flex-shrink: 0;
}

.icon-picker__panel {
  width: 420px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: 0 8px 24px rgba(22, 26, 23, 0.12);
}

.icon-picker__search {
  padding: 10px 10px 0;
}

.icon-picker__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  max-height: 320px;
  overflow: auto;
  padding: 10px;
}

.icon-picker__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 60px;
  padding: 8px 4px;
  border: 1px solid transparent;
  border-radius: var(--radius);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color 0.15s, border-color 0.15s, color 0.15s;
}

.icon-picker__item:hover {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 8%, transparent);
}

.icon-picker__item.is-active {
  color: var(--color-primary);
  border-color: color-mix(in srgb, var(--color-primary) 35%, transparent);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.icon-picker__item-label {
  max-width: 100%;
  font-size: 11px;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-picker__empty {
  grid-column: 1 / -1;
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
}
</style>
