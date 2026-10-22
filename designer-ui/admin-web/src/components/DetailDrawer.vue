<template>
  <n-drawer :show="show" :width="resolvedWidth" :placement="placement" @update:show="emit('update:show', $event)">
    <n-drawer-content :title="title" closable>
      <slot />
      <template v-if="$slots.footer" #footer>
        <slot name="footer" />
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed } from 'vue'

const SIZE_MAP = {
  sm: 360,
  md: 480,
  lg: 640,
  xl: 800
}

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: '详情' },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg', 'xl'].includes(v)
  },
  width: { type: [Number, String], default: null },
  placement: { type: String, default: 'right' }
})

const emit = defineEmits(['update:show'])

const resolvedWidth = computed(() => {
  if (props.width != null && props.width !== '') return props.width
  return SIZE_MAP[props.size] || SIZE_MAP.md
})
</script>
