<template>
  <div class="page-container user-page">
    <div class="user-layout">
      <aside class="dept-aside">
        <n-card class="dept-card" :bordered="false">
          <div class="dept-head">
            <h3 class="dept-title">部门</h3>
            <n-button text type="primary" @click="clearDeptFilter">全部</n-button>
          </div>
          <n-input
            v-model:value="deptKeyword"
            class="dept-search"
            placeholder="搜索部门"
            clearable
          />
          <n-spin :show="deptLoading" class="dept-spin">
            <n-tree
              v-if="deptTreeData.length"
              block-line
              selectable
              default-expand-all
              :data="deptTreeData"
              :pattern="deptKeyword"
              :show-irrelevant-nodes="false"
              :selected-keys="selectedDeptKeys"
              key-field="id"
              label-field="deptName"
              children-field="children"
              @update:selected-keys="onDeptSelect"
            />
            <n-empty v-else description="暂无部门"/>
          </n-spin>
        </n-card>
      </aside>

      <div class="user-main">
        <SearchBar :pager="pager">
          <template #fields>
            <n-form-item label="账号">
              <n-input v-model:value="query.username" placeholder="登录账号" clearable />
            </n-form-item>
            <n-form-item label="昵称">
              <n-input v-model:value="query.nickname" placeholder="昵称" clearable />
            </n-form-item>
            <n-form-item label="状态">
              <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
            </n-form-item>
            <n-form-item label="角色">
              <n-select v-model:value="query.roleId" :options="roleSelectOptions" placeholder="全部" clearable />
            </n-form-item>
          </template>
        </SearchBar>

        <PageTable :pager="pager" :columns="columns">
          <template #toolbar-right>
            <n-space :wrap="false" :size="8">
              <n-button v-perm="'system:user:add'" type="primary" @click="handleOpenCreate">
                新增用户
              </n-button>
              <n-button
                v-perm="'system:user:remove'"
                type="error"
                :disabled="!checkedRowKeys.length"
                @click="handleBatchDelete"
              >
                批量删除
              </n-button>
              <n-button
                v-perm="'system:user:export'"
                :loading="fileDownloading"
                :disabled="fileDownloading"
                @click="handleExport"
              >
                导出
              </n-button>
              <n-dropdown
                v-perm="'system:user:import'"
                trigger="click"
                :options="importMenuOptions"
                @select="handleImportMenu"
              >
                <n-button :loading="importing || fileDownloading" :disabled="fileDownloading">
                  导入
                  <n-icon class="btn-caret" :component="ChevronDownOutline" />
                </n-button>
              </n-dropdown>
              <input
                ref="importFileRef"
                class="hidden-file"
                type="file"
                accept=".xlsx,.xls"
                @change="onImportFileChange"
              />
            </n-space>
          </template>
        </PageTable>
      </div>
    </div>

    <Dialog
      v-model:show="formVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="90">
        <n-form-item label="登录账号" path="username">
          <n-input v-model:value="form.username" placeholder="请输入登录账号" />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            :placeholder="form.id ? '留空表示不修改密码' : '请输入初始密码'"
          />
          <!-- 提示不要放进 n-form-item 的 #feedback 插槽：naive-ui 拿到该插槽后不再渲染校验错误 -->
          <div class="pwd-form__hint">{{ PASSWORD_HINT }}</div>
        </n-form-item>
        <n-form-item label="昵称" path="nickname">
          <n-input v-model:value="form.nickname" placeholder="请输入昵称" />
        </n-form-item>
        <n-form-item label="部门" path="deptId">
          <n-tree-select
            v-model:value="form.deptId"
            :options="deptOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            placeholder="请选择部门"
            clearable
            default-expand-all
          />
        </n-form-item>
        <n-form-item label="角色" path="roleIds">
          <n-select v-model:value="form.roleIds" multiple :options="roleSelectOptions" placeholder="请选择角色" />
        </n-form-item>
        <n-form-item label="手机号" path="phone">
          <n-input v-model:value="form.phone" placeholder="请输入手机号" />
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="form.email" placeholder="请输入邮箱" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>

    <Dialog
      v-model:show="pwdVisible"
      title="重置密码"
      :loading="pwdSubmitting"
      confirm-text="确定"
      @confirm="handleResetPwd"
    >
      <n-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-placement="left" label-width="80">
        <n-form-item label="用户">
          <n-input :value="pwdForm.username" disabled />
        </n-form-item>
        <n-form-item label="新密码" path="password">
          <n-input
            v-model:value="pwdForm.password"
            type="password"
            show-password-on="click"
            placeholder="请输入新密码"
          />
          <!-- 提示不要放进 n-form-item 的 #feedback 插槽：naive-ui 拿到该插槽后不再渲染校验错误 -->
          <div class="pwd-form__hint">{{ PASSWORD_HINT }}</div>
        </n-form-item>
        <!-- 重置密码由管理员代填，多一个确认框可以拦住「手抖输错、把别人的密码改成一个自己都不知道的值」 -->
        <n-form-item label="确认密码" path="confirmPassword">
          <n-input
            v-model:value="pwdForm.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="请再次输入新密码"
          />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref, watch } from 'vue'
import { NSpace, NSwitch, NTag, useMessage } from 'naive-ui'
import { CreateOutline, LockClosedOutline, TrashOutline, ChevronDownOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  changeUserStatus,
  createUser,
  deleteUsers,
  downloadUserImportTemplate,
  exportUsers,
  getDeptOptions,
  importUsers,
  pageUsers,
  resetUserPassword,
  getRoleOptions,
  updateUser
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import {
  PASSWORD_HINT,
  buildEqualToRule,
  buildPasswordRules
} from '@/constants/password'
import { useDict } from '@/composables/useDict'
import { DICT_NORMAL_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'
import { fileDownloading } from '@/utils/download'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const importing = ref(false)
const roleSelectOptions = ref([])
const deptOptions = ref([])
const deptTreeData = ref([])
const deptLoading = ref(false)
const deptKeyword = ref('')
const selectedDeptKeys = ref([])

const pager = usePagination((q) => pageUsers(q), {
  defaults: {
    username: '',
    nickname: '',
    status: null,
    roleId: null,
    deptId: null
  }
})
const { query, loadData, checkedRowKeys, getFilters, handleSearch } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteUsers, {
  confirm: (n) => `确定删除选中的 ${n} 个用户吗？`
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate,
  openEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    username: '',
    password: '',
    nickname: '',
    deptId: null,
    roleIds: [],
    phone: '',
    email: '',
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateUser({ ...payload })
      message.success('修改成功')
    } else {
      await createUser({ ...payload })
      message.success('新增成功')
    }
    loadData()
  }
})

const {
  visible: pwdVisible,
  submitting: pwdSubmitting,
  formRef: pwdFormRef,
  form: pwdForm,
  openCreate: openPwdForm,
  submit: handleResetPwd
} = useFormDialog({
  defaults: { userId: null, username: '', password: '', confirmPassword: '' },
  async onSubmit(payload) {
    await resetUserPassword({ userId: payload.userId, password: payload.password })
    message.success('密码重置成功')
  }
})

/** 状态筛选项取自字典 */
const { options: statusOptions } = useDict(DICT_NORMAL_DISABLE, { numeric: true })

const formRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: ['input', 'blur'] }],
  password: [
    {
      // 新增必填、编辑留空表示不修改密码（与后端 UserSaveDTO 同语义），
      // 所以「要不要必填」不能写死，得看当前是不是新增（form.id 为空）
      validator: (rule, value) => Boolean(form.id) || Boolean(value),
      message: '请输入初始密码',
      trigger: ['input', 'blur']
    },
    // 长度 + 复杂度，与重置密码、个人改密共用同一份策略（登录页只卡长度，不校验复杂度）
    ...buildPasswordRules()
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: ['input', 'blur'] }],
  roleIds: [
    {
      required: true,
      type: 'array',
      message: '请至少选择一个角色',
      trigger: ['change', 'blur']
    }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: ['input', 'blur'] }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: ['input', 'blur'] }]
}

const pwdRules = {
  password: buildPasswordRules({ required: true, requiredMessage: '请输入新密码' }),
  // 与个人改密一致：确认框只负责「两次输入一致」，空值交给 required 规则提示
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: ['input', 'blur'] },
    buildEqualToRule(() => pwdForm.password, '两次输入的密码不一致')
  ]
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '登录账号', key: 'username', width: 130 },
  { title: '昵称', key: 'nickname', width: 130 },
  { title: '部门', key: 'deptName', width: 140 },
  {
    title: '角色',
    key: 'roleNames',
    width: 160,
    render: (row) =>
      h(
        NSpace,
        { size: 4 },
        {
          default: () =>
            (row.roleNames || []).map((name) =>
              h(NTag, { type: 'primary', bordered: false }, { default: () => name })
            )
        }
      )
  },
  { title: '手机号', key: 'phone', width: 130 },
  { title: '邮箱', key: 'email', width: 180, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: row.id === 1 || !userStore.hasPerm('system:user:edit') || busy(row.id),
        'onUpdate:value': (val) => run(row.id, 'status', () => handleStatusChange(row, val ? 1 : 0))
      })
  },
  { title: '创建时间', key: 'createTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:user:edit') &&
            hActionBtn({
              tip: '编辑',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  username: r.username,
                  password: '',
                  nickname: r.nickname,
                  deptId: r.deptId ?? null,
                  roleIds: [...(r.roleIds || [])],
                  phone: r.phone,
                  email: r.email,
                  status: r.status,
                  remark: r.remark
                }))
            }),
          userStore.hasPerm('system:user:resetPwd') &&
            hActionBtn({
              tip: '重置密码',
              type: 'warning',
              icon: LockClosedOutline,
              disabled: busy(row.id),
              onClick: () => openResetPwd(row)
            }),
          userStore.hasPerm('system:user:remove') &&
            hActionConfirm({
              tip: '删除',
              confirm: `确定删除用户「${row.username}」吗？`,
              icon: TrashOutline,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function loadRoleOptions() {
  const res = await getRoleOptions()
  roleSelectOptions.value = (res.data || []).map((r) => ({ label: r.roleName, value: r.id }))
}

/** 去掉空 children，避免叶子节点仍显示展开图标 */
function pruneEmptyChildren(nodes) {
  return (nodes || []).map((n) => ({
    ...n,
    children: n.children?.length ? pruneEmptyChildren(n.children) : undefined
  }))
}

async function loadDeptOptions() {
  deptLoading.value = true
  try {
    const res = await getDeptOptions()
    const tree = pruneEmptyChildren(res.data || [])
    deptOptions.value = tree
    deptTreeData.value = tree
  } finally {
    deptLoading.value = false
  }
}

function onDeptSelect(keys) {
  const deptId = keys?.[0] ?? null
  selectedDeptKeys.value = deptId != null ? [deptId] : []
  query.deptId = deptId
  handleSearch()
}

function clearDeptFilter() {
  selectedDeptKeys.value = []
  query.deptId = null
  handleSearch()
}

// 搜索区「重置」会清空 query.deptId，同步左侧树高亮
watch(
  () => query.deptId,
  (id) => {
    selectedDeptKeys.value = id != null ? [id] : []
  }
)

function handleOpenCreate() {
  openCreate({
    deptId: selectedDeptKeys.value[0] ?? null
  })
}

function openResetPwd(row) {
  openPwdForm({ userId: row.id, username: row.username, password: '' })
}

async function handleStatusChange(row, status) {
  try {
    await changeUserStatus(row.id, status)
    row.status = status
    message.success(status === 1 ? '已启用' : '已停用')
  } catch (e) {
    loadData()
  }
}

function handleDelete(row) {
  return deleteUsers([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

async function handleExport() {
  await exportUsers(getFilters())
}

const importFileRef = ref(null)
const importMenuOptions = [
  { label: '下载模板', key: 'template' },
  { label: '导入数据', key: 'import' }
]

function handleImportMenu(key) {
  if (key === 'template') {
    downloadUserImportTemplate()
    return
  }
  if (key === 'import') {
    importFileRef.value?.click()
  }
}

async function onImportFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  importing.value = true
  try {
    const res = await importUsers(file)
    message.success(`成功导入 ${res.data?.success ?? 0} 条`)
    loadData()
  } catch (err) {
    // 错误由请求拦截器提示
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadRoleOptions()
  loadDeptOptions()
})
</script>

<style scoped>
.pwd-form__hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted);
}

.user-layout {
  display: flex;
  align-items: stretch;
  gap: 6px;
}

.dept-aside {
  flex: 0 0 240px;
  width: 240px;
  min-width: 200px;
  display: flex;
}

.dept-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: none !important;
}

.dept-card :deep(.n-card__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px 10px !important;
}

.dept-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.dept-title {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
  color: var(--color-text);
  letter-spacing: -0.01em;
}

.dept-search {
  margin-bottom: 10px;
}

.dept-spin {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.dept-spin :deep(.n-spin-content) {
  min-height: 120px;
}

.user-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.hidden-file {
  display: none;
}

.btn-caret {
  margin-left: 2px;
  font-size: 14px;
  opacity: 0.55;
}

@media (max-width: 960px) {
  .user-layout {
    flex-direction: column;
  }

  .dept-aside {
    flex: none;
    width: 100%;
  }

  .dept-card {
    max-height: 280px;
  }
}
</style>
