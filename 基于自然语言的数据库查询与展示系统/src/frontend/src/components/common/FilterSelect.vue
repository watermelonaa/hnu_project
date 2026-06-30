<!--
  @file components/common/FilterSelect.vue
  @description 带图标的筛选选择器

  功能：
  - 下拉选择框
  - 可配置图标
  - 紧凑/标准模式

  @author Frontend Team
-->
<template>
  <div class="relative inline-block" :class="{ compact: compact }">
    <!-- 带图标的选择器 -->
    <div class="relative">
      <select
        :value="value"
        @change="handleChange"
        :disabled="disabled || loading"
        :class="[
          'filter-select',
          'px-3 py-1.5 text-sm rounded-md border bg-white font-medium',
          'focus:ring-2 focus:ring-primary/30 focus:border-primary/50',
          disabled || loading ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : 'cursor-pointer',
          compact ? 'pl-8 pr-6' : 'pl-10 pr-6',
          hasIcon ? 'with-icon' : '',
        ]"
        :style="selectStyle"
      >
        <!-- 占位符选项 -->
        <option v-if="placeholder && !compact" value="" disabled hidden>
          {{ placeholder }}
        </option>

        <!-- 选项列表 -->
        <option
          v-for="option in options"
          :key="option.value || option.id"
          :value="option.value || option.name"
          :disabled="option.disabled"
        >
          {{ option.label || option.name }}
        </option>
      </select>

      <!-- 图标 -->
      <div
        v-if="icon"
        class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"
      >
        <i :class="`fa ${icon}`"></i>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none">
        <div
          class="animate-spin h-4 w-4 border-2 border-primary/30 border-t-primary rounded-full"
        ></div>
      </div>
    </div>

    <!-- 标签（紧凑模式下显示在左侧） -->
    <label v-if="label && !compact" class="block text-xs font-medium text-gray-500 mb-1">
      {{ label }}
    </label>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Option {
  id?: string
  name?: string
  value?: string
  label?: string
  disabled?: boolean
}

interface Props {
  // 基础属性
  type?: string
  label?: string
  value: string
  options: Option[]

  // 配置属性
  placeholder?: string
  icon?: string
  compact?: boolean

  // 状态属性
  disabled?: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  compact: false,
  disabled: false,
  loading: false,
})

const emit = defineEmits<{
  change: [value: string]
  'update:value': [value: string]
}>()

const selectStyle = computed(() => ({
  backgroundImage:
    "url(\"data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23333' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e\")",
  backgroundRepeat: 'no-repeat',
  backgroundPosition: 'right 0.5rem center',
  backgroundSize: '1em',
  paddingRight: 'calc(1em + 1rem)',
}))

const hasIcon = computed(() => !!props.icon)

const handleChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  emit('change', target.value)
  emit('update:value', target.value)
}
</script>

<style scoped>
.filter-select {
  transition: all 0.2s ease;
  min-width: 140px;
}

.filter-select:focus {
  outline: none;
}

.filter-select.compact {
  min-width: 100px;
  padding-left: 2rem;
}

.filter-select.with-icon {
  padding-left: 2.5rem;
}
</style>
