import { reactive } from 'vue'

/**
 * 行内操作防重复点击：按 rowId + action 加锁，并驱动按钮 loading。
 *
 * @example
 * const { locked, run } = useActionLock()
 * h(NButton, {
 *   loading: locked(row.id, 'delete'),
 *   disabled: locked(row.id, 'delete'),
 *   onClick: () => run(row.id, 'delete', () => doDelete(row))
 * })
 */
export function useActionLock() {
  const locks = reactive({})

  function lockKey(id, action = 'default') {
    return `${id ?? ''}::${action}`
  }

  function locked(id, action = 'default') {
    return !!locks[lockKey(id, action)]
  }

  /** 任意操作进行中（用于整行禁用） */
  function busy(id) {
    const prefix = `${id ?? ''}::`
    return Object.keys(locks).some((k) => k.startsWith(prefix) && locks[k])
  }

  async function run(id, action, fn) {
    const k = lockKey(id, action)
    if (locks[k]) {
      return
    }
    locks[k] = true
    try {
      return await fn()
    } finally {
      locks[k] = false
    }
  }

  return { locks, locked, busy, run }
}

export default useActionLock
