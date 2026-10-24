<template>
  <div v-if="links.length" class="mm-links">
    <div v-for="item in links" :key="item.projectId" class="mm-link">
      <div class="mm-link-meta">
        <span class="mm-link-icon">导图</span>
        <div class="mm-link-text">
          <div class="mm-link-name">{{ item.name }}</div>
          <div class="mm-link-sub">
            {{ isCurrent(item) ? '当前导图已更新，关闭面板即可看到' : '思维导图已落库' }}
          </div>
        </div>
      </div>
      <button
        v-if="!isCurrent(item)"
        type="button"
        class="mm-btn primary"
        @click="openEdit(item)"
      >
        打开思维导图
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { extractMindmapLinks } from '@/modules/agent/utils/mindmapToolLink'

const props = defineProps({
  /** 消息上的 tools 数组 */
  tools: { type: Array, default: () => [] },
  /** 编辑页侧边对话绑定的当前导图 ID */
  currentProjectId: { type: [Number, String], default: null }
})

const router = useRouter()
const links = computed(() => extractMindmapLinks(props.tools))

function isCurrent(item) {
  if (props.currentProjectId == null || props.currentProjectId === '') return false
  return String(item.projectId) === String(props.currentProjectId)
}

function openEdit(item) {
  const path = item.editPath?.startsWith('/')
    ? item.editPath
    : `/mindmap/project/edit?id=${item.projectId}`
  const [pathname, qs] = path.split('?')
  const query = Object.fromEntries(new URLSearchParams(qs || ''))
  const { href } = router.resolve({ path: pathname, query })
  window.open(href, '_blank', 'noopener,noreferrer')
}
</script>

<style scoped>
.mm-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
.mm-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--color-border) 85%, transparent);
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-body) 45%, var(--color-surface));
}
.mm-link-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.mm-link-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(145deg, #16a34a, #4ade80);
}
.mm-link-text {
  min-width: 0;
}
.mm-link-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mm-link-sub {
  margin-top: 2px;
  font-size: 11px;
  color: var(--color-text-muted);
}
.mm-btn {
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  border-radius: 7px;
  border: 1px solid color-mix(in srgb, var(--color-primary) 45%, transparent);
  background: color-mix(in srgb, var(--color-primary) 12%, var(--color-surface));
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.mm-btn:hover {
  border-color: color-mix(in srgb, var(--color-primary) 60%, transparent);
}
</style>
