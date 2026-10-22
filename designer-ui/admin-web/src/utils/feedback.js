import { createDiscreteApi } from 'naive-ui'

/**
 * 脱离组件树的全局消息/对话框实例，供请求拦截器等非组件场景使用
 */
const { message, dialog, notification } = createDiscreteApi(['message', 'dialog', 'notification'])

export { message, dialog, notification }
