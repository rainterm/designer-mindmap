<template>
  <div class="theme-color-picker">
    <div class="color-row">
      <button
        v-for="preset in PRIMARY_PRESETS"
        :key="preset.value"
        type="button"
        class="color-swatch"
        :class="{ 'is-active': isActive(preset.value) }"
        :style="{ background: preset.value }"
        :title="preset.label"
        @click="emit('update:modelValue', preset.value)"
      >
        <n-icon
          v-if="isActive(preset.value)"
          class="color-swatch__check"
          :component="CheckmarkOutline"
          :size="16"
        />
      </button>
      <n-color-picker
        class="color-picker"
        :value="modelValue"
        :modes="['hex']"
        :show-alpha="false"
        @update:value="onPick"
      />
    </div>
    <div class="color-current">
      当前：{{ currentPresetLabel }}
      <span class="color-current__hex">{{ modelValue }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CheckmarkOutline } from '@vicons/ionicons5'
import { PRIMARY_PRESETS, normalizePrimaryColor } from '@/theme'

/**
 * 主题色选择器：预设色板 + 取色器 + 当前值回显。
 *
 * <p>「偏好设置」外观面板与「系统设置」的默认主题色共用同一个组件，
 * 保证两处色板尺寸、选中态与文案一致。</p>
 */
const props = defineProps({
  /** 当前主题色（合法 hex；见 theme/index.js 的 normalizePrimaryColor） */
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

function isActive(value) {
  return normalizePrimaryColor(props.modelValue) === normalizePrimaryColor(value)
}

const currentPresetLabel = computed(() => {
  const hit = PRIMARY_PRESETS.find((p) => isActive(p.value))
  return hit ? hit.label : '自定义'
})

/** 统一在组件内归一化，调用方拿到的永远是合法的大写 hex */
function onPick(color) {
  if (!color) return
  emit('update:modelValue', normalizePrimaryColor(color))
}
</script>

<style scoped>
.theme-color-picker {
  /* 与 LayoutPicker 同理：放进 n-form-item 的 blank（flex，flex-grow 为 0）时
     需要主动撑满，否则会缩成内容宽度。 */
  width: 100%;
  min-width: 0;
}

.color-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.color-swatch {
  position: relative;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s, box-shadow 0.15s;
}

.color-swatch:hover {
  transform: scale(1.08);
}

.color-swatch.is-active {
  box-shadow:
    0 0 0 2px var(--color-surface),
    0 0 0 4px var(--color-primary);
}

.color-swatch__check {
  position: absolute;
  inset: 0;
  margin: auto;
  color: #fff;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.25));
}

.color-picker {
  width: 28px;
  height: 28px;
}

.color-picker :deep(.n-color-picker-trigger) {
  width: 28px;
  height: 28px;
  min-width: 28px;
  padding: 0;
  border-radius: 50%;
  overflow: hidden;
}

.color-picker :deep(.n-color-picker-trigger__fill) {
  border-radius: 50%;
  inset: 2px;
}

.color-current {
  margin-top: 12px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.color-current__hex {
  margin-left: 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--color-text-muted);
}
</style>
