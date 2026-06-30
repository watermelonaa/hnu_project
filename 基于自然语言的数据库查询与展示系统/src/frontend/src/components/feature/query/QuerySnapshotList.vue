<!--
  @file components/feature/query/QuerySnapshotList.vue
  @description 查询快照列表组件

  功能：
  - 显示查询快照列表
  - 支持选择、查看详情、查看对话、重新执行、删除等操作
  - 支持多选和对比功能

  @author Frontend Team
-->
<template>
  <div class="space-y-3">
    <div
      v-for="query in snapshots"
      :key="query.id"
      :class="`p-3 rounded-lg flex items-center justify-between ${selectedIds.has(query.id) ? 'bg-primary/10' : 'bg-white'}`"
    >
      <div class="flex flex-col flex-1">
        <div class="flex items-center">
          <input
            v-if="selectable"
            type="checkbox"
            :checked="selectedIds.has(query.id)"
            @change.stop="(e) => handleSelect(query.id, (e.target as HTMLInputElement).checked)"
            class="mr-4 h-4 w-4 text-primary focus:ring-primary/50 border-gray-300 rounded cursor-pointer"
          />
          <p class="text-sm font-semibold text-dark flex-1">{{ query.userPrompt || '查询结果' }}</p>
        </div>
        <div class="flex flex-wrap gap-x-6 gap-y-1 mt-1 text-xs text-gray-500" :class="selectable ? 'ml-8' : ''">
          <span>执行于: {{ formatDate(query.queryTime) }}</span>
          <span>耗时: {{ query.executionTime }}</span>
          <span>大模型: {{ query.model }}</span>
          <span>数据库: {{ query.database }}</span>
          <span v-if="showConversation">所属对话: "{{ getConversationTitle(query.conversationId) }}"</span>
        </div>
      </div>
      <div class="flex space-x-3 text-xs">
        <button
          @click.stop="() => $emit('view-detail', query)"
          class="text-primary hover:underline"
        >
          查看详情
        </button>
        <button
          v-if="showConversation"
          @click.stop="() => query.conversationId && $emit('view-in-chat', query.conversationId)"
          :disabled="!query.conversationId"
          :class="query.conversationId ? 'text-primary hover:underline' : 'text-gray-400 cursor-not-allowed'"
        >
          查看对话
        </button>
        <button
          @click.stop="() => $emit('rerun', query.userPrompt)"
          class="text-primary hover:underline"
        >
          重新执行
        </button>
        <button
          @click.stop="() => $emit('delete', query.id)"
          class="text-danger hover:underline"
        >
          删除
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QueryResultData } from '../../../types'

interface Props {
  snapshots: QueryResultData[]
  selectedIds?: Set<string>
  selectable?: boolean
  showConversation?: boolean
  getConversationTitle?: (id: string) => string
}

const props = withDefaults(defineProps<Props>(), {
  selectedIds: () => new Set<string>(),
  selectable: true,
  showConversation: true,
  getConversationTitle: (id: string) => id || '无对话',
})

const emit = defineEmits<{
  'select': [id: string, checked: boolean]
  'view-detail': [query: QueryResultData]
  'view-in-chat': [conversationId: string]
  'rerun': [prompt: string]
  'delete': [id: string]
}>()

const handleSelect = (id: string, checked: boolean) => {
  emit('select', id, checked)
}

// 格式化日期
const formatDate = (date: string | Date): string => {
  try {
    const d = typeof date === 'string' ? new Date(date) : date
    if (isNaN(d.getTime())) {
      return '无效日期'
    }
    return d.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch (e) {
    return '无效日期'
  }
}
</script>

