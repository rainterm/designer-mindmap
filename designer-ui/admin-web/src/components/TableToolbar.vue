<template>
  <div
    v-if="hasLeft || hasRight"
    class="table-toolbar"
    :class="{ 'table-toolbar--split': hasLeft && hasRight }"
  >
    <div v-if="hasLeft" class="table-toolbar__left">
      <slot name="left" />
    </div>
    <div v-if="hasRight" class="table-toolbar__right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

const slots = useSlots()
const hasLeft = computed(() => !!slots.left)
const hasRight = computed(() => !!slots.right)
</script>

<style scoped>
/* 默认靠右；左右都有时（如标题 + 操作）再两端对齐 */
.table-toolbar--split {
  justify-content: space-between;
}

.table-toolbar__left:only-child,
.table-toolbar__right {
  margin-left: auto;
}
</style>
