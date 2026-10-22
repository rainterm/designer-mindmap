<template>
  <div class="page-container agent-store">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="名称">
          <n-input v-model:value="query.name" placeholder="搜索智能体" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <div class="store-main">
      <n-card :bordered="false" class="list-card">
        <!-- <div class="store-toolbar">
          <span class="store-count">共 {{ total }} 个可用智能体</span>
        </div> -->

        <n-spin :show="loading" class="store-spin">
          <div v-if="!rows.length && !loading" class="empty-wrap">
            <TableEmpty
              title="暂无可用智能体"
              description="请联系管理员启用智能体后再来使用"
            />
          </div>

          <div v-else class="agent-grid">
            <article
              v-for="row in rows"
              :key="row.id"
              class="agent-card"
              :class="{ busy: busy(row.id) }"
              @click="onCardClick(row)"
            >
              <div class="card-head">
                <div
                  class="avatar"
                  :class="{ 'has-img': !!row.avatar && !locked(row.id, 'chat') }"
                  :style="row.avatar && !locked(row.id, 'chat') ? undefined : { background: avatarColor(row.name) }"
                >
                  <n-spin v-if="locked(row.id, 'chat')" :size="16" />
                  <img v-else-if="row.avatar" :src="row.avatar" alt="" class="avatar-img" />
                  <template v-else>{{ initials(row.name) }}</template>
                </div>
                <h3 class="name">{{ row.name }}</h3>
              </div>
              <p class="desc">{{ row.remark || '暂无介绍' }}</p>
            </article>
          </div>
        </n-spin>
      </n-card>

      <PaginationBar :pager="pager" hide-on-empty />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import TableEmpty from '@/components/TableEmpty.vue'
import { openSessionByAgent, pageStoreAgents } from '@/modules/agent/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useUserStore } from '@/store/user'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const AVATAR_COLORS = ['#1f6f54', '#2a6f97', '#6b4f3a', '#5a4a7a', '#3d6b5a', '#7a5c3a']

const pager = usePagination((q) => pageStoreAgents(q), {
  defaults: { name: '' }
})
const { query, rows, total, loading } = pager

function initials(name) {
  const s = String(name || '?').trim()
  return s.slice(0, 1).toUpperCase()
}

function avatarColor(name) {
  let h = 0
  for (const c of String(name || '')) h = (h * 31 + c.charCodeAt(0)) >>> 0
  return AVATAR_COLORS[h % AVATAR_COLORS.length]
}

function onCardClick(row) {
  if (!userStore.hasPerm('agent:session:chat') || busy(row.id)) return
  run(row.id, 'chat', () => handleUse(row))
}

function openChatTab(sessionId) {
  const { href } = router.resolve({ path: '/agent/session/chat', query: { id: sessionId } })
  window.open(href, '_blank', 'noopener,noreferrer')
}

async function handleUse(row) {
  try {
    const res = await openSessionByAgent(row.id)
    const id = res.data?.id
    if (!id) {
      message.error('打开对话失败')
      return
    }
    openChatTab(id)
  } catch (e) {
    message.error(e?.message || '打开对话失败')
  }
}
</script>

<style scoped>
.page-container.agent-store {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--layout-nav-height, 44px) - var(--layout-tags-height, 36px));
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}
.agent-store :deep(.search-card) {
  flex-shrink: 0;
}
.store-main {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.list-card {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.list-card :deep(.n-card__content) {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden;
}
.store-toolbar {
  margin-bottom: 12px;
  flex-shrink: 0;
}
.store-count {
  font-size: 13px;
  color: var(--color-text-muted);
}
.store-spin {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.list-card :deep(.n-spin-container),
.list-card :deep(.n-spin-content) {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  display: flex !important;
  flex-direction: column !important;
}
.agent-grid {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  align-content: start;
}
.agent-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  min-height: 120px;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.agent-card:hover {
  border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border));
  background: color-mix(in srgb, var(--color-primary) 4%, var(--color-surface));
}
.agent-card.busy {
  opacity: 0.7;
  cursor: wait;
  pointer-events: none;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.avatar {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  display: grid;
  place-items: center;
  overflow: hidden;
}
.avatar.has-img {
  background: var(--color-body);
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar :deep(.n-spin-body) {
  --n-color: #fff;
}
.name {
  margin: 0;
  min-width: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.empty-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.store-main > :deep(.pagination-wrap) {
  padding: 10px 10px 0;
  margin-top: 0;
}
</style>
