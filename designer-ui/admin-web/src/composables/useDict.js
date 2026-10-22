import { computed, ref, watch } from 'vue'
import { getDictDataByType } from '@/modules/system/api/system'

/**
 * 把字典键值转成下拉可直接使用的值。
 *
 * <p>库里 `dict_value` 一律是字符串，但实体字段常常是 `Integer`
 * （status / dataScope …）。n-select 是按全等匹配的，`'1' !== 1`，
 * 不转换就会「下拉能选、选中后不回显」。所以数值型字典要显式声明 `numeric`。</p>
 */
function toValue(raw, numeric) {
  if (!numeric) return raw
  const n = Number(raw)
  return Number.isFinite(n) ? n : raw
}

/**
 * 按字典类型加载启用项，供 n-select / 标签渲染使用。
 *
 * <p>字典是「码 → 文案」的唯一来源：文案在后台维护，页面不要再自己写
 * `[{ label: '正常', value: 1 }]` 这类数组，否则改一次文案要全仓搜。</p>
 *
 * @param {string | import('vue').Ref<string> | (() => string)} dictType 字典类型标识
 * @param {{ numeric?: boolean }} [options] numeric=true 时把键值转成数字（用于 Integer 字段）
 */
export function useDict(dictType, { numeric = false } = {}) {
  const loading = ref(false)
  const items = ref([])

  const resolvedType = computed(() => {
    if (typeof dictType === 'function') return dictType()
    if (dictType && typeof dictType === 'object' && 'value' in dictType) return dictType.value
    return dictType
  })

  const options = computed(() =>
    (items.value || []).map((d) => ({
      label: d.dictLabel,
      value: toValue(d.dictValue, numeric)
    }))
  )

  const labelMap = computed(() => {
    const m = {}
    for (const d of items.value || []) {
      m[d.dictValue] = d.dictLabel
    }
    return m
  })

  async function load() {
    const type = resolvedType.value
    if (!type) {
      items.value = []
      return
    }
    loading.value = true
    try {
      const res = await getDictDataByType(type)
      items.value = res.data || []
    } catch {
      // 请求拦截器已统一提示；这里兜底成空字典，页面按「码值原样显示」降级
      items.value = []
    } finally {
      loading.value = false
    }
  }

  /**
   * 码值 → 文案；字典里没有该码时原样返回码值。
   *
   * <p>不用空串兜底：历史数据里可能存着字典中已删除的码，
   * 显示 `OTHER` 至少还能看出是哪一类，空白单元格则完全无法判断。</p>
   */
  function labelOf(value) {
    if (value == null || value === '') return '—'
    return labelMap.value[value] ?? String(value)
  }

  watch(resolvedType, load, { immediate: true })

  return { loading, items, options, labelMap, labelOf, load }
}
