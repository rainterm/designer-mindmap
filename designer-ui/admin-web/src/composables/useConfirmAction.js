import { useDialog, useMessage } from 'naive-ui'

/**
 * 通用确认操作（清空、强制下线、全部标记等）。
 *
 * @param {() => Promise<any>} action
 * @param {object} [options]
 */
export function useConfirmAction(action, options = {}) {
  const {
    title = '确认操作',
    content = '确定执行该操作吗？',
    positiveText = '确定',
    successMessage = '操作成功',
    onSuccess
  } = options

  const dialog = useDialog()
  const message = useMessage()

  function run(overrides = {}) {
    dialog.warning({
      title: overrides.title ?? title,
      content: typeof (overrides.content ?? content) === 'function'
        ? (overrides.content ?? content)()
        : overrides.content ?? content,
      positiveText: overrides.positiveText ?? positiveText,
      negativeText: '取消',
      onPositiveClick: async () => {
        await action(overrides)
        message.success(overrides.successMessage ?? successMessage)
        await onSuccess?.(overrides)
      }
    })
  }

  return { run }
}
