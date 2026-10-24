<template>
  <div class="list-page">
    <header class="list-hero">
      <div class="title-row">
        <h1>思维导图</h1>
        <n-dropdown :options="userOptions" trigger="click" @select="onUserSelect">
          <n-button quaternary class="user-btn">{{ userStore.nickname }}</n-button>
        </n-dropdown>
      </div>
    </header>
    <div class="list-body">
      <aside class="cat-side">
        <div class="cat-head">
          <span>分类</span>
          <n-button
            v-if="userStore.hasPerm('mindmap:project:add')"
            quaternary
            @click="openAddCategory(null)"
          >
            新建
          </n-button>
        </div>
        <div class="cat-scroll">
          <button
            type="button"
            class="cat-item"
            :class="{ active: activeCategory === 'all' }"
            @click="selectCategory('all')"
          >
            <span>全部</span>
            <span class="cat-count">{{ totalAll }}</span>
          </button>
          <button
            type="button"
            class="cat-item"
            :class="{ active: activeCategory === 'none' }"
            @click="selectCategory('none')"
          >
            <span>未分类</span>
            <span class="cat-count">{{ uncategorizedCount }}</span>
          </button>
          <div v-for="node in flatCategories" :key="node.id" class="cat-row">
            <div
              class="cat-item"
              :class="{ active: activeCategory === node.id }"
              :style="{ paddingLeft: `${10 + node.depth * 14}px` }"
              @click="selectCategory(node.id)"
            >
              <span class="cat-main">
                <span class="cat-name">{{ node.name }}</span>
                <n-dropdown
                  v-if="canManageCategory"
                  :options="categoryMenuOptions"
                  trigger="click"
                  placement="bottom-start"
                  @select="(key) => onCategoryMenu(key, node)"
                >
                  <button type="button" class="cat-more" title="分类操作" @click.stop>
                    <n-icon :component="EllipsisHorizontal" :size="16" />
                  </button>
                </n-dropdown>
              </span>
              <span class="cat-count">{{ node.totalCount || 0 }}</span>
            </div>
          </div>
        </div>
      </aside>
      <n-card :bordered="false" class="list-card" content-style="display:flex;flex-direction:column;height:100%;min-height:0;padding:16px;">
        <div class="list-toolbar">
          <n-input
            v-model:value="query.name"
            clearable
            placeholder="搜索导图名称"
            style="max-width: 280px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <n-button @click="handleSearch">查询</n-button>
          <n-button v-if="userStore.hasPerm('mindmap:project:add')" type="primary" @click="openCreate">
            新建导图
          </n-button>
        </div>
        <div class="table-wrap">
          <n-data-table
            bordered
            :columns="columns"
            :data="rows"
            :loading="loading"
            :row-key="(row) => row.id"
            :checked-row-keys="checkedRowKeys"
            flex-height
            style="height: 100%"
            @update:checked-row-keys="(keys) => (checkedRowKeys = keys)"
          />
        </div>
        <div class="list-footer">
          <n-button
            v-if="userStore.hasPerm('mindmap:project:remove') && checkedRowKeys.length"
            type="error"
            @click="handleBatchDelete"
          >
            删除选中（{{ checkedRowKeys.length }}）
          </n-button>
          <span v-else />
          <PaginationBar
            :page="query.pageNum"
            :page-size="query.pageSize"
            :item-count="total"
            :page-sizes="[10, 20, 50]"
            @update:page="onPage"
            @update:page-size="onPageSize"
          />
        </div>
      </n-card>
    </div>
    <n-modal
      v-model:show="visible"
      preset="dialog"
      title="新建导图"
      positive-text="创建"
      :loading="submitting"
      @positive-click="submit"
    >
      <div class="form-stack">
        <n-input
          v-model:value="formName"
          placeholder="请输入导图名称"
          maxlength="100"
          show-count
          @keydown.enter.prevent="submit"
        />
        <n-select
          v-model:value="formCategoryId"
          :options="categorySelectOptions"
          placeholder="选择分类（可选）"
          clearable
          filterable
        />
      </div>
    </n-modal>
    <n-modal
      v-model:show="moveVisible"
      preset="dialog"
      title="移动到分类"
      positive-text="确定"
      :loading="moving"
      @positive-click="submitMove"
    >
      <n-select
        v-model:value="moveCategoryId"
        :options="categorySelectOptions"
        placeholder="选择分类，清空为未分类"
        clearable
        filterable
      />
    </n-modal>
    <n-modal
      v-model:show="catVisible"
      preset="dialog"
      :title="catEditId ? '编辑分类' : '新建分类'"
      positive-text="保存"
      :loading="catSubmitting"
      @positive-click="submitCategory"
    >
      <div class="form-stack">
        <n-input
          v-model:value="catName"
          placeholder="分类名称，如：读书笔记"
          maxlength="50"
          show-count
          @keydown.enter.prevent="submitCategory"
        />
        <n-select
          v-model:value="catParentId"
          :options="parentSelectOptions"
          placeholder="上级分类（不选则为顶级）"
          clearable
          filterable
        />
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NIcon, NPopconfirm, NTooltip, useDialog, useMessage } from 'naive-ui'
import {
  CreateOutline,
  EllipsisHorizontal,
  FolderOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { createProject, deleteProjects, pageProjects, updateProject } from '@/api/project'
import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory
} from '@/api/category'
import PaginationBar from '@/components/PaginationBar.vue'
import { useUserStore } from '@/store/user'
import { adminWebUrl } from '@/settings'
import {
  flattenCategories,
  sumTotalCount,
  toSelectOptions
} from '@/utils/categoryTree'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()

/** 'all' | 'none' | number */
const activeCategory = ref('all')
const categoryTree = ref([])
const uncategorizedCount = ref(0)

const query = reactive({ name: '', pageNum: 1, pageSize: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const checkedRowKeys = ref([])
const visible = ref(false)
const submitting = ref(false)
const formName = ref('')
const formCategoryId = ref(null)

const moveVisible = ref(false)
const moving = ref(false)
const moveRowId = ref(null)
const moveCategoryId = ref(null)

const catVisible = ref(false)
const catSubmitting = ref(false)
const catEditId = ref(null)
const catName = ref('')
const catParentId = ref(null)

const userOptions = [
  { label: '打开管理后台', key: 'admin' },
  { label: '退出登录', key: 'logout' }
]

const flatCategories = computed(() => flattenCategories(categoryTree.value))

const categorySelectOptions = computed(() => toSelectOptions(categoryTree.value))

const parentSelectOptions = computed(() =>
  toSelectOptions(categoryTree.value, { excludeId: catEditId.value })
)

const canManageCategory = computed(
  () =>
    userStore.hasPerm('mindmap:project:add') ||
    userStore.hasPerm('mindmap:project:edit') ||
    userStore.hasPerm('mindmap:project:remove')
)

const categoryMenuOptions = computed(() => {
  const opts = []
  if (userStore.hasPerm('mindmap:project:add')) {
    opts.push({ label: '加子分类', key: 'add-child' })
  }
  if (userStore.hasPerm('mindmap:project:edit')) {
    opts.push({ label: '编辑', key: 'edit' })
  }
  if (userStore.hasPerm('mindmap:project:remove')) {
    opts.push({
      label: '删除',
      key: 'delete',
      props: { style: 'color: var(--n-error-color)' }
    })
  }
  return opts
})

const totalAll = computed(
  () => sumTotalCount(categoryTree.value) + (uncategorizedCount.value || 0)
)

async function loadCategories() {
  const res = await listCategories()
  const data = res.data || {}
  categoryTree.value = data.categories || []
  uncategorizedCount.value = data.uncategorizedCount || 0
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      name: query.name || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (activeCategory.value === 'none') {
      params.categoryId = 0
    } else if (typeof activeCategory.value === 'number') {
      params.categoryId = activeCategory.value
    }
    const res = await pageProjects(params)
    const data = res.data || {}
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function onPage(page) {
  query.pageNum = page
  loadData()
}

function onPageSize(size) {
  query.pageSize = size
  query.pageNum = 1
  loadData()
}

async function refreshAll() {
  await loadCategories()
  await loadData()
}

function selectCategory(key) {
  activeCategory.value = key
  query.pageNum = 1
  loadData()
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function goEdit(id) {
  router.push({ path: '/edit', query: { id } })
}

function openCreate() {
  formName.value = ''
  formCategoryId.value =
    typeof activeCategory.value === 'number' ? activeCategory.value : null
  visible.value = true
}

async function submit() {
  const name = formName.value.trim()
  if (!name) {
    message.warning('请输入导图名称')
    return false
  }
  submitting.value = true
  try {
    const payload = { name }
    if (formCategoryId.value) {
      payload.categoryId = formCategoryId.value
    } else {
      payload.clearCategory = true
    }
    const res = await createProject(payload)
    visible.value = false
    message.success('已创建')
    goEdit(res.data.id)
  } finally {
    submitting.value = false
  }
  return false
}

function openMove(row) {
  moveRowId.value = row.id
  moveCategoryId.value = row.categoryId || null
  moveVisible.value = true
}

async function submitMove() {
  moving.value = true
  try {
    const payload = { id: moveRowId.value }
    if (moveCategoryId.value) {
      payload.categoryId = moveCategoryId.value
    } else {
      payload.clearCategory = true
    }
    await updateProject(payload)
    moveVisible.value = false
    message.success('已移动')
    await refreshAll()
  } finally {
    moving.value = false
  }
  return false
}

async function handleDelete(row) {
  await deleteProjects([row.id])
  message.success('删除成功')
  checkedRowKeys.value = checkedRowKeys.value.filter((id) => id !== row.id)
  await refreshAll()
}

function handleBatchDelete() {
  const n = checkedRowKeys.value.length
  dialog.warning({
    title: '确认删除',
    content: `确定删除选中的 ${n} 个导图项目吗？`,
    positiveText: '删除',
    negativeText: '取消',
    async onPositiveClick() {
      await deleteProjects(checkedRowKeys.value)
      checkedRowKeys.value = []
      message.success('删除成功')
      await refreshAll()
    }
  })
}

function openAddCategory(parentId) {
  catEditId.value = null
  catName.value = ''
  catParentId.value = parentId || null
  catVisible.value = true
}

function openEditCategory(cat) {
  if (!cat) return
  catEditId.value = cat.id
  catName.value = cat.name
  catParentId.value = cat.parentId || null
  catVisible.value = true
}

function onCategoryMenu(key, node) {
  if (key === 'add-child') {
    openAddCategory(node.id)
    return
  }
  if (key === 'edit') {
    openEditCategory(node)
    return
  }
  if (key === 'delete') {
    handleDeleteCategory(node)
  }
}

async function submitCategory() {
  const name = catName.value.trim()
  if (!name) {
    message.warning('请输入分类名称')
    return false
  }
  catSubmitting.value = true
  try {
    const payload = {
      name,
      parentId: catParentId.value || 0
    }
    if (catEditId.value) {
      payload.id = catEditId.value
      await updateCategory(payload)
      message.success('已保存')
    } else {
      const res = await createCategory(payload)
      message.success('已创建分类')
      activeCategory.value = res.data.id
    }
    catVisible.value = false
    await refreshAll()
  } finally {
    catSubmitting.value = false
  }
  return false
}

function handleDeleteCategory(cat) {
  if (!cat) return
  dialog.warning({
    title: '删除分类',
    content: `确定删除「${cat.name}」吗？子分类会升到上一级，其中的导图变为未分类。`,
    positiveText: '删除',
    negativeText: '取消',
    async onPositiveClick() {
      await deleteCategory(cat.id)
      message.success('已删除分类')
      if (activeCategory.value === cat.id) {
        activeCategory.value = 'all'
      }
      await refreshAll()
    }
  })
}

async function onUserSelect(key) {
  if (key === 'admin') {
    window.open(adminWebUrl, '_blank', 'noopener')
    return
  }
  if (key === 'logout') {
    await userStore.logout()
    router.replace({ path: '/sso', query: { redirect: '/' } })
  }
}

function renderActionBtn({ tip, type, icon, onClick }) {
  return h(
    NTooltip,
    { placement: 'top' },
    {
      trigger: () =>
        h(
          NButton,
          {
            quaternary: true,
            circle: true,
            type: type || 'default',
            style: 'flex:none',
            onClick
          },
          {
            icon: () => h(NIcon, { component: icon, size: 18 })
          }
        ),
      default: () => tip
    }
  )
}

const columns = [
  { type: 'selection', width: 44 },
  { title: '名称', key: 'name', minWidth: 180, ellipsis: { tooltip: true } },
  {
    title: '分类',
    key: 'categoryName',
    width: 140,
    ellipsis: { tooltip: true },
    render: (row) => row.categoryName || '未分类'
  },
  { title: '更新时间', key: 'updateTime', width: 180, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    align: 'center',
    width: 148,
    render: (row) =>
      h(
        'div',
        {
          style:
            'display:inline-flex;gap:4px;justify-content:center;align-items:center;width:100%;box-sizing:border-box'
        },
        [
        userStore.hasPerm('mindmap:project:edit') &&
          renderActionBtn({
            tip: '编辑',
            type: 'primary',
            icon: CreateOutline,
            onClick: () => goEdit(row.id)
          }),
        userStore.hasPerm('mindmap:project:edit') &&
          renderActionBtn({
            tip: '分类',
            icon: FolderOutline,
            onClick: () => openMove(row)
          }),
        userStore.hasPerm('mindmap:project:remove') &&
          h(
            NTooltip,
            { placement: 'top' },
            {
              trigger: () =>
                h(
                  NPopconfirm,
                  { onPositiveClick: () => handleDelete(row) },
                  {
                    trigger: () =>
                      h(
                        NButton,
                        {
                          quaternary: true,
                          circle: true,
                          type: 'error',
                          style: 'flex:none'
                        },
                        {
                          icon: () => h(NIcon, { component: TrashOutline, size: 18 })
                        }
                      ),
                    default: () => `确定删除导图「${row.name}」吗？`
                  }
                ),
              default: () => '删除'
            }
          )
        ].filter(Boolean)
      )
  }
]

onMounted(refreshAll)
</script>

<style scoped>
.list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px 24px 16px;
  box-sizing: border-box;
}
.list-hero {
  flex: none;
  margin-bottom: 14px;
}
.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.list-hero h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
}
.user-btn {
  font-weight: 500;
}
.list-body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 14px;
  align-items: stretch;
}
.cat-side {
  width: 280px;
  flex: none;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px 10px;
  border-radius: var(--radius, 6px);
  background: var(--color-surface);
  border: 1px solid var(--color-border, #e2e8e4);
}
.cat-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  padding: 0 6px;
  font-weight: 600;
}
.cat-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
}
.cat-row {
  margin: 0;
}
.cat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  margin-bottom: 2px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary, #55605a);
  cursor: pointer;
  text-align: left;
}
.cat-item:hover {
  background: color-mix(in srgb, var(--color-primary) 8%, transparent);
}
.cat-item.active {
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  color: var(--color-primary);
  font-weight: 600;
}
.cat-main {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.cat-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cat-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 20px;
  height: 20px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-muted, #87918b);
  opacity: 0;
  cursor: pointer;
}
.cat-item:hover .cat-more,
.cat-item.active .cat-more {
  opacity: 1;
}
.cat-more:hover {
  background: color-mix(in srgb, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
}
.cat-count {
  flex: none;
  color: var(--color-text-muted, #87918b);
}
.cat-item.active .cat-count {
  color: var(--color-primary);
}
.list-card {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  background: var(--color-surface);
  display: flex;
  flex-direction: column;
}
.list-card :deep(.n-card__content) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.list-toolbar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.list-footer {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}
.list-footer :deep(.pagination-wrap) {
  margin-top: 0;
  padding-top: 0;
}
.form-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
