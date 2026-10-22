<template>
  <div class="attachment-uploader">
    <n-space>
      <n-button :loading="uploading" @click="triggerUpload">
        {{ placeholder || '点击上传附件' }}</n-button>
      <input
        ref="inputRef"
        type="file"
        class="att-input"
        multiple
        @change="onFileChange"
      />
    </n-space>
    <div v-if="items.length" class="att-list">
      <div v-for="item in items" :key="itemKey(item)" class="att-item">
        <a v-if="item.url" :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.name || '文件' }}</a>
        <span v-else>{{ item.name || '文件' }}</span>
        <n-button text type="error" @click="remove(item)">删除</n-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { uploadFile } from '@/modules/system/api/file'

const props = defineProps({
  /** 文件元信息数组：[{ id, url, name, fileSize }] */
  modelValue: { type: Array, default: () => [] },
  /** 业务子目录 */
  dir: { type: String, default: 'common' },
  placeholder: { type: String, default: '点击上传附件' }
})
const emit = defineEmits(['update:modelValue'])

const message = useMessage()
const inputRef = ref(null)
const uploading = ref(false)

const items = computed(() => (Array.isArray(props.modelValue) ? props.modelValue : []))

function itemKey(item) {
  return String(item.id || item.url || item.name)
}

function triggerUpload() {
  inputRef.value?.click()
}

async function onFileChange(e) {
  const files = Array.from(e?.target?.files || [])
  if (e?.target) e.target.value = ''
  if (!files.length) return
  uploading.value = true
  const added = [...items.value]
  try {
    for (const file of files) {
      try {
        const res = await uploadFile(file, props.dir)
        const data = res.data || {}
        const meta = {
          id: data.id,
          url: data.url,
          name: data.name || data.fileName || file.name,
          fileSize: data.fileSize
        }
        if (!meta.url) {
          throw new Error('上传成功但未返回文件地址')
        }
        added.push(meta)
      } catch (err) {
        message.error(`${file.name} 上传失败：${err?.message || ''}`)
      }
    }
    emit('update:modelValue', added)
  } finally {
    uploading.value = false
  }
}

function remove(item) {
  emit(
    'update:modelValue',
    items.value.filter((i) => itemKey(i) !== itemKey(item))
  )
}
</script>

<style scoped>
.att-input {
  display: none;
}
.att-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.att-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}
.att-item a {
  color: var(--color-primary);
  word-break: break-all;
}
</style>
