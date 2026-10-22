<template>
  <div class="page-container kb-detail-route">
    <Dialog
      v-model:show="visible"
      :title="detailTitle"
      size="full"
      :fullscreenable="false"
      :draggable="false"
      :show-footer="false"
      @after-leave="goList"
    >
      <KnowledgeDetailPanel
        v-if="visible && knowledgeId"
        :knowledge-id="knowledgeId"
        @title-change="detailTitle = $event"
        @close="visible = false"
      />
    </Dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import KnowledgeDetailPanel from './KnowledgeDetailPanel.vue'

const route = useRoute()
const router = useRouter()

const visible = ref(true)
const detailTitle = ref('知识库详情')
const knowledgeId = computed(() => route.query.id || null)

function goList() {
  router.replace('/agent/knowledge')
}
</script>

<style scoped>
.kb-detail-route {
  min-height: 40vh;
}
</style>
