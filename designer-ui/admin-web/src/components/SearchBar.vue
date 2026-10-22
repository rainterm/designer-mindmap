<template>
  <n-card class="search-card" :bordered="false">
    <n-form
      inline
      :show-feedback="false"
      :model="formModel"
      label-placement="left"
      class="search-form"
      @submit.prevent
    >
      <div class="search-fields">
        <slot name="fields" />
      </div>
      <div class="search-actions">
        <slot name="actions">
          <n-button v-if="showReset" @click="onReset">重置</n-button>
          <n-button v-if="showSearch" type="primary" :loading="formLoading" @click="onSearch">
            查询
          </n-button>
          <SearchExpand v-if="showExpand" />
        </slot>
      </div>
    </n-form>
  </n-card>
</template>

<script setup>
import { computed, unref } from 'vue'
import SearchExpand from '@/components/SearchExpand.vue'

const props = defineProps({
  /** usePagination 返回值；传入后自动绑定 model / loading / search / reset */
  pager: { type: Object, default: null },
  model: { type: Object, default: null },
  loading: { type: Boolean, default: undefined },
  showExpand: { type: Boolean, default: true },
  showReset: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true }
})

const emit = defineEmits(['search', 'reset'])

const formModel = computed(() => props.model ?? props.pager?.query ?? {})
const formLoading = computed(() => {
  if (props.loading !== undefined) return props.loading
  return unref(props.pager?.loading) || false
})

function onSearch() {
  props.pager?.handleSearch?.()
  emit('search')
}

function onReset() {
  props.pager?.handleReset?.()
  emit('reset')
}
</script>
