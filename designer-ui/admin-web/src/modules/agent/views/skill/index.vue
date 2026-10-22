<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="名称">
          <n-input v-model:value="query.name" placeholder="名称 / 编码" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-upload
            v-perm="'agent:skill:add'"
            :show-file-list="false"
            accept=".zip,application/zip,application/x-zip-compressed"
            :custom-request="handleToolbarUpload"
            :disabled="uploading"
          >
            <n-button type="primary" secondary :loading="uploading">
              上传压缩包
            </n-button>
          </n-upload>
          <n-button v-perm="'agent:skill:add'" type="primary" @click="openCreate">
            新增 Skill
          </n-button>
          <n-button
            v-perm="'agent:skill:remove'"
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
      :title="isEdit ? '编辑 Skill' : '新增 Skill'"
      size="xl"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="90">
        <n-form-item label="压缩包">
          <div class="pkg-block">
            <n-upload
              :show-file-list="false"
              accept=".zip,application/zip,application/x-zip-compressed"
              :custom-request="handleFormUpload"
              :disabled="parsing"
            >
              <n-button :loading="parsing">
                {{ form.packageName ? '重新上传 .zip' : '选择 .zip 压缩包' }}
              </n-button>
            </n-upload>
            <div v-if="form.packageName" class="pkg-meta">
              <n-tag type="info" :bordered="false">{{ form.packageName }}</n-tag>
              <span>{{ formatPackageSize(form.packageSize) }}</span>
              <span v-if="form.files?.length">{{ form.files.length }} 个文件</span>
              <span v-if="form.skillPath" class="mono-inline">{{ form.skillPath }}</span>
            </div>
            <p class="pkg-hint">需包含 SKILL.md（根目录或 skill-name/SKILL.md），上传后自动解析 frontmatter</p>
          </div>
        </n-form-item>
        <n-form-item label="名称" path="name">
          <n-input v-model:value="form.name" placeholder="Skill 名称" />
        </n-form-item>
        <n-form-item label="编码" path="code">
          <n-input v-model:value="form.code" placeholder="如 cs-tone（对应 SKILL.md name）" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="form.description"
            type="textarea"
            :rows="2"
            placeholder="何时使用该 Skill（对应 description）"
          />
        </n-form-item>
        <n-form-item v-if="form.files?.length" label="包内文件">
          <div class="file-list">
            <code v-for="f in form.files" :key="f">{{ f }}</code>
          </div>
        </n-form-item>
        <n-form-item label="正文" path="content">
          <n-input
            v-model:value="form.content"
            type="textarea"
            :rows="12"
            placeholder="SKILL.md 全文（含 frontmatter）"
            class="mono"
          />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">停用</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { CreateOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import {
  createSkill,
  deleteSkills,
  importSkillPackage,
  pageSkills,
  updateSkill
} from '@/modules/agent/api'
import { formatPackageSize, parseSkillPackage } from '@/modules/agent/utils/skillPackage'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()
const uploading = ref(false)
const parsing = ref(false)

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const pager = usePagination((q) => pageSkills(q), {
  defaults: { name: '', status: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteSkills, {
  confirm: (n) => `确定删除选中的 ${n} 个 Skill 吗？`,
  positiveText: '删除'
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate: openCreateBase,
  openEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: {
    id: null,
    name: '',
    code: '',
    description: '',
    content: '---\nname: \ndescription: \n---\n\n# \n\n',
    sourceType: 'manual',
    packageName: '',
    packageSize: 0,
    files: [],
    skillPath: '',
    status: 1
  },
  async onSubmit(payload) {
    const body = {
      ...payload,
      files: [...(payload.files || [])]
    }
    if (payload.id) {
      await updateSkill(body)
      message.success('更新成功')
    } else {
      await createSkill(body)
      message.success('新增成功')
    }
    loadData()
  }
})

function openCreate() {
  openCreateBase({
    files: [],
    packageName: '',
    packageSize: 0,
    skillPath: '',
    sourceType: 'manual'
  })
}

function applyPackageToForm(pkg) {
  form.name = pkg.name
  form.code = pkg.code
  form.description = pkg.description
  form.content = pkg.content
  form.sourceType = 'package'
  form.packageName = pkg.packageName
  form.packageSize = pkg.packageSize
  form.files = [...(pkg.files || [])]
  form.skillPath = pkg.skillPath || ''
}

async function parseUploadFile(rawFile) {
  return parseSkillPackage(rawFile)
}

async function handleToolbarUpload({ file, onFinish, onError }) {
  uploading.value = true
  try {
    const pkg = await parseUploadFile(file.file)
    const res = await importSkillPackage(pkg)
    if (res.code !== 200) {
      message.error(res.msg || '导入失败')
      onError()
      return
    }
    message.success(pkg.code ? `已导入 Skill「${pkg.name}」` : '导入成功')
    onFinish()
    loadData()
  } catch (e) {
    message.error(e?.message || '解析压缩包失败')
    onError()
  } finally {
    uploading.value = false
  }
}

async function handleFormUpload({ file, onFinish, onError }) {
  parsing.value = true
  try {
    const pkg = await parseUploadFile(file.file)
    applyPackageToForm(pkg)
    message.success('已解析压缩包，请确认后保存')
    onFinish()
  } catch (e) {
    message.error(e?.message || '解析压缩包失败')
    onError()
  } finally {
    parsing.value = false
  }
}

const formRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' },
  code: { required: true, message: '请输入编码', trigger: 'blur' },
  description: { required: true, message: '请输入描述', trigger: 'blur' },
  content: { required: true, message: '请输入正文', trigger: 'blur' }
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'name', width: 150 },
  { title: '编码', key: 'code', width: 130 },
  {
    title: '来源',
    key: 'sourceType',
    width: 150,
    render: (row) => {
      if (row.sourceType === 'package' || row.packageName) {
        return h('div', { class: 'source-cell' }, [
          h(NTag, { type: 'info', bordered: false }, { default: () => '压缩包' }),
          h('span', { class: 'source-file' }, row.packageName || '-')
        ])
      }
      return h(NTag, { bordered: false }, { default: () => '手工' })
    }
  },
  {
    title: '描述',
    key: 'description',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NTag, { type: row.status === 1 ? 'success' : 'default', bordered: false }, {
        default: () => (row.status === 1 ? '启用' : '停用')
      })
  },
  { title: '更新时间', key: 'updateTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 100,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('agent:skill:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  name: r.name,
                  code: r.code,
                  description: r.description || '',
                  content: r.content || '',
                  sourceType: r.sourceType || (r.packageName ? 'package' : 'manual'),
                  packageName: r.packageName || '',
                  packageSize: r.packageSize || 0,
                  files: [...(r.files || [])],
                  skillPath: r.skillPath || '',
                  status: r.status
                }))
            }),
          userStore.hasPerm('agent:skill:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除 Skill「${row.name}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function handleDelete(row) {
  await deleteSkills([row.id])
  message.success('删除成功')
  loadData()
}
</script>

<style scoped>
.pkg-block {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pkg-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.pkg-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.4;
}
.file-list {
  width: 100%;
  max-height: 120px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  border-radius: var(--radius);
  background: var(--color-fill-secondary, rgba(0, 0, 0, 0.04));
}
.file-list code {
  font-size: 12px;
  line-height: 1.4;
}
.mono :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.5;
}
.mono-inline {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>

<style>
.source-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.source-file {
  font-size: 12px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
