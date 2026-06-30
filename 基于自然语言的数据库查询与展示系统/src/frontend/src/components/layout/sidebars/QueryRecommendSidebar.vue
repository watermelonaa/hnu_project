<!--
  @file components/layout/sidebars/QueryRecommendSidebar.vue
  @description 查询推荐侧边栏

  功能：
  - 基于当前查询结果推荐相关查询
  - 查询失败时的建议
  - 可折叠显示（通过星键控制）

  布局说明：
  - 常用推荐卡片：白色背景圆角，底部边框
  - 悬浮星键：固定在右上角

  @author Frontend Team
-->
<template>
  <!-- 侧边栏容器：显示触发按钮和下拉菜单 -->
  <div
    :class="[
      'relative flex flex-col items-end gap-3',
      className?.includes('h-full') ? 'h-full' : '',
      className,
    ]"
  >
    <!-- 常用搜索触发按钮和下拉菜单 -->
    <div class="relative" ref="commonButtonRef">
      <button
        @click="toggleCommonMenu"
        class="w-10 h-10 rounded-lg bg-white border border-gray-300 shadow-md hover:bg-gray-50 hover:shadow-lg transition-all flex items-center justify-center text-gray-600 hover:text-primary"
        title="常用搜索"
      >
        <span class="text-lg">⭐</span>
      </button>
      
      <!-- 常用搜索下拉菜单：在按钮左侧显示，不遮挡按钮 -->
      <!-- 注意：使用!important确保样式不被覆盖，避免与页面其他样式冲突 -->
      <transition name="fade-slide">
        <div
          v-if="isCommonMenuOpen"
          ref="commonMenuRef"
          class="absolute right-full top-0 mr-2 w-64 bg-gradient-to-br from-white via-blue-50/30 to-indigo-50/20 border-2 border-primary/20 rounded-xl shadow-xl overflow-hidden backdrop-blur-sm"
          style="z-index: 10000 !important; position: absolute !important;"
          @click.stop
        >
          <div class="p-2 space-y-1 bg-white/50 backdrop-blur-sm">
            <div class="px-3 py-2 text-sm font-semibold text-gray-800 border-b border-primary/20 mb-1 bg-gradient-to-r from-primary/10 to-transparent">
              <i class="fa fa-star text-primary mr-2"></i>智能推荐查询
            </div>
            <!-- 加载状态 -->
            <div v-if="isLoadingCommonSearches" class="px-3 py-2 text-sm text-gray-500 text-center">
              <i class="fa fa-spinner fa-spin mr-2"></i>AI正在思考中...
            </div>
            <!-- 推荐列表 -->
            <button
              v-else
              v-for="query in commonSearches"
              :key="query"
              @click="handleRecommendationClick(query)"
              class="w-full text-left px-3 py-2 text-sm rounded-md hover:bg-gray-100 transition-colors"
            >
              {{ query }}
            </button>
            <!-- 错误提示 -->
            <div v-if="commonSearchError" class="px-3 py-2 text-xs text-red-500">
              {{ commonSearchError }}
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 大模型思考触发按钮和下拉菜单 - 只在有建议时显示 -->
    <div v-if="showSuggestions" class="relative" ref="suggestionsButtonRef">
      <button
        @click="toggleSuggestionsMenu"
        class="w-10 h-10 rounded-lg bg-white border border-gray-300 shadow-md hover:bg-gray-50 hover:shadow-lg transition-all flex items-center justify-center text-gray-600 hover:text-primary"
        title="大模型思考"
      >
        <i class="fa fa-magic text-base"></i>
      </button>
      
      <!-- 大模型思考下拉菜单：在按钮左侧显示，不遮挡按钮 -->
      <!-- 注意：使用!important确保样式不被覆盖，避免与页面其他样式冲突 -->
      <transition name="fade-slide">
        <div
          v-if="isSuggestionsMenuOpen"
          ref="suggestionsMenuRef"
          class="absolute right-full top-0 mr-2 w-64 bg-gradient-to-br from-white via-purple-50/30 to-indigo-50/20 border-2 border-purple-200/30 rounded-xl shadow-xl overflow-hidden backdrop-blur-sm"
          style="z-index: 10000 !important; position: absolute !important;"
          @click.stop
        >
          <div class="p-2 space-y-1 bg-white/50 backdrop-blur-sm">
            <div class="px-3 py-2 text-sm font-semibold text-gray-800 border-b border-purple-200/30 mb-1 bg-gradient-to-r from-purple-100/50 to-transparent">
              <i class="fa fa-magic text-purple-600 mr-2"></i>大模型思考
            </div>
            <!-- 状态提示 -->
            <div
              :class="[
                'mx-2 mb-2 p-2 rounded-md text-xs',
                queryFailed ? 'bg-red-50 text-red-600 border border-red-100' : 'bg-green-50 text-green-600 border border-green-100'
              ]"
            >
              {{ queryFailed ? '查询似乎遇到了问题，您可以尝试：' : '基于当前结果，您可以继续探索：' }}
            </div>
            <!-- 加载状态 -->
            <div v-if="isLoadingRecommendations" class="px-3 py-2 text-sm text-gray-500 text-center">
              <i class="fa fa-spinner fa-spin mr-2"></i>AI正在思考中...
            </div>
            <!-- 推荐列表 -->
            <button
              v-else
              v-for="(query, index) in relatedSearches"
              :key="index"
              @click="handleRecommendationClick(query)"
              class="w-full text-left px-3 py-2 text-sm rounded-md hover:bg-gray-100 transition-colors"
            >
              {{ query }}
            </button>
            <!-- 错误提示 -->
            <div v-if="recommendationError" class="px-3 py-2 text-xs text-red-500">
              {{ recommendationError }}
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import type { Conversation, Message, QueryResultData } from '../../../types'
import { recommendationApi } from '../../../services/api/query'

// 推荐查询常量
const COMMON_RECOMMENDATIONS = [
  '近7天用户增长趋势',
  '上个季度各产品线销售额对比',
  '查询华东地区销量最高的产品',
  '统计每月新增用户数',
]

const MOCK_SUCCESS_SUGGESTIONS = [
  '按地区细分订单量',
  '与去年同期数据进行对比',
  '分析各季度订单的平均金额',
]

const MOCK_FAILURE_SUGGESTIONS = [
  '换一种更简单的问法',
  '检查是否选择了正确的数据源',
  '尝试询问"你能做什么？"',
]

// --- 1. Props 和 Emits ---

interface RightSidebarProps {
  currentConversation: Conversation | undefined
  className?: string
  dbConnectionId?: number
  llmConfigId?: string
}

// 【修改1】添加新的props来接收followupQuestions
const props = defineProps<RightSidebarProps & {
  followupQuestions?: string[] // 【新增】接收后端返回的后续问题
}>()

const emit = defineEmits<{
  (e: 'recommendation-click', prompt: string): void
  (e: 'open-common'): void
  (e: 'open-suggestions'): void
}>()

// --- 2. 下拉菜单状态 ---
const isCommonMenuOpen = ref(false)
const isSuggestionsMenuOpen = ref(false)
const commonButtonRef = ref<HTMLDivElement | null>(null)
const suggestionsButtonRef = ref<HTMLDivElement | null>(null)
const commonMenuRef = ref<HTMLDivElement | null>(null)
const suggestionsMenuRef = ref<HTMLDivElement | null>(null)

// --- 2. Helper Type Guard ---
const isQueryResult = (content: any): content is QueryResultData => {
  return content && typeof content === 'object' && 'sqlQuery' in content
}

// --- 3. 计算属性 ---

// 获取最新一条消息
const lastMessage = computed<Message | undefined>(() => {
  const messages = props.currentConversation?.messages
  if (!messages || messages.length === 0) return undefined
  return messages[messages.length - 1]
})

// 是否应该显示建议
const showSuggestions = computed<boolean>(() => {
  const conv = props.currentConversation
  return (
    !!conv && conv.messages.length > 1 && !!lastMessage.value && lastMessage.value.role === 'ai'
  )
})

// 查询是否失败
const queryFailed = computed<boolean>(() => {
  if (!showSuggestions.value) return false
  if (!isQueryResult(lastMessage.value?.content)) {
    return true
  }
  return false
})

// 相关搜索/建议列表
const relatedSearches = ref<string[]>([])
const isLoadingRecommendations = ref(false)
const recommendationError = ref<string | null>(null)

// 【新增】常用搜索推荐列表
const commonSearches = ref<string[]>(COMMON_RECOMMENDATIONS)
const isLoadingCommonSearches = ref(false)
const commonSearchError = ref<string | null>(null)

// 监听对话变化，更新推荐
watch(
  () => [
    props.currentConversation?.messages, 
    queryFailed.value,
    props.followupQuestions // 【新增】监听followupQuestions变化
  ],
  () => {
    if (showSuggestions.value) {
      updateRecommendations()
    } else {
      relatedSearches.value = queryFailed.value ? MOCK_FAILURE_SUGGESTIONS : MOCK_SUCCESS_SUGGESTIONS
    }
  },
  { immediate: true, deep: true }
)

// --- 4. Methods ---

// 切换常用搜索菜单
const toggleCommonMenu = async () => {
  isCommonMenuOpen.value = !isCommonMenuOpen.value
  if (isCommonMenuOpen.value) {
    isSuggestionsMenuOpen.value = false
    // 【修改】如果还没有加载过推荐，则加载
    if (commonSearches.value.length === COMMON_RECOMMENDATIONS.length || 
        JSON.stringify(commonSearches.value) === JSON.stringify(COMMON_RECOMMENDATIONS)) {
      await loadCommonRecommendations()
    }
  }
}

// 【新增】加载常用搜索推荐
const loadCommonRecommendations = async () => {
  if (!props.dbConnectionId || !props.llmConfigId) {
    console.log('缺少必要参数，使用默认推荐')
    commonSearches.value = COMMON_RECOMMENDATIONS
    return
  }

  try {
    isLoadingCommonSearches.value = true
    commonSearchError.value = null
    
    // 构建对话历史
    const conversationHistory: Array<{
      role: 'user' | 'ai'
      content: string | any
    }> = []
    
    if (props.currentConversation) {
      props.currentConversation.messages.forEach(msg => {
        conversationHistory.push({
          role: msg.role === 'user' ? 'user' : 'ai',
          content: msg.content
        })
      })
    }

    console.log('请求智能推荐查询...')
    const recommendations = await recommendationApi.getRecommendations({
      conversationId: props.currentConversation?.id,
      conversationHistory,
      dbConnectionId: props.dbConnectionId,
      llmConfigId: props.llmConfigId,
    })

    if (recommendations && recommendations.length > 0) {
      commonSearches.value = recommendations
      console.log('✓ 获取到智能推荐:', recommendations)
    } else {
      commonSearches.value = COMMON_RECOMMENDATIONS
    }
  } catch (error) {
    console.error('加载智能推荐失败:', error)
    commonSearchError.value = '加载推荐失败，显示默认推荐'
    commonSearches.value = COMMON_RECOMMENDATIONS
  } finally {
    isLoadingCommonSearches.value = false
  }
}

// 【新增】在组件挂载时自动加载推荐
onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside)
  
  // 自动加载智能推荐
  if (props.dbConnectionId && props.llmConfigId) {
    console.log('页面加载，自动加载智能推荐...')
    loadCommonRecommendations()
  }
})

// 切换大模型思考菜单
const toggleSuggestionsMenu = () => {
  isSuggestionsMenuOpen.value = !isSuggestionsMenuOpen.value
  if (isSuggestionsMenuOpen.value) {
    isCommonMenuOpen.value = false
  }
}

// 【修改2】更新推荐方法 - 直接使用后端返回的followupQuestions
const updateRecommendations = () => {
  // 优先使用props传入的followupQuestions
  if (props.followupQuestions && props.followupQuestions.length > 0) {
    relatedSearches.value = props.followupQuestions
    console.log('✓ 使用后端返回的后续问题，共', props.followupQuestions.length, '条')
    return
  }
  
  // 【修改3】如果没有传入followupQuestions，才使用原来的逻辑（但简化很多）
  loadFallbackSuggestions()
}

// 【新增】备用的推荐加载方法
const loadFallbackSuggestions = () => {
  // 如果没有传入followupQuestions，使用默认的推荐
  if (queryFailed.value) {
    relatedSearches.value = MOCK_FAILURE_SUGGESTIONS
  } else {
    // 尝试从对话中提取用户问题来生成相关推荐
    const lastUserMessage = getLastUserMessage()
    if (lastUserMessage) {
      // 基于用户问题生成简单的相关推荐
      relatedSearches.value = generateSimpleSuggestions(lastUserMessage)
    } else {
      relatedSearches.value = MOCK_SUCCESS_SUGGESTIONS
    }
  }
}

// 【新增】获取最后一条用户消息
const getLastUserMessage = (): string | null => {
  if (!props.currentConversation) return null
  const messages = props.currentConversation.messages
  for (let i = messages.length - 1; i >= 0; i--) {
    if (messages[i].role === 'user' && typeof messages[i].content === 'string') {
      return messages[i].content as string
    }
  }
  return null
}

// 【新增】生成简单的相关推荐
const generateSimpleSuggestions = (userQuestion: string): string[] => {
  const suggestions: string[] = []
  
  // 基于关键词生成相关建议
  const lowerQuestion = userQuestion.toLowerCase()
  
  if (lowerQuestion.includes('销售') || lowerQuestion.includes('revenue') || lowerQuestion.includes('sales')) {
    suggestions.push('按月查看销售趋势')
    suggestions.push('按产品类别统计销售额')
    suggestions.push('销售额最高的月份')
  } else if (lowerQuestion.includes('用户') || lowerQuestion.includes('customer') || lowerQuestion.includes('user')) {
    suggestions.push('用户注册时间分布')
    suggestions.push('用户地域分布统计')
    suggestions.push('活跃用户数量')
  } else if (lowerQuestion.includes('订单') || lowerQuestion.includes('order')) {
    suggestions.push('订单金额分布')
    suggestions.push('订单处理时间')
    suggestions.push('订单状态统计')
  } else {
    // 通用建议
    suggestions.push('查看详细数据')
    suggestions.push('分析趋势变化')
    suggestions.push('对比不同时间段')
    suggestions.push('统计汇总信息')
  }
  
  return suggestions.length > 0 ? suggestions : MOCK_SUCCESS_SUGGESTIONS
}

// 【修改4】删除原来的loadAISuggestions方法，替换为上面的updateRecommendations

// 处理推荐点击事件
const handleRecommendationClick = (prompt: string) => {
  emit('recommendation-click', prompt)
  isCommonMenuOpen.value = false
  isSuggestionsMenuOpen.value = false
}

// 点击外部关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as Node
  
  const isClickInCommon = 
    (commonButtonRef.value && commonButtonRef.value.contains(target)) ||
    (commonMenuRef.value && commonMenuRef.value.contains(target))
  
  const isClickInSuggestions = 
    (suggestionsButtonRef.value && suggestionsButtonRef.value.contains(target)) ||
    (suggestionsMenuRef.value && suggestionsMenuRef.value.contains(target))
  
  if (!isClickInCommon && !isClickInSuggestions) {
    isCommonMenuOpen.value = false
    isSuggestionsMenuOpen.value = false
  }
}

// 点击外部关闭菜单
onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside)
  
  // 【新增】自动加载智能推荐
  if (props.dbConnectionId && props.llmConfigId) {
    console.log('页面加载，自动加载智能推荐...')
    loadCommonRecommendations()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})
</script>

<style scoped>
/* 下拉菜单淡入滑出动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease-out;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>