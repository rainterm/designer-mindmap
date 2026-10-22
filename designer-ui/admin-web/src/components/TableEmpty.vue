<template>
  <div class="table-empty">
    <svg class="table-empty-art" viewBox="0 0 96 72" fill="none" aria-hidden="true">
      <rect x="14" y="18" width="68" height="42" fill="currentColor" opacity="0.08" />
      <rect x="22" y="26" width="52" height="6" fill="currentColor" opacity="0.18" />
      <rect x="22" y="38" width="36" height="6" fill="currentColor" opacity="0.12" />
      <rect x="22" y="50" width="44" height="6" fill="currentColor" opacity="0.12" />
      <circle cx="70" cy="20" r="14" fill="var(--color-surface)" stroke="currentColor" stroke-width="2" opacity="0.45" />
      <path
        d="M70 14v8M66 18h8"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        opacity="0.45"
      />
    </svg>
    <p class="table-empty-title">{{ title }}</p>
    <p v-if="description" class="table-empty-desc">{{ description }}</p>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '试试调整筛选条件，或稍后再看' }
})
</script>

<style scoped>
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 16px;
  color: var(--color-primary);
}

.table-empty-art {
  width: 96px;
  height: 72px;
  margin-bottom: 10px;
}

.table-empty-art rect {
  rx: var(--radius);
}

.table-empty-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.table-empty-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
}
</style>
