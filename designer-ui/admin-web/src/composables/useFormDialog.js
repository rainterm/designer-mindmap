import { computed, reactive, ref } from 'vue'

/**
 * 表单弹框状态机：visible / submitting / openCreate / openEdit / submit。
 *
 * @param {object} options
 * @param {object} options.defaults 表单默认值
 * @param {(form: object) => Promise<void>} [options.onSubmit] 校验通过后执行；成功则自动关闭
 */
export function useFormDialog(options = {}) {
  const { defaults = {}, onSubmit } = options

  const visible = ref(false)
  const submitting = ref(false)
  const formRef = ref(null)
  const form = reactive({ ...defaults })

  const isEdit = computed(() => form.id != null && form.id !== '')

  function applyForm(data) {
    const next = { ...defaults, ...data }
    for (const key of Object.keys(form)) {
      if (!(key in next)) delete form[key]
    }
    Object.assign(form, next)
  }

  function openCreate(patch = {}) {
    applyForm(patch)
    visible.value = true
  }

  /** @param {object} row @param {(row: object) => object} [mapper] */
  function openEdit(row, mapper) {
    applyForm(typeof mapper === 'function' ? mapper(row) : row)
    visible.value = true
  }

  function close() {
    visible.value = false
  }

  async function submit() {
    try {
      await formRef.value?.validate()
    } catch {
      return false
    }
    if (!onSubmit) return false
    submitting.value = true
    try {
      await onSubmit(form)
      visible.value = false
      return true
    } finally {
      submitting.value = false
    }
  }

  return {
    visible,
    submitting,
    formRef,
    form,
    isEdit,
    openCreate,
    openEdit,
    close,
    submit
  }
}
