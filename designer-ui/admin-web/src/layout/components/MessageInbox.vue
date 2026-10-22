<template>
  <div class="message-inbox">
    <n-badge
      :value="noticeStore.unreadCount"
      :max="99"
      :show-zero="false"
      :offset="[-4, 4]"
    >
      <n-button quaternary circle @click="openDrawer">
        <template #icon>
          <n-icon :component="NotificationsOutline" :size="20" />
        </template>
      </n-button>
    </n-badge>

    <n-drawer v-model:show="drawerVisible" :width="400" placement="right">
      <n-drawer-content closable>
        <template #header>
          <div class="inbox-header">
            <span class="inbox-title">消息通知</span>
            <n-button
              text
              type="primary"
              :disabled="!noticeStore.unreadCount"
              :loading="markingAll"
              @click="handleMarkAll"
            >
              一键已读
            </n-button>
          </div>
        </template>

        <n-spin :show="noticeStore.inboxLoading">
          <TableEmpty
            v-if="!noticeStore.inboxRows.length"
            title="暂无消息"
            description="有新通知时会出现在这里"
          />
          <div v-else class="inbox-list">
            <div
              v-for="item in noticeStore.inboxRows"
              :key="item.id"
              class="inbox-item"
              :class="{ unread: !item.readFlag }"
              @click="openDetail(item)"
            >
              <div class="inbox-item-top">
                <span class="title">{{ item.title }}</span>
                <span class="time">{{ item.createTime }}</span>
              </div>
              <div class="preview">{{ item.content || '（无正文）' }}</div>
              <div v-if="resolveNoticeLink(item)" class="inbox-item-actions" @click.stop>
                <n-button text type="primary" @click="goNoticeLink(item)">
                  前往处理
                </n-button>
              </div>
            </div>
          </div>
        </n-spin>

        <template #footer>
          <div class="inbox-footer">
            <PaginationBar
              :page="noticeStore.inboxQuery.pageNum"
              :page-size="noticeStore.inboxQuery.pageSize"
              :item-count="noticeStore.inboxTotal"
              simple
              @update:page="onInboxPage"
            />
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <n-modal
      v-model:show="detailVisible"
      preset="card"
      :title="detail?.title || '消息详情'"
      style="width: 520px"
      :mask-closable="false"
    >
      <n-space vertical>
        <span class="meta">{{ detail?.createTime }}</span>
        <div class="content">{{ detail?.content || '（无正文）' }}</div>
      </n-space>
      <template #footer>
        <n-space justify="end">
          <n-button
            v-if="detailLink"
            type="primary"
            secondary
            @click="goNoticeLink(detail)"
          >
            前往处理
          </n-button>
          <n-button type="primary" @click="detailVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NotificationsOutline } from '@vicons/ionicons5'
import { useNoticeStore } from '@/store/notice'
import { resolveNoticeLink } from '@/utils/noticeLink'
import TableEmpty from '@/components/TableEmpty.vue'

const router = useRouter()
const noticeStore = useNoticeStore()
const drawerVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const markingAll = ref(false)

const detailLink = computed(() => resolveNoticeLink(detail.value))

noticeStore.setDrawerOpenGetter(() => drawerVisible.value)

async function openDrawer() {
  drawerVisible.value = true
  await noticeStore.loadInbox(true)
}

function onInboxPage(page) {
  noticeStore.inboxQuery.pageNum = page
  noticeStore.loadInbox()
}

async function openDetail(item) {
  detail.value = item
  detailVisible.value = true
  if (!item.readFlag) {
    try {
      await noticeStore.markRead(item.id)
    } catch {
      // ignore
    }
  }
}

async function goNoticeLink(item) {
  const link = resolveNoticeLink(item)
  if (!link) return
  if (item?.id && !item.readFlag) {
    try {
      await noticeStore.markRead(item.id)
    } catch {
      // ignore
    }
  }
  detailVisible.value = false
  drawerVisible.value = false
  if (router.currentRoute.value.path !== link) {
    router.push(link)
  }
}

async function handleMarkAll() {
  if (!noticeStore.unreadCount || markingAll.value) return
  markingAll.value = true
  try {
    await noticeStore.markAllRead()
  } finally {
    markingAll.value = false
  }
}

watch(
  () => noticeStore.detailRequest,
  (item) => {
    if (item?.id) {
      openDetail(item)
    }
  }
)

watch(
  () => noticeStore.navigateRequest,
  (req) => {
    if (req?.path) {
      drawerVisible.value = false
      detailVisible.value = false
      if (router.currentRoute.value.path !== req.path) {
        router.push(req.path)
      }
    }
  }
)

onMounted(() => {
  noticeStore.startWatch()
})

onUnmounted(() => {
  noticeStore.stopWatch()
})
</script>

<style scoped>
.message-inbox {
  display: inline-flex;
  align-items: center;
  height: 34px;
  overflow: visible;
}

.message-inbox :deep(.n-badge) {
  display: inline-flex;
  align-items: center;
  line-height: 0;
}

.inbox-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding-right: 8px;
}

.inbox-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.inbox-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.inbox-item {
  padding: 12px;
  border-radius: var(--radius);
  background: var(--color-body);
  cursor: pointer;
  transition: background-color 0.15s;
}

.inbox-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.inbox-item.unread {
  background: color-mix(in srgb, var(--color-primary) 8%, var(--color-body));
}

.inbox-item.unread .title {
  font-weight: 600;
}

.inbox-item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.inbox-item-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.time,
.meta {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-3);
}

.title {
  font-size: 14px;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview {
  font-size: 12px;
  color: var(--color-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  white-space: pre-wrap;
  line-height: 1.6;
  max-height: 360px;
  overflow: auto;
}

.inbox-footer {
  display: flex;
  justify-content: center;
}
.inbox-footer :deep(.pagination-wrap) {
  margin-top: 0;
  padding-top: 0;
  justify-content: center;
}
</style>
