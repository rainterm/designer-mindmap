<template>
  <div class="page-container dict-page">
    <n-grid :cols="24" :x-gap="6" class="dict-grid">
      <!-- 字典类型 -->
      <n-grid-item :span="11" class="dict-col">
        <div class="dict-panel">
          <n-card class="search-card" :bordered="false">
            <div class="panel-search">
              <n-input
                v-model:value="typeQuery.dictName"
                placeholder="搜索字典名称"
                clearable
                @keyup.enter="typePager.handleSearch"
              />
              <n-button type="primary" :loading="typeLoading" @click="typePager.handleSearch">
                查询
              </n-button>
              <n-button @click="typePager.handleReset">
                重置
              </n-button>
              <n-button v-perm="'system:dict:add'" type="primary" @click="openTypeCreate">
                新增
              </n-button>
              <n-dropdown
                v-if="typeMoreOptions.length"
                trigger="click"
                :options="typeMoreOptions"
                @select="handleTypeMore"
              >
                <n-button :loading="typeImporting || fileDownloading">
                  更多
                  <n-icon class="btn-caret" :component="ChevronDownOutline" />
                </n-button>
              </n-dropdown>
              <input
                ref="typeImportFileRef"
                class="hidden-file"
                type="file"
                accept=".xlsx,.xls"
                @change="onTypeImportFileChange"
              />
            </div>
          </n-card>

          <PageTable
            class="dict-table-card"
            :pager="typePager"
            :columns="typeColumns"
            :row-props="typeRowProps"
          />
        </div>
      </n-grid-item>

      <!-- 字典数据 -->
      <n-grid-item :span="13" class="dict-col">
        <div class="dict-panel">
          <n-card class="search-card" :bordered="false">
            <div class="panel-search">
              <n-input
                v-model:value="dataQuery.dictName"
                placeholder="搜索字典标签"
                clearable
                :disabled="!activeType"
                @keyup.enter="dataPager.handleSearch"
              />
              <n-button
                type="primary"
                :disabled="!activeType"
                :loading="dataLoading"
                @click="dataPager.handleSearch"
              >
                查询
              </n-button>
              <n-button :disabled="!activeType" @click="dataPager.handleReset">
                重置
              </n-button>
              <n-button
                v-perm="'system:dict:add'"
                type="primary"
                :disabled="!activeType"
                @click="openDataCreate"
              >
                新增
              </n-button>
              <n-dropdown
                v-if="dataMoreOptions.length"
                trigger="click"
                :options="dataMoreOptions"
                @select="handleDataMore"
              >
                <n-button :loading="dataImporting || fileDownloading" :disabled="!activeType">
                  更多
                  <n-icon class="btn-caret" :component="ChevronDownOutline" />
                </n-button>
              </n-dropdown>
              <input
                ref="dataImportFileRef"
                class="hidden-file"
                type="file"
                accept=".xlsx,.xls"
                @change="onDataImportFileChange"
              />
            </div>
          </n-card>

          <TableEmpty
            v-if="!activeType"
            class="dict-empty"
            title="请先选择字典类型"
            description="在左侧列表中点选一个类型后再管理字典数据"
          />
          <PageTable
            v-else
            class="dict-table-card"
            :pager="dataPager"
            :columns="dataColumns"
          />
        </div>
      </n-grid-item>
    </n-grid>

    <Dialog
      v-model:show="typeFormVisible"
      :title="typeIsEdit ? '编辑字典类型' : '新增字典类型'"
      :loading="typeSubmitting"
      @confirm="handleTypeSubmit"
    >
      <n-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-placement="left" label-width="90">
        <n-form-item label="字典名称" path="dictName">
          <n-input v-model:value="typeForm.dictName" placeholder="如 订单状态" />
        </n-form-item>
        <n-form-item label="字典类型" path="dictType">
          <n-input v-model:value="typeForm.dictType" placeholder="如 biz_order_status，全局唯一" />
        </n-form-item>
        <n-form-item label="状态">
          <n-radio-group v-model:value="typeForm.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="typeForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>

    <Dialog
      v-model:show="dataFormVisible"
      :title="dataIsEdit ? '编辑字典数据' : '新增字典数据'"
      :loading="dataSubmitting"
      @confirm="handleDataSubmit"
    >
      <n-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-placement="left" label-width="90">
        <n-form-item label="字典类型">
          <n-input :value="dataForm.dictType" disabled />
        </n-form-item>
        <n-form-item label="数据标签" path="dictLabel">
          <n-input v-model:value="dataForm.dictLabel" placeholder="展示给用户的文案" />
        </n-form-item>
        <n-form-item label="数据键值" path="dictValue">
          <n-input v-model:value="dataForm.dictValue" placeholder="程序内使用的值" />
        </n-form-item>
        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="dataForm.sort" :min="0" :max="9999" style="width: 160px" />
        </n-form-item>
        <n-form-item label="是否默认">
          <n-switch v-model:value="dataForm.isDefault" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item label="状态">
          <n-radio-group v-model:value="dataForm.status">
            <n-radio v-for="o in statusOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="dataForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { ChevronDownOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import TableEmpty from '@/components/TableEmpty.vue'
import {
  createDictData,
  createDictType,
  deleteDictData,
  deleteDictTypes,
  downloadDictDataImportTemplate,
  downloadDictTypeImportTemplate,
  exportDictData,
  exportDictTypes,
  importDictData,
  importDictTypes,
  pageDictData,
  pageDictTypes,
  updateDictData,
  updateDictType
} from '@/modules/system/api/system'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useFormDialog } from '@/composables/useFormDialog'
import { useDict } from '@/composables/useDict'
import { DICT_NORMAL_DISABLE } from '@/constants/dict'
import { useUserStore } from '@/store/user'
import { fileDownloading } from '@/utils/download'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

/** 字典类型 / 字典数据的启用状态文案取自字典本身（sys_normal_disable） */
const { options: statusOptions, labelOf: statusLabel } = useDict(DICT_NORMAL_DISABLE, {
  numeric: true
})

const typeImporting = ref(false)
const dataImporting = ref(false)
const activeType = ref(null)

const typePager = usePagination((q) => pageDictTypes(q), {
  defaults: { dictName: '' }
})
const {
  query: typeQuery,
  rows: typeRows,
  loading: typeLoading,
  loadData: loadTypes
} = typePager

const dataPager = usePagination(
  (q) => {
    if (!activeType.value) return { records: [], total: 0 }
    return pageDictData({ ...q, dictType: activeType.value.dictType })
  },
  { defaults: { dictName: '' }, immediate: false }
)
const {
  query: dataQuery,
  loading: dataLoading,
  loadData: loadDataList
} = dataPager

const {
  visible: typeFormVisible,
  submitting: typeSubmitting,
  formRef: typeFormRef,
  form: typeForm,
  isEdit: typeIsEdit,
  openCreate: openTypeCreate,
  openEdit: openTypeEditForm,
  submit: handleTypeSubmit
} = useFormDialog({
  defaults: { id: null, dictName: '', dictType: '', status: 1, remark: '' },
  async onSubmit(payload) {
    if (payload.id) {
      await updateDictType({ ...payload })
      message.success('修改成功')
    } else {
      await createDictType({ ...payload })
      message.success('新增成功')
    }
    loadTypes()
  }
})

const {
  visible: dataFormVisible,
  submitting: dataSubmitting,
  formRef: dataFormRef,
  form: dataForm,
  isEdit: dataIsEdit,
  openCreate: openDataCreateForm,
  openEdit: openDataEditForm,
  submit: handleDataSubmit
} = useFormDialog({
  defaults: {
    id: null,
    dictType: '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
    isDefault: 0,
    status: 1,
    remark: ''
  },
  async onSubmit(payload) {
    if (payload.id) {
      await updateDictData({ ...payload })
      message.success('修改成功')
    } else {
      await createDictData({ ...payload })
      message.success('新增成功')
    }
    loadDataList()
  }
})

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: ['input', 'blur'] }],
  dictType: [
    { required: true, message: '请输入字典类型', trigger: ['input', 'blur'] },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '仅支持小写字母、数字与下划线', trigger: ['input', 'blur'] }
  ]
}

const dataRules = {
  dictLabel: [{ required: true, message: '请输入数据标签', trigger: ['input', 'blur'] }],
  dictValue: [{ required: true, message: '请输入数据键值', trigger: ['input', 'blur'] }]
}

const typeColumns = computed(() => [
  { title: '字典名称', key: 'dictName', width: 130, ellipsis: { tooltip: true } },
  { title: '字典类型', key: 'dictType', width: 150, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 70,
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
    width: 100,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:dict:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => openTypeEdit(row)
            }),
          userStore.hasPerm('system:dict:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `删除「${row.dictName}」会同时删除其字典数据，确定吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleTypeDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

const dataColumns = computed(() => [
  { title: '标签', key: 'dictLabel', width: 130 },
  { title: '键值', key: 'dictValue', width: 110 },
  { title: '排序', key: 'sort', width: 70 },
  {
    title: '默认',
    key: 'isDefault',
    width: 70,
    render: (row) =>
      row.isDefault === 1
        ? h(NTag, { type: 'success', bordered: false }, { default: () => '是' })
        : h('span', { class: 'text-muted' }, '-')
  },
  {
    title: '状态',
    key: 'status',
    width: 70,
    render: (row) =>
      h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', bordered: false },
        { default: () => statusLabel(row.status) }
      )
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('system:dict:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => openDataEdit(row)
            }),
          userStore.hasPerm('system:dict:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除「${row.dictLabel}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDataDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

function typeRowProps(row) {
  const active = activeType.value?.id === row.id
  return {
    class: active ? 'dict-type-row is-active' : 'dict-type-row',
    onClick: () => selectType(row)
  }
}

function selectType(row) {
  activeType.value = row
  dataQuery.pageNum = 1
  loadDataList()
}

watch(typeRows, (rows) => {
  if (!activeType.value && rows.length) {
    selectType(rows[0])
  } else if (activeType.value) {
    const still = rows.find((item) => item.id === activeType.value.id)
    if (!still) {
      activeType.value = null
      dataPager.rows.value = []
      dataPager.total.value = 0
    }
  }
})

function openTypeEdit(row) {
  openTypeEditForm(row, (r) => ({
    id: r.id,
    dictName: r.dictName,
    dictType: r.dictType,
    status: r.status,
    remark: r.remark
  }))
}

function handleTypeDelete(row) {
  return deleteDictTypes([row.id]).then(() => {
    message.success('删除成功')
    if (activeType.value?.id === row.id) {
      activeType.value = null
      dataPager.rows.value = []
      dataPager.total.value = 0
    }
    loadTypes()
  })
}

function openDataCreate() {
  openDataCreateForm({
    id: null,
    dictType: activeType.value?.dictType || '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
    isDefault: 0,
    status: 1,
    remark: ''
  })
}

function openDataEdit(row) {
  openDataEditForm(row, (r) => ({
    id: r.id,
    dictType: r.dictType,
    dictLabel: r.dictLabel,
    dictValue: r.dictValue,
    sort: r.sort ?? 0,
    isDefault: r.isDefault ?? 0,
    status: r.status,
    remark: r.remark
  }))
}

function handleDataDelete(row) {
  return deleteDictData([row.id]).then(() => {
    message.success('删除成功')
    loadDataList()
  })
}

async function handleTypeExport() {
  await exportDictTypes({ dictName: typeQuery.dictName || undefined })
}

const typeImportFileRef = ref(null)
const typeMoreOptions = computed(() => {
  const opts = []
  if (userStore.hasPerm('system:dict:export')) opts.push({ label: '导出', key: 'export' })
  if (userStore.hasPerm('system:dict:import')) {
    opts.push({ label: '下载模板', key: 'template' })
    opts.push({ label: '导入数据', key: 'import' })
  }
  return opts
})

function handleTypeMore(key) {
  if (key === 'export') return handleTypeExport()
  if (key === 'template') return downloadDictTypeImportTemplate()
  if (key === 'import') typeImportFileRef.value?.click()
}

async function onTypeImportFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  typeImporting.value = true
  try {
    const res = await importDictTypes(file)
    message.success(`成功导入 ${res.data?.success ?? 0} 条类型`)
    loadTypes()
  } catch (err) {
    // 错误由请求拦截器提示
  } finally {
    typeImporting.value = false
  }
}

async function handleDataExport() {
  await exportDictData({ dictType: activeType.value?.dictType || undefined })
}

const dataImportFileRef = ref(null)
const dataMoreOptions = computed(() => {
  const opts = []
  if (userStore.hasPerm('system:dict:export')) opts.push({ label: '导出', key: 'export' })
  if (userStore.hasPerm('system:dict:import')) {
    opts.push({ label: '下载模板', key: 'template' })
    opts.push({ label: '导入数据', key: 'import' })
  }
  return opts
})

function handleDataMore(key) {
  if (key === 'export') return handleDataExport()
  if (key === 'template') return downloadDictDataImportTemplate()
  if (key === 'import') dataImportFileRef.value?.click()
}

async function onDataImportFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  dataImporting.value = true
  try {
    const res = await importDictData(file)
    message.success(`成功导入 ${res.data?.success ?? 0} 条数据`)
    if (activeType.value) loadDataList()
  } catch (err) {
    // 错误由请求拦截器提示
  } finally {
    dataImporting.value = false
  }
}
</script>

<style scoped>
.dict-grid {
  align-items: stretch;
}

.dict-col {
  display: flex;
  min-width: 0;
}

.dict-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - var(--layout-nav-height, 44px) - var(--layout-tags-height, 36px) - 28px);
  min-width: 0;
}

.panel-search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-search .n-input {
  flex: 1;
  min-width: 0;
  max-width: 280px;
}

.dict-table-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.dict-table-card :deep(.n-card__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.dict-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
}

.hidden-file {
  display: none;
}

.btn-caret {
  margin-left: 2px;
  font-size: 14px;
  opacity: 0.55;
}

:deep(.dict-type-row) {
  cursor: pointer;
}

:deep(.dict-type-row td) {
  transition: background-color 0.15s ease;
}

:deep(.dict-type-row:hover td) {
  background: color-mix(in srgb, var(--color-primary) 5%, transparent) !important;
}

:deep(.dict-type-row.is-active td) {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent) !important;
}
</style>
