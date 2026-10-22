<template>
  <Dialog
    :show="show"
    title="个人中心"
    confirm-text="保存修改"
    :loading="saving"
    @update:show="emit('update:show', $event)"
    @confirm="handleConfirm"
  >
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      label-width="90"
      class="profile-dialog__form"
    >
      <n-form-item label="登录账号">
        <n-input :value="info.username" disabled />
      </n-form-item>
      <n-form-item label="昵称" path="nickname">
        <n-input v-model:value="form.nickname" placeholder="请输入昵称" />
      </n-form-item>
      <n-form-item label="邮箱" path="email">
        <n-input v-model:value="form.email" placeholder="请输入邮箱" />
      </n-form-item>
      <n-form-item label="手机号" path="phone">
        <n-input v-model:value="form.phone" placeholder="请输入手机号" />
      </n-form-item>
      <n-form-item label="头像">
        <!-- 图片卡片模式：点击卡片上传、悬停卡片上的删除按钮移除，无需额外按钮 -->
        <n-upload
          v-model:file-list="avatarFileList"
          list-type="image-card"
          accept="image/*"
          :max="1"
          :custom-request="handleAvatarUpload"
          @remove="handleAvatarRemove"
        />
      </n-form-item>
    </n-form>
  </Dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { getProfile, updateProfile } from '@/api/auth'
import { uploadFile } from '@/modules/system/api/file'
import { useUserStore } from '@/store/user'

const props = defineProps({
  show: { type: Boolean, default: false }
})
const emit = defineEmits(['update:show'])

const message = useMessage()
const userStore = useUserStore()

const saving = ref(false)
const formRef = ref(null)
const form = reactive({ nickname: '', email: '', phone: '', avatar: '' })

const info = reactive({
  userId: null,
  username: '',
  nickname: '',
  avatar: '',
  email: '',
  phone: '',
  roles: [],
  perms: [],
  superAdmin: false
})

// 头像上传组件的受控文件列表：以 form.avatar 为唯一数据源，
// 上传/删除均由 n-upload 自身完成，无需额外的按钮
const avatarFileList = ref([])

function syncAvatarFileList(url = form.avatar) {
  avatarFileList.value = url
    ? [{ id: 'avatar', name: 'avatar', status: 'finished', url }]
    : []
}

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: ['input', 'blur'] },
    { max: 50, message: '昵称长度不能超过 50', trigger: ['input', 'blur'] }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: ['input', 'blur'] }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: ['input', 'blur'] }]
}

async function loadProfile() {
  const res = await getProfile()
  Object.assign(info, res.data)
  Object.assign(form, {
    nickname: res.data.nickname || '',
    email: res.data.email || '',
    phone: res.data.phone || '',
    avatar: res.data.avatar || ''
  })
  syncAvatarFileList()
}

async function handleAvatarUpload({ file }) {
  try {
    const res = await uploadFile(file.file, 'avatar')
    form.avatar = res.data.url
    // 直接用已完成的文件项替换上传中的临时项，避免残留无地址的占位图
    syncAvatarFileList(res.data.url)
    message.success('上传成功')
  } catch (e) {
    syncAvatarFileList()
    message.error('头像上传失败')
  }
}

function handleAvatarRemove() {
  form.avatar = ''
  syncAvatarFileList('')
  return true
}

async function handleConfirm() {
  try {
    await formRef.value?.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await updateProfile({ ...form })
    message.success('资料已更新')
    // 同步刷新顶部导航栏展示的用户信息
    await userStore.loadUserInfo()
    emit('update:show', false)
  } finally {
    saving.value = false
  }
}

// 每次打开都拉取最新资料，并清掉上一次遗留的校验状态
watch(
  () => props.show,
  async (visible) => {
    if (!visible) return
    formRef.value?.restoreValidation()
    await loadProfile()
  }
)
</script>

<style scoped>
.profile-dialog__form {
  padding-top: 4px;
}
</style>
