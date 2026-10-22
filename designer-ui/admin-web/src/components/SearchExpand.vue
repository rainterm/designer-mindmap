<template>
  <span ref="rootRef" class="search-expand">
    <n-button
      v-show="needToggle"
      text
      type="primary"
      class="search-expand-btn"
      @click="toggle"
    >
      {{ expanded ? '收起' : '展开' }}
    </n-button>
  </span>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 查询条件超出一行时显示展开/收起
 * 需放在 .search-form > .search-actions 内
 */
const expanded = ref(false)
const needToggle = ref(false)
const rootRef = ref(null)

let fieldsEl = null
let formEl = null
let ro = null
let measuring = false
let raf = 0

function columnCount() {
  const w = window.innerWidth
  if (w <= 720) return 1
  if (w <= 1100) return 2
  return 3
}

function itemCount() {
  return fieldsEl?.querySelectorAll(':scope > .n-form-item').length || 0
}

function rowHeight() {
  const first = fieldsEl?.querySelector(':scope > .n-form-item')
  return first ? Math.ceil(first.getBoundingClientRect().height) : 34
}

function measure() {
  if (!fieldsEl || measuring) return
  measuring = true
  try {
    const cols = columnCount()
    const shouldToggle = itemCount() > cols
    needToggle.value = shouldToggle

    if (!shouldToggle) {
      expanded.value = false
      fieldsEl.classList.remove('is-collapsed')
      fieldsEl.style.maxHeight = ''
      formEl?.classList.remove('is-expanded')
      return
    }

    formEl?.classList.toggle('is-expanded', expanded.value)

    // 展开态用 display:contents 统一网格，收起态用 max-height 裁切
    if (expanded.value) {
      fieldsEl.classList.remove('is-collapsed')
      fieldsEl.style.maxHeight = ''
      return
    }

    fieldsEl.classList.add('is-collapsed')
    fieldsEl.style.maxHeight = `${rowHeight()}px`
  } finally {
    requestAnimationFrame(() => {
      measuring = false
    })
  }
}

function scheduleMeasure() {
  if (raf) cancelAnimationFrame(raf)
  raf = requestAnimationFrame(() => {
    raf = 0
    measure()
  })
}

function toggle() {
  expanded.value = !expanded.value
  formEl?.classList.toggle('is-expanded', expanded.value)
  measure()
  nextTick(measure)
}

onMounted(() => {
  nextTick(() => {
    formEl = rootRef.value?.closest?.('.search-form') || null
    fieldsEl = formEl?.querySelector('.search-fields') || null
    if (!fieldsEl) return
    expanded.value = false
    measure()
    ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(scheduleMeasure) : null
    ro?.observe(formEl || fieldsEl)
    window.addEventListener('resize', scheduleMeasure)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', scheduleMeasure)
  if (raf) cancelAnimationFrame(raf)
  ro?.disconnect()
  if (fieldsEl) {
    fieldsEl.classList.remove('is-collapsed')
    fieldsEl.style.maxHeight = ''
  }
  formEl?.classList.remove('is-expanded')
  ro = null
  fieldsEl = null
  formEl = null
})
</script>
