<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="菜单名称">
          <n-input v-model:value="query.keyword" placeholder="菜单名称关键字" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable
      :key="tableKey"
      :pager="pager"
      :columns="columns"
      :default-expand-all="expandedAll"
    >
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:menu:add'" type="primary" @click="openCreate(null)">
            新增菜单
          </n-button>
          <n-button secondary @click="toggleExpandAll">
            {{ expandedAll ? '折叠全部' : '展开全部' }}
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑菜单' : '新增菜单'"
      size="lg"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="上级菜单" path="parentId">
          <n-tree-select
            v-model:value="form.parentId"
            :options="parentOptions"
            key-field="id"
            label-field="menuName"
            children-field="children"
            placeholder="不选表示顶级菜单"
            clearable
            default-expand-all
          />
        </n-form-item>
        <n-form-item label="菜单类型" path="menuType">
          <n-radio-group v-model:value="form.menuType" :disabled="!!form.id">
            <n-radio-button v-for="o in menuTypeOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="菜单名称" path="menuName">
          <n-input v-model:value="form.menuName" placeholder="请输入菜单名称" />
        </n-form-item>

        <template v-if="form.menuType !== 'F'">
          <n-form-item v-if="form.menuType === 'C'" label="打开方式">
            <n-radio-group v-model:value="form.openMode">
              <n-radio-button value="route">内部页面</n-radio-button>
              <n-radio-button value="link">外链</n-radio-button>
            </n-radio-group>
          </n-form-item>

          <n-form-item
            v-if="form.menuType === 'C' && form.openMode === 'link'"
            label="外链地址"
            path="path"
          >
            <n-input v-model:value="form.path" placeholder="如 https://example.com/docs" />
          </n-form-item>

          <template v-else>
            <n-form-item label="路由地址" path="path">
              <n-input v-model:value="form.path" placeholder="如 /system/user" />
            </n-form-item>
            <n-form-item v-if="form.menuType === 'C'" label="组件路径" path="component">
              <n-input
                v-model:value="form.component"
                placeholder="如 system/user/index（相对 src/views）"
              />
            </n-form-item>
          </template>

          <n-form-item label="菜单图标">
            <IconPicker v-model="form.icon" />
          </n-form-item>
          <n-form-item label="显示状态">
            <n-radio-group v-model:value="form.visible">
              <n-radio :value="1">显示</n-radio>
              <n-radio :value="0">隐藏</n-radio>
            </n-radio-group>
          </n-form-item>
        </template>

        <n-form-item v-if="form.menuType !== 'M'" label="权限标识">
          <n-input v-model:value="form.perms" placeholder="如 system:user:add" />
        </n-form-item>

        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="form.sort" :min="0" :max="9999" style="width: 160px" />
        </n-form-item>
        <n-form-item label="状态">
            <n-radio-group v-model:value="form.status">
              <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
                {{ o.label }}
              </n-radio>
            </n-radio-group>
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { NIcon, NTag, useMessage } from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import IconPicker from '@/components/IconPicker.vue'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { resolveIcon } from '@/utils/icon'
import { createMenu, deleteMenu, getMenuTree, updateMenu } from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_MENU_TYPE, DICT_NORMAL_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const expandedAll = ref(true)
const tableKey = ref(0)

function detectOpenMode(row) {
  const comp = String(row?.component || '')
  const path = String(row?.path || '')
  if (comp.toLowerCase().startsWith('link') || /^https?:\/\//i.test(path)) return 'link'
  return 'route'
}

function resolveLinkPath(row) {
  const comp = String(row?.component || '')
  if (comp.toLowerCase().startsWith('link:')) {
    const url = comp.slice(5).trim()
    if (url) return url
  }
  return row?.path || ''
}

const pager = usePagination(
  async (q) => {
    const res = await getMenuTree(q)
    const list = res.data || []
    if (expandedAll.value) tableKey.value += 1
    return { records: list, total: list.length }
  },
  { paged: false, defaults: { keyword: '', status: null } }
)
const { query, loadData } = pager

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate: openFormCreate,
  openEdit: openFormEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    parentId: 0,
    menuName: '',
    menuType: 'C',
    openMode: 'route',
    path: '',
    component: '',
    perms: '',
    icon: null,
    sort: 0,
    visible: 1,
    status: 1
  },
  async onSubmit(payload) {
    if (payload.id && payload.parentId === payload.id) {
      message.error('上级菜单不能是自己')
      throw new Error('invalid parent')
    }
    const data = {
      id: payload.id,
      parentId: payload.parentId ?? 0,
      menuName: payload.menuName,
      menuType: payload.menuType,
      path: payload.path || '',
      component: payload.component || '',
      perms: payload.perms || '',
      icon: payload.icon || '',
      sort: payload.sort ?? 0,
      visible: payload.visible ?? 1,
      status: payload.status ?? 1
    }
    if (payload.menuType === 'C' && payload.openMode === 'link') {
      data.component = 'Link'
    } else if (payload.menuType === 'M') {
      data.component = ''
    }
    if (data.id) {
      await updateMenu(data)
      message.success('修改成功')
    } else {
      await createMenu(data)
      message.success('新增成功')
    }
    loadData()
  }
})

/** 状态文案取自字典：筛选下拉与列表标签同源 */
const { options: statusOptions, labelOf: statusLabel } = useDict(DICT_NORMAL_DISABLE, {
  numeric: true
})

/** 菜单类型文案取自字典；标签颜色仍按码值（样式不属于字典） */
const { options: menuTypeOptions, labelOf: menuTypeLabel } = useDict(DICT_MENU_TYPE)

const MENU_TYPE_TAG_TYPES = {
  M: 'default',
  C: 'info',
  F: 'warning'
}

const columns = computed(() => [
  {
    title: '菜单名称',
    key: 'menuName',
    width: 220,
    fixed: 'left',
    render: (row) =>
      h(
        'span',
        { style: { display: 'inline-flex', alignItems: 'center', gap: '6px' } },
        [
          row.icon
            ? h(NIcon, {
                component: resolveIcon(row.icon),
                size: 16,
                style: { color: 'var(--color-text-secondary)', flexShrink: 0 }
              })
            : null,
          row.menuName
        ]
      )
  },
  {
    title: '类型',
    key: 'menuType',
    width: 80,
    render: (row) =>
      h(
        NTag,
        { type: MENU_TYPE_TAG_TYPES[row.menuType] || 'default', bordered: false },
        { default: () => menuTypeLabel(row.menuType) }
      )
  },
  { title: '排序', key: 'sort', width: 70 },
  { title: '路由/外链', key: 'path', width: 180, ellipsis: { tooltip: true } },
  { title: '组件路径', key: 'component', width: 160, ellipsis: { tooltip: true } },
  { title: '权限标识', key: 'perms', width: 180, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', bordered: false },
        { default: () => statusLabel(row.status) }
      )
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          row.menuType !== 'F' &&
            userStore.hasPerm('system:menu:add') &&
            hActionBtn({
              tip: '新增下级',
              type: 'success',
              icon: AddOutline,
              disabled: busy(row.id),
              onClick: () => openCreate(row)
            }),
          userStore.hasPerm('system:menu:edit') &&
            hActionBtn({
              tip: '编辑',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => openEdit(row)
            }),
          userStore.hasPerm('system:menu:remove') &&
            hActionConfirm({
              tip: '删除',
              confirm: `确定删除菜单「${row.menuName}」吗？其子菜单将一并删除。`,
              icon: TrashOutline,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

/** 上级菜单候选项：只保留目录与菜单，按钮不能作为父节点 */
const parentOptions = computed(() => [
  { id: 0, menuName: '顶级菜单', children: pruneButtons(pager.rows.value) }
])

function pruneButtons(nodes = []) {
  return nodes
    .filter((n) => n.menuType !== 'F')
    .map((n) => ({
      ...n,
      children: n.children?.length ? pruneButtons(n.children) : undefined
    }))
}

function toggleExpandAll() {
  expandedAll.value = !expandedAll.value
  tableKey.value += 1
}

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: ['input', 'blur'] }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: ['change'] }],
  path: [
    {
      validator: (rule, value) => {
        if (form.menuType === 'F') return true
        if (!value) {
          return new Error(form.openMode === 'link' ? '请输入外链地址' : '请输入路由地址')
        }
        if (form.menuType === 'C' && form.openMode === 'link') {
          return /^https?:\/\//i.test(value) || new Error('外链需以 http:// 或 https:// 开头')
        }
        return /^\//.test(value) || new Error('路由地址需以 / 开头')
      },
      trigger: ['input', 'blur']
    }
  ]
}

function openCreate(parent) {
  openFormCreate({
    id: null,
    parentId: parent ? parent.id : 0,
    menuName: '',
    menuType: parent && parent.menuType === 'C' ? 'F' : 'C',
    openMode: 'route',
    path: '',
    component: '',
    perms: '',
    icon: null,
    sort: 0,
    visible: 1,
    status: 1
  })
}

function openEdit(row) {
  openFormEdit(row, (r) => {
    const openMode = detectOpenMode(r)
    return {
      id: r.id,
      parentId: r.parentId ?? 0,
      menuName: r.menuName,
      menuType: r.menuType,
      openMode,
      path: openMode === 'link' ? resolveLinkPath(r) : r.path || '',
      component: openMode === 'route' ? r.component || '' : '',
      perms: r.perms || '',
      icon: r.icon || null,
      sort: r.sort ?? 0,
      visible: r.visible ?? 1,
      status: r.status ?? 1
    }
  })
}

function handleDelete(row) {
  return deleteMenu(row.id).then(() => {
    message.success('删除成功')
    loadData()
  })
}
</script>
