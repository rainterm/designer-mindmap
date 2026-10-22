<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="角色名称">
          <n-input v-model:value="query.roleName" placeholder="角色名称" clearable />
        </n-form-item>
        <n-form-item label="权限字符">
          <n-input v-model:value="query.roleKey" placeholder="如 editor" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:role:add'" type="primary" @click="openCreate">
            新增角色
          </n-button>
          <n-button
            v-perm="'system:role:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="90">
        <n-form-item label="角色名称" path="roleName">
          <n-input v-model:value="form.roleName" placeholder="请输入角色名称" />
        </n-form-item>
        <n-form-item label="权限字符" path="roleKey">
          <n-input
            v-model:value="form.roleKey"
            placeholder="如 auditor，权限校验用"
            :disabled="isBuiltin(form)"
          />
        </n-form-item>
        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="form.sort" :min="0" :max="9999" style="width: 160px" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="数据权限" path="dataScope">
          <n-select
            v-model:value="form.dataScope"
            :options="dataScopeOptions"
            @update:value="onDataScopeChange"
          />
        </n-form-item>
        <n-form-item
          v-if="form.dataScope === 2"
          label="可见部门"
          path="deptIds"
          :show-feedback="true"
        >
          <n-tree-select
            v-model:value="form.deptIds"
            :options="deptTreeOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            multiple
            checkable
            cascade
            default-expand-all
            placeholder="请选择可见部门"
          />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>

    <Dialog
      v-model:show="permVisible"
      :title="`分配权限 - ${permRole.roleName || ''}`"
      :loading="permSubmitting"
      @confirm="handleAssignSubmit"
    >
      <n-spin :show="permLoading">
        <div class="perm-toolbar">
          <n-space>
            <n-button secondary @click="handleExpandAll">
              展开/折叠
            </n-button>
            <n-button secondary @click="handleCheckAll">
              全选/全不选
            </n-button>
            <n-button secondary @click="handleSelectLeaf">
              父子联动
            </n-button>
          </n-space>
        </div>
        <n-tree
          ref="menuTreeRef"
          block-line
          cascade
          checkable
          :data="menuTreeData"
          :checked-keys="checkedMenuKeys"
          :indeterminate-keys="halfCheckedMenuKeys"
          :expanded-keys="expandedMenuKeys"
          key-field="key"
          label-field="label"
          children-field="children"
          @update:checked-keys="onCheckedKeys"
          @update:indeterminate-keys="onHalfCheckedKeys"
          @update:expanded-keys="(keys) => (expandedMenuKeys = keys)"
        />
      </n-spin>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, reactive, ref } from 'vue'
import { NSpace, NSwitch, NTag, NTooltip, useMessage } from 'naive-ui'
import { CreateOutline, KeyOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  assignRoleMenus,
  createRole,
  deleteRoles,
  getDeptOptions,
  getRoleDeptIds,
  getRoleMenuIds,
  getMenuTree,
  pageRoles,
  updateRole
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_DATA_SCOPE, DICT_NORMAL_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

/** 数据权限档位取自字典（编码与后端 DataScopeType 一一对应） */
const { options: dataScopeOptions, labelOf: dataScopeLabel } = useDict(DICT_DATA_SCOPE, {
  numeric: true
})

/** 部门树（数据权限「自定义部门」用）；按当前账号的数据权限过滤，超管看到全量 */
const deptTreeOptions = ref([])
async function loadDeptTree() {
  if (deptTreeOptions.value.length) return
  const res = await getDeptOptions()
  deptTreeOptions.value = res.data || []
}

/** 切到「自定义部门」时才需要部门树，避免每次打开角色弹框都多一次请求 */
function onDataScopeChange(value) {
  if (Number(value) === 2) {
    loadDeptTree()
  }
}

/** 是否内置角色（roleType === 1，不可修改/删除） */
const isBuiltin = (row) => Number(row?.roleType) === 1
/** 是否超级管理员角色（isSuper === 1，跳过权限校验） */
const isSuper = (row) => Number(row?.isSuper) === 1

const pager = usePagination((q) => pageRoles(q), {
  defaults: {
    roleName: '',
    roleKey: '',
    status: null
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteRoles, {
  confirm: (n) => `确定删除选中的 ${n} 个角色吗？`,
  filter: (ids, list) =>
    ids.filter((id) => {
      const row = list.find((r) => r.id === id)
      return !(isBuiltin(row) || row?.id === 1)
    }),
  emptyWarning: '选中的均为内置角色，不可删除'
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate,
  openEdit: openEditDialog,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    roleName: '',
    roleKey: '',
    sort: 0,
    status: 1,
    dataScope: 1,
    deptIds: [],
    remark: ''
  },
  async onSubmit(payload) {
    // 只有「自定义部门」才需要带部门；其余档位传空数组，后端会顺带清掉历史关联
    const body = {
      ...payload,
      deptIds: Number(payload.dataScope) === 2 ? payload.deptIds : []
    }
    if (payload.id) {
      await updateRole(body)
      message.success('修改成功')
    } else {
      await createRole(body)
      message.success('新增成功')
    }
    loadData()
  }
})

/**
 * 打开编辑弹框：数据权限的部门回显要单独拉一次接口，所以先同步开框、再异步填部门。
 */
function openEdit(row, mapper) {
  openEditDialog(row, (r) => ({ ...mapper(r), dataScope: Number(r.dataScope ?? 1), deptIds: [] }))
  if (Number(row.dataScope) === 2) {
    fillRoleDepts(row.id)
  }
}

async function fillRoleDepts(roleId) {
  try {
    await loadDeptTree()
    const res = await getRoleDeptIds(roleId)
    // 防串台：异步期间用户可能已经点了另一个角色
    if (form.id === roleId) {
      form.deptIds = res.data || []
    }
  } catch {
    // 部门树加载失败不阻塞表单，用户重新选择即可
    if (form.id === roleId) {
      form.deptIds = []
    }
  }
}

/** 状态筛选项取自字典 */
const { options: statusOptions } = useDict(DICT_NORMAL_DISABLE, { numeric: true })

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: ['input', 'blur'] }],
  roleKey: [
    { required: true, message: '请输入权限字符', trigger: ['input', 'blur'] },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '仅支持字母、数字与下划线', trigger: ['input', 'blur'] }
  ]
}

const columns = computed(() => [
  { type: 'selection', width: 44, disabled: (row) => isBuiltin(row) },
  { title: 'ID', key: 'id', width: 70 },
  { title: '角色名称', key: 'roleName', width: 150 },
  {
    title: '权限字符',
    key: 'roleKey',
    width: 130,
    render: (row) => h(NTag, { type: 'primary', bordered: false }, { default: () => row.roleKey })
  },
  {
    title: '类型',
    key: 'roleType',
    width: 130,
    render: (row) => {
      const tags = []
      if (isBuiltin(row)) {
        tags.push(h(NTag, { type: 'error', bordered: false }, { default: () => '内置' }))
      } else {
        tags.push(h(NTag, { type: 'default', bordered: false }, { default: () => '自定义' }))
      }
      if (isSuper(row)) {
        tags.push(h(NTag, { type: 'warning', bordered: false }, { default: () => '超管' }))
      }
      return h(NSpace, { size: 4 }, { default: () => tags })
    }
  },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '数据权限',
    key: 'dataScope',
    width: 120,
    render: (row) => dataScopeLabel(row.dataScope)
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => {
      const builtin = isBuiltin(row)
      const sw = h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: builtin || !userStore.hasPerm('system:role:edit') || busy(row.id),
        'onUpdate:value': (val) => run(row.id, 'status', () => handleStatusToggle(row, val ? 1 : 0))
      })
      return builtin
        ? h(NTooltip, {}, {
            trigger: () => sw,
            default: () => '内置角色不允许修改'
          })
        : sw
    }
  },
  { title: '备注', key: 'remark', width: 180, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 140,
    fixed: 'right',
    render: (row) => {
      const builtin = isBuiltin(row)
      const canEdit = userStore.hasPerm('system:role:edit')
      const canRemove = userStore.hasPerm('system:role:remove')
      const rowBusy = busy(row.id)

      const editBtn = canEdit
        ? hActionBtn({
            tip: builtin ? '内置角色不允许修改' : '编辑',
            icon: CreateOutline,
            disabled: builtin || rowBusy,
            onClick: () =>
              openEdit(row, (r) => ({
                id: r.id,
                roleName: r.roleName,
                roleKey: r.roleKey,
                sort: r.sort ?? 0,
                status: r.status,
                roleType: r.roleType,
                remark: r.remark
              }))
          })
        : null

      const permBtn = canEdit
        ? hActionBtn({
            tip: isSuper(row) ? '超级管理员默认拥有全部权限' : '分配权限',
            type: 'success',
            icon: KeyOutline,
            loading: locked(row.id, 'perm'),
            disabled: isSuper(row) || rowBusy,
            onClick: () => run(row.id, 'perm', () => openPerm(row))
          })
        : null

      const delBtn = canRemove
        ? hActionConfirm({
            tip: builtin ? '内置角色不允许删除' : '删除',
            confirm: `确定删除角色「${row.roleName}」吗？`,
            icon: TrashOutline,
            disabled: builtin || rowBusy,
            loading: locked(row.id, 'delete'),
            onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
          })
        : null

      return hActionGroup([editBtn, permBtn, delBtn])
    }
  }
])

async function handleStatusToggle(row, status) {
  try {
    await updateRole({ ...row, status })
    row.status = status
    message.success(status === 1 ? '已启用' : '已停用')
  } catch (e) {
    loadData()
  }
}

function handleDelete(row) {
  return deleteRoles([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

const permVisible = ref(false)
const permLoading = ref(false)
const permSubmitting = ref(false)
const permRole = reactive({ id: null, roleName: '' })
const menuTreeRef = ref(null)
const rawMenuTree = ref([])
const menuTreeData = ref([])
const checkedMenuKeys = ref([])
const halfCheckedMenuKeys = ref([])
const expandedMenuKeys = ref([])

function toTreeData(nodes) {
  return (nodes || []).map((node) => ({
    key: node.id,
    label: node.menuName,
    children: node.children?.length ? toTreeData(node.children) : undefined
  }))
}

function collectExpandKeys(nodes, acc = []) {
  for (const node of nodes || []) {
    if (node.children?.length) {
      acc.push(node.id)
      collectExpandKeys(node.children, acc)
    }
  }
  return acc
}

function pickLeafKeys(ids, nodes) {
  const wanted = new Set(ids)
  const result = []
  const walk = (list) =>
    (list || []).forEach((node) => {
      if (node.children?.length) {
        walk(node.children)
      } else if (wanted.has(node.id)) {
        result.push(node.id)
      }
    })
  walk(nodes)
  const known = new Set(flattenKeys(nodes))
  ids.forEach((id) => {
    if (!known.has(id)) result.push(id)
  })
  return result
}

function flattenKeys(nodes, acc = []) {
  for (const node of nodes || []) {
    acc.push(node.id)
    if (node.children?.length) flattenKeys(node.children, acc)
  }
  return acc
}

async function loadMenuTree() {
  if (rawMenuTree.value.length) return rawMenuTree.value
  const res = await getMenuTree()
  rawMenuTree.value = res.data || []
  menuTreeData.value = toTreeData(rawMenuTree.value)
  expandedMenuKeys.value = collectExpandKeys(rawMenuTree.value)
  return rawMenuTree.value
}

async function openPerm(row) {
  permRole.id = row.id
  permRole.roleName = row.roleName
  permVisible.value = true
  permLoading.value = true
  try {
    const tree = await loadMenuTree()
    const res = await getRoleMenuIds(row.id)
    checkedMenuKeys.value = pickLeafKeys(res.data || [], tree)
    halfCheckedMenuKeys.value = []
  } finally {
    permLoading.value = false
  }
}

function onCheckedKeys(keys) {
  checkedMenuKeys.value = keys
}

function onHalfCheckedKeys(keys) {
  halfCheckedMenuKeys.value = keys
}

function handleExpandAll() {
  const all = flattenKeys(rawMenuTree.value)
  expandedMenuKeys.value = expandedMenuKeys.value.length ? [] : all
}

function handleCheckAll() {
  const all = flattenKeys(rawMenuTree.value)
  checkedMenuKeys.value = checkedMenuKeys.value.length ? [] : all
  halfCheckedMenuKeys.value = []
}

function handleSelectLeaf() {
  message.info('菜单树已启用父子联动')
}

async function handleAssignSubmit() {
  const ids = Array.from(new Set([...checkedMenuKeys.value, ...halfCheckedMenuKeys.value]))
  permSubmitting.value = true
  try {
    await assignRoleMenus(permRole.id, ids)
    message.success('权限分配成功')
    permVisible.value = false
  } finally {
    permSubmitting.value = false
  }
}
</script>

<style scoped>
.perm-toolbar {
  margin-bottom: 8px;
}
</style>
