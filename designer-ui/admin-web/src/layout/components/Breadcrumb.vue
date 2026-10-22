<template>
  <n-breadcrumb>
    <n-breadcrumb-item
      v-for="(item, index) in items"
      :key="`${item.path}-${index}`"
      :clickable="canClick(item, index)"
      @click="handleClick(item, index)"
    >
      {{ item.title }}
    </n-breadcrumb-item>
  </n-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 找到菜单树中某个路径的完整父级链 */
function findChain(nodes, path, chain = []) {
  for (const node of nodes || []) {
    const next = [...chain, node]
    const leafPath = node.path || ''
    if (leafPath === path) {
      return next
    }
    if (node.children?.length) {
      const found = findChain(node.children, path, next)
      if (found) return found
    }
  }
  return null
}

/** 真实菜单页（非 Layout 目录分组）才可跳转 */
function isMenuPage(item) {
  if (!item?.path || !String(item.path).startsWith('/')) return false
  const comp = item.component
  if (!comp || comp === 'Layout') return false
  return true
}

const items = computed(() => {
  const chain = findChain(userStore.routers, route.path)
  if (chain && chain.length) {
    return chain.map((node) => ({
      title: node.meta?.title || node.name,
      path: node.path,
      component: node.component
    }))
  }
  return [{ title: route.meta?.title || '首页', path: route.path, component: route.name }]
})

function canClick(item, index) {
  if (index === items.value.length - 1) return false
  return isMenuPage(item)
}

function handleClick(item, index) {
  if (!canClick(item, index)) return
  if (item.path !== route.path) {
    router.push(item.path)
  }
}
</script>
