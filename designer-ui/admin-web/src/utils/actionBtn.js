import { h } from 'vue'
import { NButton, NIcon, NPopconfirm, NTooltip } from 'naive-ui'

/** 操作列按钮组：单行、靠右 */
export function hActionGroup(children) {
  return h(
    'div',
    {
      class: 'table-action-group',
      style: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        flexWrap: 'nowrap',
        gap: '2px',
        width: '100%',
        whiteSpace: 'nowrap'
      }
    },
    (children || []).filter(Boolean)
  )
}

/**
 * 操作列图标按钮：仅显示图标，悬浮显示文字
 */
export function hActionBtn({
  tip,
  type = 'primary',
  icon,
  disabled = false,
  loading = false,
  onClick
}) {
  return h(
    NTooltip,
    { placement: 'top' },
    {
      trigger: () =>
        h(
          NButton,
          {
            quaternary: true,
            circle: true,
            type,
            disabled,
            loading,
            onClick
          },
          {
            icon: () => h(NIcon, { size: 18 }, { default: () => h(icon) })
          }
        ),
      default: () => tip
    }
  )
}

/**
 * 带二次确认的操作列图标按钮
 */
export function hActionConfirm({
  tip,
  confirm,
  type = 'error',
  icon,
  disabled = false,
  loading = false,
  onPositiveClick
}) {
  return h(
    NTooltip,
    { placement: 'top' },
    {
      trigger: () =>
        h(
          NPopconfirm,
          { disabled, onPositiveClick },
          {
            trigger: () =>
              h(
                NButton,
                {
                  quaternary: true,
                  circle: true,
                  type,
                  disabled,
                  loading
                },
                {
                  icon: () => h(NIcon, { size: 18 }, { default: () => h(icon) })
                }
              ),
            default: () => confirm
          }
        ),
      default: () => tip
    }
  )
}
