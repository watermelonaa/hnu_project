<!--
  @file components/ui/FilterDropdown.vue
  @description 筛选下拉框组件

  功能：
  - 列表筛选选择器
  - 统一的筛选 UI 样式
  - 双向绑定支持

  @author Frontend Team
-->
<template>
  <div class="relative">
    <select
      :value="value"
      @change="handleChange"
      class="px-3 py-1.5 text-sm rounded-md border border-gray-300 bg-white font-bold focus:ring-2 focus:ring-primary/30 pr-6 appearance-none"
      :style="selectStyle"
    >
      <option v-for="option in options" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
interface Props {
  label: string
  type: 'date' | 'model' | 'database'
  options: Array<{ value: string; label: string }>
  value: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  change: [type: 'date' | 'model' | 'database', value: string]
}>()

const selectStyle = {
  backgroundImage:
    "url(\"data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23333' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e\")",
  backgroundRepeat: 'no-repeat',
  backgroundPosition: 'right 0.5rem center',
  backgroundSize: '1em',
}

const handleChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  emit('change', props.type, target.value)
}
</script>
