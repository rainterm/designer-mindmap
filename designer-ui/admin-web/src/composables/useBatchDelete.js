import { useDialog, useMessage } from 'naive-ui'

/**
 * 列表批量删除（确认弹框 + 清空勾选 + 刷新）。
 *
 * @param {object} pager usePagination 返回值
 * @param {(ids: any[]) => Promise<any>} deleteApi
 * @param {object} [options]
 * @param {string} [options.title='批量删除']
 * @param {(count: number) => string} [options.confirm] 确认文案
 * @param {string} [options.positiveText='确定删除']
 * @param {string} [options.successMessage='删除成功']
 * @param {(ids: any[], rows: any[]) => any[]} [options.filter] 过滤不可删 id
 * @param {string} [options.emptyWarning] filter 后为空时的提示
 * @param {() => void} [options.onSuccess] 删除并刷新列表后的回调（如同步菜单红点）
 */
export function useBatchDelete(pager, deleteApi, options = {}) {
  const {
    title = '批量删除',
    confirm = (n) => `确定删除选中的 ${n} 项吗？`,
    positiveText = '确定删除',
    successMessage = '删除成功',
    filter,
    emptyWarning = '没有可删除的项',
    onSuccess
  } = options

  const dialog = useDialog()
  const message = useMessage()

  function handleBatchDelete() {
    let ids = [...(pager.checkedRowKeys?.value || [])]
    if (typeof filter === 'function') {
      ids = filter(ids, pager.rows?.value || [])
      if (!ids.length) {
        message.warning(emptyWarning)
        return
      }
    }
    if (!ids.length) return

    dialog.warning({
      title,
      content: confirm(ids.length),
      positiveText,
      negativeText: '取消',
      onPositiveClick: async () => {
        await deleteApi(ids)
        message.success(successMessage)
        pager.clearSelection?.()
        await pager.loadData?.()
        onSuccess?.()
      }
    })
  }

  return { handleBatchDelete }
}
