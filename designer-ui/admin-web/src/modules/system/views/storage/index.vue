<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="配置名称">
          <n-input v-model:value="query.configName" placeholder="配置名称" clearable />
        </n-form-item>
        <n-form-item label="存储类型">
          <n-select v-model:value="query.storageType" :options="typeOptions" placeholder="全部" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
        <n-form-item label="访问类型">
          <n-select v-model:value="query.isPublic" :options="accessOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'system:storage:add'" type="primary" @click="openCreate">
            新增配置
          </n-button>
          <n-button
            v-perm="'system:storage:remove'"
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
      :title="isEdit ? '编辑存储配置' : '新增存储配置'"
      size="lg"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="110">
        <n-form-item label="配置名称" path="configName">
          <n-input v-model:value="form.configName" placeholder="如 本地默认 / 生产 MinIO" />
        </n-form-item>
        <n-form-item label="存储类型" path="storageType">
          <n-select
            v-model:value="form.storageType"
            :options="typeOptions"
            :disabled="!!form.id"
            placeholder="选择存储类型"
            @update:value="onStorageTypeChange"
          />
        </n-form-item>
        <n-form-item label="设为默认">
          <n-switch v-model:value="form.isDefault" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item label="状态">
            <n-radio-group v-model:value="form.status">
              <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
                {{ o.label }}
              </n-radio>
            </n-radio-group>
          </n-form-item>
        <n-form-item label="访问类型">
          <n-radio-group v-model:value="form.isPublic">
            <n-space>
              <n-radio v-for="o in accessOptions" :key="o.value" :value="o.value">
                {{ o.label }}
              </n-radio>
            </n-space>
          </n-radio-group>
          <span class="form-tip">
            <template v-if="form.isPublic === 1">
              匿名可访问，适用无需登录公开访问的前台资源
            </template>
            <template v-else>
              仅登录后可访问，适用于敏感文件
            </template>
          </span>
        </n-form-item>
        <n-form-item label="下载前缀">
          <n-input :value="form.isPublic === 1 ? publicUrlPrefix : privateUrlPrefix" disabled />
        </n-form-item>
        <n-form-item label="允许扩展名">
          <n-input
            v-model:value="form.allowedExtensions"
            type="textarea"
            :rows="2"
            placeholder="jpg,png,pdf..."
          />
        </n-form-item>
        <n-form-item label="单文件上限(MB)">
          <n-input-number
            v-model:value="form.maxSizeMb"
            :min="1"
            :max="100"
            :precision="0"
            style="width: 220px"
          />
        </n-form-item>

        <n-divider title-placement="left">图片压缩</n-divider>
        <n-form-item label="压缩图片">
          <n-space align="center" :size="10">
            <n-switch v-model:value="form.compressEnabled" :checked-value="1" :unchecked-value="0" />
            <span class="form-tip">仅对 jpg / jpeg / png 生效，其余类型按原文件上传</span>
          </n-space>
        </n-form-item>
        <n-form-item v-if="form.compressEnabled === 1" label="压缩质量">
          <div class="quality-field">
            <n-slider v-model:value="form.compressQuality" :min="10" :max="100" :step="5" />
            <n-input-number
              v-model:value="form.compressQuality"
              :min="10"
              :max="100"
              :step="5"
              style="width: 110px"
            >
              <template #suffix>%</template>
            </n-input-number>
          </div>
          <template #feedback>
            越低体积越小、画质越差；PNG 为无损压缩，会忽略该值
          </template>
        </n-form-item>

        <n-divider title-placement="left">类型专属配置</n-divider>
        <TableEmpty
          v-if="!activeFields.length"
          title="请先选择存储类型"
          description="选择类型后即可配置对应参数"
        />
        <template v-for="field in activeFields" :key="field.name">
          <n-form-item
            :label="field.label"
            :path="`config.${field.name}`"
            :rule="configFieldRule(field)"
          >
            <n-input-number
              v-if="field.inputType === 'number'"
              v-model:value="form.config[field.name]"
              style="width: 100%"
            />
            <n-switch
              v-else-if="field.inputType === 'switch'"
              v-model:value="form.config[field.name]"
              :checked-value="true"
              :unchecked-value="false"
            />
            <n-input
              v-else-if="field.inputType === 'textarea'"
              v-model:value="form.config[field.name]"
              type="textarea"
              :rows="2"
              :placeholder="field.placeholder"
            />
            <n-input
              v-else-if="field.inputType === 'password'"
              v-model:value="form.config[field.name]"
              type="password"
              show-password-on="click"
              :placeholder="form.id ? '留空表示不修改' : field.placeholder"
            />
            <n-input
              v-else
              v-model:value="form.config[field.name]"
              :placeholder="field.placeholder"
            />
            <template v-if="field.help" #feedback>{{ field.help }}</template>
          </n-form-item>
        </template>

        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { NButton, NSwitch, NTag, useMessage } from 'naive-ui'
import { CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import TableEmpty from '@/components/TableEmpty.vue'
import {
  changeStorageConfigStatus,
  createStorageConfig,
  deleteStorageConfigs,
  getStorageConfig,
  listStorageTypes,
  pageStorageConfigs,
  setDefaultStorageConfig,
  updateStorageConfig
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_ENABLE_DISABLE, DICT_STORAGE_ACCESS } from '@/constants/dict'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const storageTypes = ref([])

const pager = usePagination((q) => pageStorageConfigs(q), {
  defaults: {
    configName: '',
    storageType: null,
    status: null,
    isPublic: null
  },
  immediate: false
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteStorageConfigs, {
  confirm: (n) => `确定删除选中的 ${n} 条配置吗？`,
  filter: (ids, rows) => ids.filter((id) => rows.find((r) => r.id === id)?.isDefault !== 1),
  emptyWarning: '默认配置不可删除'
})

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
    configName: '',
    storageType: null,
    isDefault: 0,
    status: 1,
    isPublic: 0,
    allowedExtensions: 'jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,txt,md,markdown',
    maxSizeMb: 5,
    compressEnabled: 0,
    compressQuality: 80,
    config: {},
    remark: ''
  },
  async onSubmit(payload) {
    const data = {
      id: payload.id,
      configName: payload.configName,
      storageType: payload.storageType,
      isDefault: payload.isDefault,
      status: payload.status,
      isPublic: payload.isPublic,
      allowedExtensions: payload.allowedExtensions,
      maxSize: mbToBytes(payload.maxSizeMb),
      compressEnabled: payload.compressEnabled,
      compressQuality: payload.compressQuality,
      remark: payload.remark,
      config: { ...payload.config }
    }
    if (payload.id) {
      await updateStorageConfig(data)
    } else {
      await createStorageConfig(data)
    }
    message.success('保存成功')
    loadData()
  }
})

/** 状态与访问类型文案取自字典：筛选项、列标签、单选按钮同源 */
const { options: statusOptions } = useDict(DICT_ENABLE_DISABLE, { numeric: true })
const { options: accessOptions, labelOf: accessLabel } = useDict(DICT_STORAGE_ACCESS, {
  numeric: true
})

const typeOptions = computed(() =>
  storageTypes.value.map((t) => ({ label: t.label, value: t.type }))
)

const typeLabelMap = computed(() => {
  const map = {}
  storageTypes.value.forEach((t) => {
    map[t.type] = t.label
  })
  return map
})

const activeFields = computed(() => {
  const t = storageTypes.value.find((x) => x.type === form.storageType)
  return t?.fields || []
})

const privateUrlPrefix = computed(
  () =>
    storageTypes.value.find((x) => x.defaultUrlPrefix)?.defaultUrlPrefix ||
    '/admin-api/system/file/download/'
)

const publicUrlPrefix = computed(
  () =>
    storageTypes.value.find((x) => x.publicUrlPrefix)?.publicUrlPrefix ||
    '/app-api/system/file/download/'
)

const formRules = {
  configName: { required: true, message: '请输入配置名称', trigger: 'blur' },
  storageType: { required: true, message: '请选择存储类型', trigger: 'change' }
}

const MB = 1024 * 1024

const columns = computed(() => [
  { type: 'selection', width: 44, disabled: (row) => row.isDefault === 1 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '配置名称', key: 'configName', width: 140 },
  {
    title: '存储类型',
    key: 'storageType',
    width: 120,
    render: (row) =>
      h(NTag, { bordered: false, type: 'info' }, {
        default: () => typeLabelMap.value[row.storageType] || row.storageType
      })
  },
  {
    title: '访问类型',
    key: 'isPublic',
    width: 100,
    render: (row) =>
      h(
        NTag,
        { bordered: false, type: row.isPublic === 1 ? 'success' : 'warning' },
        { default: () => accessLabel(row.isPublic) }
      )
  },
  {
    title: '默认',
    key: 'isDefault',
    width: 90,
    render: (row) =>
      row.isDefault === 1
        ? h(NTag, { type: 'success', bordered: false }, { default: () => '默认' })
        : userStore.hasPerm('system:storage:edit')
          ? h(
              NButton,
              {
                text: true,
                type: 'primary',
                loading: locked(row.id, 'default'),
                disabled: busy(row.id),
                onClick: () => run(row.id, 'default', () => handleSetDefault(row))
              },
              { default: () => '设为默认' }
            )
          : '—'
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        loading: locked(row.id, 'status'),
        disabled: busy(row.id) || !userStore.hasPerm('system:storage:edit') || row.isDefault === 1,
        onUpdateValue: (v) => run(row.id, 'status', () => handleStatus(row, v ? 1 : 0))
      })
  },
  { title: '单文件上限', key: 'maxSize', width: 120, render: (row) => formatSizeMb(row.maxSize) },
  {
    title: '图片压缩',
    key: 'compressEnabled',
    width: 110,
    render: (row) =>
      row.compressEnabled === 1
        ? h(NTag, { bordered: false, type: 'success' }, {
            default: () => `质量 ${row.compressQuality}%`
          })
        : '—'
  },
  { title: '备注', key: 'remark', width: 160, ellipsis: { tooltip: true } },
  { title: '更新时间', key: 'updateTime', width: 190 },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:storage:edit') &&
            hActionBtn({
              tip: '编辑',
              icon: CreateOutline,
              loading: locked(row.id, 'edit'),
              disabled: busy(row.id),
              onClick: () => run(row.id, 'edit', () => openEdit(row))
            }),
          userStore.hasPerm('system:storage:remove') &&
            row.isDefault !== 1 &&
            hActionConfirm({
              tip: '删除',
              confirm: '确定删除该配置吗？',
              icon: TrashOutline,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

/** 类型专属字段必填规则（编辑时密码可留空表示不改） */
function configFieldRule(field) {
  if (!field?.required || field.inputType === 'switch') {
    return undefined
  }
  if (field.inputType === 'password' && form.id) {
    return undefined
  }
  const msg = `请填写${field.label}`
  return {
    required: true,
    message: msg,
    trigger: ['input', 'blur', 'change'],
    validator(_rule, value) {
      if (field.inputType === 'number') {
        if (value === null || value === undefined || value === '') {
          return new Error(msg)
        }
        return true
      }
      if (value === null || value === undefined || String(value).trim() === '') {
        return new Error(msg)
      }
      return true
    }
  }
}

function formatSizeMb(size) {
  const n = Number(size) || 0
  const mb = n / MB
  return Number.isInteger(mb) ? `${mb} MB` : `${mb.toFixed(2)} MB`
}

function bytesToMb(bytes) {
  const n = Number(bytes) || 0
  return Math.max(1, Math.round(n / MB) || 5)
}

function mbToBytes(mb) {
  const n = Number(mb) || 5
  return Math.round(n * MB)
}

function buildConfigDefaults(type) {
  const t = storageTypes.value.find((x) => x.type === type)
  const cfg = {}
  ;(t?.fields || []).forEach((f) => {
    if (f.inputType === 'switch') {
      const d = String(f.defaultValue ?? 'false')
      cfg[f.name] = d === 'true' || d === '1'
    } else if (f.inputType === 'number') {
      cfg[f.name] = f.defaultValue ? Number(f.defaultValue) : null
    } else {
      cfg[f.name] = f.defaultValue || ''
    }
  })
  return cfg
}

function onStorageTypeChange(type) {
  form.config = buildConfigDefaults(type)
}

async function loadTypes() {
  const res = await listStorageTypes()
  storageTypes.value = res.data || []
}

function openCreate() {
  const storageType = storageTypes.value[0]?.type || null
  openFormCreate({
    id: null,
    configName: '',
    storageType,
    isDefault: 0,
    status: 1,
    isPublic: 0,
    allowedExtensions: 'jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,txt,md,markdown',
    maxSizeMb: 5,
    compressEnabled: 0,
    compressQuality: 80,
    remark: '',
    config: buildConfigDefaults(storageType)
  })
}

async function openEdit(row) {
  const res = await getStorageConfig(row.id)
  const data = res.data
  openFormEdit(data, (d) => ({
    id: d.id,
    configName: d.configName,
    storageType: d.storageType,
    isDefault: d.isDefault,
    status: d.status,
    isPublic: d.isPublic,
    allowedExtensions: d.allowedExtensions,
    maxSizeMb: bytesToMb(d.maxSize),
    compressEnabled: d.compressEnabled === 1 ? 1 : 0,
    compressQuality: d.compressQuality == null ? 80 : d.compressQuality,
    remark: d.remark || '',
    config: { ...(d.config || {}) }
  }))
}

function handleDelete(row) {
  return deleteStorageConfigs([row.id]).then(() => {
    message.success('删除成功')
    loadData()
  })
}

async function handleSetDefault(row) {
  await setDefaultStorageConfig(row.id)
  message.success('已设为默认')
  loadData()
}

async function handleStatus(row, status) {
  await changeStorageConfigStatus(row.id, status)
  message.success('状态已更新')
  loadData()
}

onMounted(async () => {
  await loadTypes()
  await loadData()
})
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: var(--n-text-color-3);
  font-size: 12px;
}

/* 质量滑块 + 数字输入：与「Logo 圆角」保持同一套排版 */
.quality-field {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.quality-field .n-slider {
  flex: 1;
  min-width: 0;
}
</style>
