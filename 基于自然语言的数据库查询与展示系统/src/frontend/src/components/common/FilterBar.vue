<!--
  @file components/common/FilterBar.vue
  @description 筛选条组件

  功能：
  - 模型/数据库选择
  - 筛选条件组合
  - 统一的筛选 UI

  @author Frontend Team
-->
<template>
  <div class="filter-bar">
    <div class="flex items-center justify-end space-x-2 mb-2">
      <!-- 模型选择器 -->
      <FilterSelect
        v-if="showModelFilter"
        :type="'model'"
        :label="modelLabel"
        :options="modelOptions"
        :value="currentModelName"
        @change="handleModelChange"
        :icon="modelIcon"
        :loading="modelLoading"
      />

      <!-- 数据库选择器 -->
      <FilterSelect
        v-if="showDatabaseFilter"
        :type="'database'"
        :label="databaseLabel"
        :options="databaseOptions"
        :value="currentDatabaseName"
        @change="handleDatabaseChange"
        :icon="databaseIcon"
        :loading="databaseLoading"
      />

      <!-- 自定义筛选器插槽 -->
      <slot name="custom-filters"></slot>

      <!-- 操作按钮插槽 -->
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import FilterSelect from './FilterSelect.vue'

interface Option {
  id: string
  name: string
  disabled?: boolean
  description?: string
}

interface Props {
  // 模型筛选器配置
  showModelFilter?: boolean
  modelOptions: Option[]
  currentModelName: string
  modelLabel?: string
  modelIcon?: string
  modelLoading?: boolean

  // 数据库筛选器配置
  showDatabaseFilter?: boolean
  databaseOptions: Option[]
  currentDatabaseName: string
  databaseLabel?: string
  databaseIcon?: string
  databaseLoading?: boolean

  // 通用配置
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showModelFilter: true,
  showDatabaseFilter: true,
  modelLabel: '大模型',
  modelIcon: 'fa-cogs',
  databaseLabel: '数据库',
  databaseIcon: 'fa-database',
  compact: false,
  modelLoading: false,
  databaseLoading: false,
})

const emit = defineEmits<{
  'model-change': [modelId: string, modelName: string]
  'database-change': [databaseId: string, databaseName: string]
}>()

const handleModelChange = (value: string) => {
  const selectedOption = props.modelOptions.find((opt) => opt.name === value)
  if (selectedOption) {
    emit('model-change', selectedOption.id, value)
  }
}

const handleDatabaseChange = (value: string) => {
  const selectedOption = props.databaseOptions.find((opt) => opt.name === value)
  if (selectedOption) {
    emit('database-change', selectedOption.id, value)
  }
}
</script>

<style scoped>
.filter-bar {
  @apply w-full;
}
</style>
