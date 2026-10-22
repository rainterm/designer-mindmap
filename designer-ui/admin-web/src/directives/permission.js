import { useUserStore } from '@/store/user'

/**
 * 权限指令：v-perm="'system:user:add'"
 * 无权限时直接移除元素
 */
export default {
  mounted(el, binding) {
    const userStore = useUserStore()
    const value = binding.value
    if (!value) return
    const perms = Array.isArray(value) ? value : [value]
    const passed = perms.some((p) => userStore.hasPerm(p))
    if (!passed && el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
}
