<template>
  <!-- 现代化聊天消息样式 -->
  <div :class="wrapperClass">
    <!-- 消息容器：包含复制按钮 -->
    <div :class="['group relative flex flex-col', isUser ? 'items-end' : 'items-start']">
      <div :class="bubbleClass">
        <template v-if="typeof content === 'string'">
          <p class="whitespace-pre-wrap leading-relaxed break-words">{{ content }}</p>
        </template>
        <template v-else>
          <QueryResult
            :result="content"
            :saved-queries="savedQueries"
            @save-query="onSaveQuery"
            @share-query="onShareQuery"
          />
        </template>
      </div>

      <!-- 快捷复制按钮：仅针对用户提问显示 -->
      <div 
        v-if="isUser && typeof content === 'string'"
        class="mt-1 opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-2"
      >
        <button 
          @click="handleCopy"
          class="flex items-center gap-1.5 px-2 py-1 text-[11px] font-medium text-gray-400 hover:text-blue-500 hover:bg-blue-50 rounded transition-colors"
          title="复制提问内容"
        >
          <i :class="['fa', copySuccess ? 'fa-check text-green-500' : 'fa-copy']"></i>
          {{ copySuccess ? '已复制' : '复制提问' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Message, QueryResultData } from '../../../types'
import QueryResult from '../query/QueryResult.vue'

interface ChatMessageProps {
  message: Message
  onSaveQuery: (query: QueryResultData) => void
  onShareQuery: (queryId: string, friendId: string) => void
  savedQueries: QueryResultData[]
}

const props = defineProps<ChatMessageProps>()

const { role, content } = props.message

// 复制状态
const copySuccess = ref(false)

// 计算属性
const isUser = computed(() => role === 'user')
// 消息容器样式
const wrapperClass = computed(
  () => `w-full flex ${isUser.value ? 'justify-end' : 'justify-start'} mb-4`,
)
// 消息气泡样式，添加最大宽度和文本截断
const bubbleClass = computed(
  () =>
    `inline-block p-4 md:p-5 text-base rounded-2xl transition-all duration-200 max-w-[100%] break-words ${
      isUser.value 
        ? 'bg-gradient-to-br from-blue-500 to-blue-600 text-white shadow-lg hover:shadow-xl' 
        : 'bg-white border-2 border-gray-100 shadow-md hover:shadow-lg text-gray-800'
    }`,
  // 用户消息：蓝色渐变，AI消息：白色卡片，最大宽度85%并支持文本截断
)
// 注意：消息宽度保持max-w-[75%]，输入框宽度通过calc(75% + 2rem)来匹配，确保等宽
// 复制功能
const handleCopy = async () => {
  if (typeof content !== 'string') return
  
  try {
    await navigator.clipboard.writeText(content)
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}
</script>