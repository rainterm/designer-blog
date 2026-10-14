import { computed, ref, watch } from 'vue'
import { getDictDataByType } from '@/modules/system/api/system'

/**
 * 按字典类型加载启用项，供 n-select 等使用。
 * @param {string | import('vue').Ref<string> | (() => string)} dictType
 */
export function useDict(dictType) {
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
      value: d.dictValue
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
    } finally {
      loading.value = false
    }
  }

  function labelOf(value) {
    if (value == null || value === '') return '—'
    return labelMap.value[value] ?? String(value)
  }

  watch(resolvedType, load, { immediate: true })

  return { loading, items, options, labelMap, labelOf, load }
}
