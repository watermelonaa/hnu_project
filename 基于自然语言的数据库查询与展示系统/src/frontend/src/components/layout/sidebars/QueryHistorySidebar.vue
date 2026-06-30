<!--
  @file components/layout/sidebars/QueryHistorySidebar.vue
  @description 查询历史侧边栏

  功能：
  - 显示对话/会话列表
  - 切换/删除对话
  - 新建对话
  - 可折叠显示

  @author Frontend Team
-->
<template>
  <div :class="sidebarClasses">
    <div class="w-80 h-full flex flex-col overflow-hidden">
      <div class="flex justify-between items-center p-4 border-b">
        <h2 class="text-xl font-bold">对话历史</h2>
        <button
          @click="handleClose"
          class="text-gray-500 hover:text-gray-700 transition-colors p-2"
        >
          <i class="fa fa-times text-lg"></i>
        </button>
      </div>

      <div class="p-4 border-b">
        <button
          @click="newConversationAndClose"
          class="w-full bg-primary text-white py-2 rounded-lg hover:bg-primary-dark transition-colors flex items-center justify-center space-x-2 shadow-md"
        >
          <i class="fa fa-plus-circle"></i>
          <span>新建对话</span>
        </button>
      </div>

      <div class="flex-1 overflow-y-auto px-4 py-3 space-y-2">
        <div v-if="isLoading" class="text-center py-8">
          <i class="fa fa-spinner fa-spin text-primary text-2xl mb-2"></i>
          <p class="text-gray-500 text-sm">加载对话历史中...</p>
        </div>
        <p v-else-if="displayConversations.length === 0" class="text-gray-500 text-center py-4">暂无对话记录</p>

        <div
          v-for="conv in displayConversations"
          :key="conv.id"
          :class="[
            'flex items-center justify-between p-3 rounded-lg cursor-pointer transition-colors',
            conv.id === currentConversationId
              ? 'bg-primary/10 border border-primary'
              : 'hover:bg-gray-100',
          ]"
        >
          <div @click="switchAndClose(conv.id)" class="flex-1 min-w-0 pr-2">
            <p
              class="truncate font-medium text-sm"
              :class="{ 'text-primary': conv.id === currentConversationId }"
            >
              {{ conv.title || '无标题对话' }}
            </p>
            <p class="text-xs text-gray-500 mt-0.5">
              创建于: {{ new Date(conv.createTime).toLocaleDateString() }}
            </p>
          </div>

          <button
            @click.stop="startDeleteConfirmation(conv.id)"
            class="text-gray-400 hover:text-danger p-1 rounded-full transition-colors"
            title="删除对话"
          >
            <i class="fa fa-trash text-sm"></i>
          </button>
        </div>
      </div>
    </div>
  </div>

  <div
    v-if="isOpen"
    @click="handleClose"
    class="fixed inset-0 bg-black/30 z-30 transition-opacity duration-300"
  ></div>

  <div
    v-if="showDeleteConfirm"
    class="fixed inset-0 flex items-center justify-center z-50 bg-black/40"
  >
    <div class="bg-white rounded-lg shadow-2xl p-6 w-full max-w-sm" @click.stop>
      <h3 class="text-lg font-medium text-gray-900 mb-2">确认删除</h3>
      <p class="text-gray-600 text-sm mb-6">确定要删除这条对话吗？删除后无法恢复。</p>
      <div class="flex justify-end gap-3">
        <button
          @click="cancelDelete"
          class="px-4 py-2 border border-gray-300 rounded-md text-sm hover:bg-gray-50"
        >
          取消
        </button>
        <button
          @click="confirmDelete"
          class="px-4 py-2 bg-danger text-white rounded-md text-sm hover:bg-red-600"
        >
          确认删除
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
// 确保 Conversation 类型在您设置的统一类型文件中可用
import type { Conversation } from '../../../types'

// --- 1. Props 和 Emits ---

interface HistorySidebarProps {
  isOpen: boolean
  conversations: Conversation[]
  currentConversationId: string
  isLoading?: boolean
}

const props = defineProps<HistorySidebarProps>()

const emit = defineEmits<{
  // 所有的 on... 函数都转换为事件 (使用 kebab-case)
  (e: 'close'): void
  (e: 'switch-conversation', id: string): void
  (e: 'new-conversation'): void
  (e: 'delete-conversation', id: string): void
}>()

// --- 2. 内部状态 ---

// 删除确认弹窗状态
const showDeleteConfirm = ref(false)
// 待删除对话的 ID
const deleteTargetId = ref<string | null>(null)

// --- 3. Computed Properties (计算属性) ---

// 过滤掉初始对话和空对话，只显示有效的历史对话
const displayConversations = computed(() => {
  const filtered = props.conversations.filter((conv) => {
    // 过滤掉初始对话（ID以'conv-initial'开头或没有标题）
    if (conv.id === 'conv-initial' || conv.id.startsWith('conv-initial')) {
      return false
    }
    
    // 如果对话有后端保存的轮次数（totalRounds > 0），则显示
    // 即使消息详情还没有加载（只有初始欢迎消息）
    if (conv.totalRounds && conv.totalRounds > 0) {
      return true
    }
    
    // 过滤掉只有一条欢迎消息的本地临时对话
    if (conv.messages.length <= 1) {
      return false
    }
    
    return true
  })
  
  console.log('QueryHistorySidebar - 显示的对话数量:', filtered.length, filtered.map(c => ({ id: c.id, title: c.title, totalRounds: c.totalRounds, messages: c.messages.length })))
  return filtered
})

// 侧边栏动态类名
const sidebarClasses = computed(() => [
  'bg-white border-l border-gray-200 h-full flex flex-col',
  'transition-transform duration-300 ease-in-out',
  'fixed top-0 right-0 z-40 transform',
  // 控制侧边栏的显示/隐藏
  props.isOpen ? 'translate-x-0' : 'translate-x-full',
  'w-80 shadow-lg',
])

// --- 4. Methods (事件处理) ---

// 启动删除流程：打开确认弹窗并设置目标ID
const startDeleteConfirmation = (id: string) => {
  deleteTargetId.value = id
  showDeleteConfirm.value = true
}

// 取消删除
const cancelDelete = () => {
  showDeleteConfirm.value = false
  deleteTargetId.value = null
}

// 确认删除并触发父组件事件
const confirmDelete = () => {
  if (deleteTargetId.value) {
    emit('delete-conversation', deleteTargetId.value)
  }
  cancelDelete() // 重置状态
}

// 切换对话并关闭侧边栏
const switchAndClose = (id: string) => {
  emit('switch-conversation', id)
  emit('close') // 切换后通常关闭历史面板
}

// 新建对话并关闭侧边栏
const newConversationAndClose = () => {
  emit('new-conversation')
  emit('close')
}

// 关闭侧边栏
const handleClose = () => {
  emit('close')
}
</script>
