<template>
  <div class="page-container agent-edit-route">
    <Dialog
      v-model:show="visible"
      :title="editTitle"
      size="full"
      :fullscreenable="false"
      :draggable="false"
      :loading="saving"
      confirm-text="保存草稿"
      @confirm="handleConfirm"
      @after-leave="goList"
    >
      <AgentEditForm
        ref="formRef"
        :agent-id="agentId"
        @missing="goList"
      />
    </Dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AgentEditForm from './AgentEditForm.vue'

const route = useRoute()
const router = useRouter()

const visible = ref(true)
const formRef = ref(null)
const saving = ref(false)

const agentId = computed(() => route.query.id || null)
const editTitle = computed(() => (agentId.value ? '编辑智能体' : '新建智能体'))

async function handleConfirm() {
  saving.value = true
  try {
    const ok = await formRef.value?.save()
    if (ok && formRef.value?.form?.id && !route.query.id) {
      router.replace({ path: '/agent/agent/edit', query: { id: formRef.value.form.id } })
    }
  } finally {
    saving.value = false
  }
}

function goList() {
  router.replace('/agent/agent')
}
</script>

<style scoped>
.agent-edit-route {
  min-height: 40vh;
}
</style>
