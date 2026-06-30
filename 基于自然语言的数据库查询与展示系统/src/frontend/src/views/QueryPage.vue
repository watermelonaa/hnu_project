<!--
  @file views/QueryPage.vue
  @description 数据查询页面

  功能：
  - 自然语言查询输入
  - SQL 生成与执行
  - 结果展示（表格/图表）
  - 查询历史侧边栏
  - 推荐查询侧边栏

  布局结构：
  - 最外层：flex容器，无背景色（继承父级）
  - 左侧：聊天消息区域 + 输入区域
  - 右侧：推荐侧边栏（桌面端）/ 移动端覆盖层
  - 覆盖层：历史对话侧边栏

  @author Frontend Team
-->
<template>
  <!-- 最外层：全宽布局，使用纯色背景，使用flex-1确保不被顶部导航栏挤压 -->
  <div class="flex-1 flex flex-col relative min-h-0 bg-gray-50">
    <!-- 主要内容区：flex容器，聊天区域 -->
    <div class="flex-1 flex flex-col px-4 md:px-8 lg:px-12 py-6 overflow-hidden">
      <!-- 聊天区域 - 聊天界面容器，包含消息和输入框 -->
      <div class="flex-1 flex flex-col min-h-0 mx-auto w-full" style="max-width: 900px;">
        <!-- 聊天界面容器：包裹消息区域和输入框，确保输入框固定在底部 -->
        <div class="flex-1 flex flex-col min-h-0">
          <!-- 聊天消息区域 - 自适应高度，可滚动，优化视觉效果，增加右边距避免与推荐按钮重叠 -->
          <div 
            ref="chatContainerRef" 
            class="flex-1 overflow-y-auto min-h-0 py-6 px-2"
            style="scrollbar-width: thin; scrollbar-color: rgba(156, 163, 175, 0.5) transparent; padding-right: 1rem;"
          >
            <!-- 消息容器：居中显示，限制最大宽度，与输入框等宽 -->
            <div class="w-full mx-auto space-y-6">
              <!-- 推荐查询词条：常驻显示在消息列表顶部，方便参考 -->
              <div 
                v-if="allRecommendations.length > 0 && !isLoading"
                class="w-full space-y-2 mb-6"
              >
                <div class="text-sm text-muted-foreground mb-3 px-2 flex items-center justify-between">
                  <span><i class="fa fa-star text-primary mr-2"></i>推荐查询</span>
                </div>
                <div class="flex flex-wrap gap-2">
                  <template v-if="allRecommendations[0] !== '暂无推荐查询'">
                    <button
                      v-for="(recommendation, index) in allRecommendations"
                      :key="index"
                      @click="handleRecommendationClick(recommendation)"
                      class="inline-flex items-center px-4 py-2 bg-card/80 backdrop-blur-sm border border-border rounded-xl hover:bg-accent hover:border-primary/30 transition-all duration-200 text-sm text-foreground hover:text-primary whitespace-nowrap"
                    >
                      {{ recommendation }}
                    </button>
                  </template>
                  <div v-else class="px-3 py-2 text-sm text-gray-400 italic bg-gray-50/50 rounded-lg border border-dashed border-gray-200">
                    暂无推荐查询
                  </div>
                </div>
              </div>

              <!-- 聊天消息 -->
              <ChatMessage
                v-for="(msg, index) in currentConversation?.messages"
                :key="index"
                :message="msg"
                :saved-queries="savedQueries"
                @save-query="handleSaveQuery"
                @share-query="handleShareQuery"
              />
              
              <!-- 查询结果下方的大模型思考推荐 -->
              <div
                v-if="lastMessageIsQueryResult && currentFollowupQuestions.length > 0"
                class="w-full mt-4 space-y-2"
              >
                <div class="text-sm text-muted-foreground mb-2 px-2">
                  <i class="fa fa-magic text-primary mr-2"></i>大模型思考
                </div>
                <div class="flex flex-col gap-2">
                  <button
                    v-for="(question, qIndex) in currentFollowupQuestions"
                    :key="qIndex"
                    @click="handleRecommendationClick(question)"
                    class="inline-flex items-center px-4 py-2 bg-card/50 backdrop-blur-sm border border-border/30 rounded-xl hover:bg-accent/50 hover:border-primary/50 transition-all duration-200 text-sm text-foreground hover:text-primary self-start"
                  >
                    {{ question }}
                  </button>
                </div>
              </div>

              <!-- 加载中状态 - 优化设计 -->
              <div v-if="isLoading" class="w-full flex justify-start">
                <div class="inline-flex items-center space-x-3 px-5 py-4 bg-white rounded-2xl shadow-md border border-gray-100">
                  <div class="flex space-x-1">
                    <div class="w-2 h-2 bg-primary rounded-full animate-bounce" style="animation-delay: 0ms"></div>
                    <div class="w-2 h-2 bg-primary rounded-full animate-bounce" style="animation-delay: 150ms"></div>
                    <div class="w-2 h-2 bg-primary rounded-full animate-bounce" style="animation-delay: 300ms"></div>
                  </div>
                  <span class="text-sm text-gray-600 font-medium">AI正在思考中...</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入框容器：固定在底部，现代化设计，与消息气泡等宽（两边聊天气泡的最远端） -->
          <div class="flex-shrink-0 mt-4 w-full flex justify-center">
            <div class="bg-white rounded-2xl shadow-lg border border-gray-200/50 p-4 backdrop-blur-sm" style="max-width: 900px; width: 100%;">
              <!-- textarea 区域：现代化输入框 -->
              <textarea
                v-model="prompt"
                @keydown.enter.prevent="handleEnterKey"
                @input="handleInputChange"
                @focus="showRecommendations = true"
                placeholder="输入您的查询需求，例如：展示2023年各季度的订单量..."
                class="w-full px-4 py-3 resize-none focus:outline-none text-base bg-gray-50 rounded-xl border-2 border-transparent focus:border-primary/30 focus:bg-white transition-all max-h-40 overflow-y-auto placeholder:text-gray-400"
                :disabled="isLoading"
                rows="1"
              />

              <!-- 按钮区域：优化布局和样式 -->
              <div class="flex items-center justify-between pt-3 mt-3 border-t border-gray-100">
                <!-- 左侧：模型和数据库选择 -->
                <div class="flex items-center gap-3">
                  <!-- 模型选择 - 下拉选择器（只显示当前选中的模型） -->
                  <div class="relative group">
                    <i class="fa fa-brain absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm pointer-events-none z-10 group-hover:text-primary transition-colors"></i>
                    <select 
                      v-model="selectedModelId"
                      :class="[
                        'pl-9 pr-10 py-2 text-sm border-2 rounded-lg bg-white hover:border-primary/30 transition-all appearance-none cursor-pointer min-w-[180px]',
                        selectedModelId ? 'border-primary/50 bg-primary/5 text-gray-800' : 'border-gray-200 text-gray-600'
                      ]"
                      :title="modelOptions.find(m => m.id === selectedModelId)?.name || '选择大模型'"
                      style="white-space: normal; text-overflow: initial;"
                    >
                      <option value="" disabled>选择大模型</option>
                      <option v-for="model in modelOptions" :key="model.id" :value="model.id">
                        {{ model.name }}
                      </option>
                    </select>
                    <i class="fa fa-chevron-down absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs pointer-events-none"></i>
                  </div>
                  
                  <!-- 数据库选择 -->
                  <div class="relative group">
                    <i class="fa fa-database absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm pointer-events-none z-10 group-hover:text-primary transition-colors"></i>
                    <select 
                      v-model="selectedDatabaseId"
                      :class="[
                        'pl-9 pr-10 py-2 text-sm border-2 rounded-lg bg-white hover:border-primary/30 transition-all appearance-none cursor-pointer min-w-[150px]',
                        selectedDatabaseId ? 'border-primary/50 bg-primary/5 text-gray-800' : 'border-gray-200 text-gray-600'
                      ]"
                      :title="databaseOptions.find(d => d.id === selectedDatabaseId)?.name || '选择数据库'"
                      style="white-space: normal; text-overflow: initial;"
                    >
                      <option value="">选择数据库</option>
                      <option v-for="db in databaseOptions" :key="db.id" :value="db.id">
                        {{ db.name }}
                      </option>
                    </select>
                    <i class="fa fa-chevron-down absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs pointer-events-none"></i>
                  </div>
                </div>

                <!-- 右侧：发送/中止按钮 -->
                <button
                  @click="isLoading ? handleStop() : handleSubmit()"
                  :disabled="!isLoading && !prompt.trim()"
                  :class="[
                    'px-6 py-2.5 rounded-xl flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed font-medium shadow-md hover:shadow-lg transition-all transform hover:scale-105 disabled:transform-none min-w-[100px] max-w-[150px]',
                    isLoading 
                      ? 'bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white' 
                      : 'bg-gradient-to-r from-primary to-blue-600 hover:from-primary/90 hover:to-blue-600/90 text-white'
                  ]"
                  :title="isLoading ? '中止查询' : '发送'"
                >
                  <i v-if="isLoading" class="fa fa-stop text-sm flex-shrink-0"></i>
                  <i v-else class="fa fa-paper-plane text-sm flex-shrink-0"></i>
                  <span class="text-sm truncate">{{ isLoading ? '中止' : '发送' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 历史对话侧边栏 - 覆盖层 -->
  <HistorySidebar
    :is-open="isHistoryOpen"
    :conversations="conversations"
    :current-conversation-id="currentConversationId"
    :is-loading="isLoadingHistory"
    @close="toggleHistory"
    @switch-conversation="handleSwitchConversation"
    @new-conversation="handleNewConversation"
    @delete-conversation="handleDeleteConversation"
  />
</template>


<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import type { Conversation, MessageRole, QueryResultData } from '../types'
import ChatMessage from '../components/feature/chat/ChatMessage.vue'
import Dropdown from '../components/ui/Dropdown.vue'
import HistorySidebar from '../components/layout/sidebars/QueryHistorySidebar.vue'
import {
  queryApi,
  llmConfigApi,
  dbConnectionApi,
  queryShareApi,
  queryLogApi,
  dialogApi,
  dialogDetailApi,
  recommendationApi
} from '../services/api.real'
import type { QueryResponse, DialogRecord, DialogDetail } from '../services/api.real'
import { saveQuery, shareQuery, isQuerySaved, isQuerySavedByContent } from '../services/queryShareService'

// 查询建议常量
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

interface Props {
  // 外部传入的初始提示（用于从历史页面重新执行查询）
  initialPrompt?: string
}

interface Emits {
  (e: 'update:title', title: string): void
  (e: 'save-query', query: QueryResultData): void
  (e: 'share-query', queryId: string, friendId: string): void
  (e: 'toggle-history'): void
  (e: 'new-conversation'): void
  (e: 'rerun-query', prompt: string): void
  (e: 'view-in-chat', conversationId: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// ===== 对话状态管理（从 App.vue 移入） =====
const initialConversation: Conversation = {
  id: 'conv-initial',
  title: '',
  messages: [
    {
      role: 'ai',
      content:
        '您好！我是数据查询助手，您可以通过自然语言描述您的查询需求（例如："展示2023年各季度的订单量"），我会为您生成相应的结果。',
    },
  ],
  createTime: new Date().toISOString(),
}
const conversations = ref<Conversation[]>([initialConversation])
const currentConversationId = ref<string>(initialConversation.id)
const isHistoryOpen = ref(false)
const savedQueries = ref<QueryResultData[]>([])
const isLoadingHistory = ref(false)
const loadedConversationIds = ref<Set<string>>(new Set([initialConversation.id]))


// 计算属性
const currentConversation = computed(() => {
  return conversations.value.find((c) => c.id === currentConversationId.value)
})

// 判断是否是新建聊天（只有一条AI欢迎消息）
const isNewConversation = computed(() => {
  const conv = currentConversation.value
  if (!conv) return false
  // 只有一条消息且是AI的欢迎消息
  return conv.messages.length === 1 && conv.messages[0].role === 'ai' && typeof conv.messages[0].content === 'string'
})

// 判断最后一条消息是否是查询结果
const lastMessageIsQueryResult = computed(() => {
  const conv = currentConversation.value
  if (!conv || conv.messages.length === 0) return false
  const lastMsg = conv.messages[conv.messages.length - 1]
  return lastMsg.role === 'ai' && typeof lastMsg.content !== 'string'
})

// 监听对话标题变化，通知父组件更新 TopHeader
watch(
  () => currentConversation.value?.title,
  (newTitle) => {
    if (newTitle) {
      emit('update:title', newTitle)
    }
  },
)

// ===== 查询相关状态 =====
const prompt = ref('')
const modelOptions = ref<
  Array<{
    id: string
    name: string
    disabled: boolean
    description: string
  }>
>([])
const selectedModelId = ref('')
const databaseOptions = ref<
  Array<{
    id: string
    name: string
    disabled: boolean
    description: string
  }>
>([])
const selectedDatabaseId = ref('')
const selectedDatabase = ref('')
const isLoading = ref(false)
const error = ref<string | null>(null)
const abortController = ref<AbortController | null>(null)
const pendingConversationId = ref<string | null>(null)
const chatContainerRef = ref<HTMLElement | null>(null)
const isMobile = ref(false)
const showRecommendations = ref(false)
const allRecommendations = ref<string[]>([])
const isRecommendationLoading = ref(false) // Added to prevent duplicate API calls
// 【新增】后续问题存储
const currentFollowupQuestions = ref<string[]>([])



const checkMobile = () => {
  isMobile.value = window.innerWidth < 768 // md breakpoint
}

// 过滤推荐搜索（基于输入内容）
const filteredRecommendations = computed(() => {
  if (!prompt.value.trim()) {
    return allRecommendations.value.slice(0, 5) // 没有输入时显示前5个
  }
  const lowerPrompt = prompt.value.toLowerCase()
  return allRecommendations.value
    .filter((rec) => rec.toLowerCase().includes(lowerPrompt))
    .slice(0, 5)
})

// 处理输入变化
const handleInputChange = () => {
  if (prompt.value.trim()) {
    showRecommendations.value = true
  }
}


// ===== 生命周期钩子 =====
/**
 * 组件挂载时的初始化逻辑
 * 
 * 统一加载流程：
 * 1. 初始化移动端检测
 * 2. 加载大模型配置（异步）
 * 3. 加载数据库连接（异步）
 * 4. 初始化推荐列表（同步）
 * 5. 初始化标题（同步）
 * 6. 绑定事件监听器
 */
onMounted(async () => {
  // 1. 初始化移动端检测
  checkMobile()
  window.addEventListener('resize', checkMobile)
  
  // 2. 加载配置数据（等待基础配置加载完成）
  await Promise.all([
    loadAvailableModels(),
    loadDatabaseConnections()
  ])
  
  // 3. 初始化动态推荐列表
  await loadDynamicRecommendations()
  
  // 4. 从后端恢复对话历史（异步）
  await loadConversationHistory()
  
  // 5. 如果有对话历史，加载最后一次对话
  if (conversations.value.length > 0) {
    // 找到最后更新的对话（不包括初始对话）
    const lastConversation = conversations.value.find((c) => c.id !== 'conv-initial')
    if (lastConversation) {
      currentConversationId.value = lastConversation.id
      await loadConversationDetail(lastConversation.id)
      emit('update:title', lastConversation.title || '新对话')
    } else {
      emit('update:title', currentConversation.value?.title || '新对话')
    }
  } else {
    emit('update:title', currentConversation.value?.title || '新对话')
  }
  
  // 6. 绑定事件监听器
  document.addEventListener('click', handleClickOutside)
})

/**
 * 组件卸载时的清理逻辑
 * 
 * 统一清理流程：
 * 1. 移除窗口大小监听器
 * 2. 移除文档点击监听器
 */
onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  document.removeEventListener('click', handleClickOutside)
})

// 点击外部关闭推荐列表
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const inputArea = target.closest('form')
  const recommendationPopup = target.closest('.absolute')
  
  // 如果点击的不是输入区域和推荐弹窗，则关闭推荐列表
  if (!inputArea && !recommendationPopup) {
    showRecommendations.value = false
  }
}

// Watchers
watch(
  () => currentConversation.value?.messages,
  () => {
    nextTick(() => {
      if (chatContainerRef.value) {
        chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
      }
    })
  },
  { deep: true },
)

watch(
  () => props.initialPrompt,
  (newPrompt) => {
    if (newPrompt) {
      prompt.value = newPrompt
      handleSubmit(undefined, newPrompt)
    }
  },
)

watch(
  () => currentConversationId.value,
  () => {
    if (
      isLoading.value &&
      pendingConversationId.value &&
      pendingConversationId.value !== currentConversationId.value
    ) {
      handleStop()
    }
    // 切换对话时更新标题
    emit('update:title', currentConversation.value?.title || '新对话')
  },
)

// ===== 数据加载方法 =====
/**
 * 加载大模型配置
 * 
 * 统一加载逻辑：
 * 1. 调用 API 获取配置列表
 * 2. 转换为下拉选项格式
 * 3. 设置默认选中项（第一个）
 * 4. 错误处理：记录错误并设置空数组
 */
const loadAvailableModels = async () => {
  try {
    const configs = await llmConfigApi.getAvailable()
    const options = configs.map((config) => ({
      id: String(config.id),
      name: `${config.name} (${config.version})`,
      disabled: false,
      description: `${config.name} - ${config.version}`,
    }))
    modelOptions.value = options
    if (options.length > 0 && !selectedModelId.value) {
      selectedModelId.value = options[0].id
    }
  } catch (err) {
    console.error('加载大模型配置失败:', err)
    modelOptions.value = []
    error.value = '无法加载大模型配置，请联系管理员'
  }
}

// 拖动处理方法（用于模型选择列表）
const draggedIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

const handleModelDragStart = (index: number, event: DragEvent) => {
  draggedIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/html', '')
  }
}

const handleModelDragOver = (index: number, event: DragEvent) => {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dragOverIndex.value = index
}

const handleModelDrop = (index: number, event: DragEvent) => {
  event.preventDefault()
  if (draggedIndex.value === null || draggedIndex.value === index) {
    return
  }
  
  // 重新排序模型列表
  const newOptions = [...modelOptions.value]
  const draggedItem = newOptions[draggedIndex.value]
  newOptions.splice(draggedIndex.value, 1)
  newOptions.splice(index, 0, draggedItem)
  modelOptions.value = newOptions
  
  dragOverIndex.value = null
}

const handleModelDragEnd = () => {
  draggedIndex.value = null
  dragOverIndex.value = null
}

/**
 * 加载数据库连接配置
 * 
 * 统一加载逻辑：
 * 1. 调用 API 获取连接列表
 * 2. 过滤掉已禁用的连接
 * 3. 转换为下拉选项格式
 * 4. 设置默认选中项（第一个）
 * 5. 错误处理：记录错误并设置空数组
 */
const loadDatabaseConnections = async () => {
  try {
    const connections = await dbConnectionApi.getList()
    const activeConnections = connections.filter((conn) => conn.status !== 'disabled')
    const options = activeConnections.map((conn) => ({
      id: String(conn.id),
      name: conn.name,
      disabled: false,
      description: `${conn.name} - ${conn.url}`,
    }))
    databaseOptions.value = options
    if (options.length > 0) {
      selectedDatabaseId.value = options[0].id
      selectedDatabase.value = options[0].name
    }
  } catch (err) {
    console.error('加载数据库连接失败:', err)
    databaseOptions.value = []
  }
}

/**
 * 加载动态推荐词
 */
const loadDynamicRecommendations = async () => {
  // 1. 如果正在加载中，直接返回，避免并发重复请求导致 API 限制
  if (isRecommendationLoading.value) return
  
  // 2. 必须有选中的连接和模型
  if (!selectedDatabaseId.value || !selectedModelId.value) {
    allRecommendations.value = ['暂无推荐查询']
    return
  }

  try {
    isRecommendationLoading.value = true
    console.log('[QueryPage] 正在加载动态推荐词...')
    
    const recommendations = await recommendationApi.getRecommendations({
      conversationId: currentConversationId.value === 'conv-initial' ? undefined : currentConversationId.value,
      conversationHistory: currentConversation.value?.messages.map(m => ({
        role: m.role as 'user' | 'ai',
        content: m.content
      })) || [],
      dbConnectionId: Number(selectedDatabaseId.value),
      llmConfigId: selectedModelId.value,
    })
    
    // 只有当获取到新推荐且与当前不同时才更新，减少页面闪烁
    if (recommendations && recommendations.length > 0) {
      // 深度比较，如果一致则不更新
      if (JSON.stringify(allRecommendations.value) !== JSON.stringify(recommendations)) {
        allRecommendations.value = recommendations
      }
      console.log('[QueryPage] 动态推荐词加载成功:', recommendations)
    } else if (allRecommendations.value.length === 0 || allRecommendations.value[0] !== '暂无推荐查询') {
      allRecommendations.value = ['暂无推荐查询']
    }
  } catch (error) {
    console.warn('[QueryPage] 加载动态推荐词失败:', error)
    if (allRecommendations.value.length === 0) {
      allRecommendations.value = ['暂无推荐查询']
    }
  } finally {
    // 延迟重置加载标记，确保状态稳定
    setTimeout(() => {
      isRecommendationLoading.value = false
    }, 500)
  }
}

// 监听连接或模型变化，刷新推荐词
let recommendationTimeout: any = null
watch([selectedDatabaseId, selectedModelId], () => {
  if (recommendationTimeout) clearTimeout(recommendationTimeout)
  
  // 使用防抖，避免频繁切换导致的 API 限制和闪烁
  recommendationTimeout = setTimeout(() => {
    if (isNewConversation.value) {
      loadDynamicRecommendations()
    }
  }, 300)
})

const handleSubmit = async (event?: Event, customPrompt?: string, skipAddMessage = false) => {
  if (event) event.preventDefault()

  const finalPrompt = customPrompt || prompt.value
  if (!finalPrompt.trim() || isLoading.value) return

  // 清空后续问题，因为开始新的查询
  currentFollowupQuestions.value = []

  const controller = new AbortController()
  abortController.value = controller

  // 只有非注入模式才需要手动添加消息
  if (!skipAddMessage) {
    handleAddMessage('user', finalPrompt)
  }
  
  prompt.value = ''
  isLoading.value = true
  error.value = null

  // 【新增】重置后续问题
  currentFollowupQuestions.value = []

  // 在handleAddMessage之后捕获对话ID，因为初始对话会在第一条消息时获得真实ID
  const requestConversationId = currentConversationId.value
  pendingConversationId.value = requestConversationId

  try {
    if (!currentConversation.value) throw new Error('No active conversation.')
    
    // 确保使用正确的对话ID（如果是初始对话，会在handleAddMessage中被更新）
    const frontendConversationId = currentConversationId.value
    
    console.log('发送查询请求:', {
      userPrompt: finalPrompt,
      model: selectedModelId.value,
      database: selectedDatabase.value,
      dbConnectionId: Number(selectedDatabaseId.value),
      conversationId: frontendConversationId !== 'conv-initial' ? frontendConversationId : undefined,
    })
    
    const response: QueryResponse = await queryApi.execute({
      userPrompt: finalPrompt,
      model: selectedModelId.value,
      database: selectedDatabase.value,
      dbConnectionId: Number(selectedDatabaseId.value),
      conversationId:
        frontendConversationId !== 'conv-initial' ? frontendConversationId : undefined,
    }, controller.signal)
    
    console.log('收到查询响应:', response)
    
    // 【关键修复】同步后端生成的真实对话 ID
    if (response.conversationId && currentConversationId.value !== response.conversationId) {
      const oldId = currentConversationId.value
      const newId = response.conversationId
      console.log(`[QueryPage] 正在将临时 ID ${oldId} 同步为真实 ID ${newId}`)
      
      conversations.value = conversations.value.map(c => {
        if (c.id === oldId) {
          return { ...c, id: newId }
        }
        return c
      })
      
      // 必须立刻同步 currentConversationId，否则界面会因为找不到旧 ID 的消息而变为空白
      currentConversationId.value = newId
    }

    // =====【新增】提取后续问题==========
    if (response.followupQuestions && response.followupQuestions.length > 0) {
      currentFollowupQuestions.value = response.followupQuestions
      console.log('✓ 获取到后续问题:', currentFollowupQuestions.value.length, '条')
    } else {
      console.log('⚠ 未获取到后续问题')
    }
    //====================================

    // 检查是否是权限错误（后端返回的tableData中包含权限错误信息）
    const isPermissionError = 
      response.tableData && 
      response.tableData.headers && 
      response.tableData.headers.length > 0 && 
      (response.tableData.headers[0] === '权限错误' || 
       response.tableData.headers[0].includes('权限'))
    
    if (isPermissionError && response.tableData.rows && response.tableData.rows.length > 0) {
      // 权限错误，显示错误消息
      const errorMessage = response.tableData.rows[0][0] || '权限不足，无法执行查询'
      console.error('权限检查失败:', errorMessage)
      
      if (currentConversationId.value === requestConversationId) {
        error.value = errorMessage
        handleAddMessage('ai', errorMessage)
      }
      return // 不继续处理
    }

    const result: QueryResultData = {
      id: response.id,
      userPrompt: response.userPrompt,
      sqlQuery: response.sqlQuery,
      conversationId: currentConversationId.value, // 【修复】使用最新的对话ID（已同步）
      queryTime: response.queryTime,
      executionTime: response.executionTime,
      database: response.database,
      model: response.model,
      tableData: response.tableData,
      chartData: response.chartData
        ? {
            type: (response.chartData.type || 'bar') as 'bar' | 'line' | 'pie',
            labels: response.chartData.labels || [],
            datasets: (response.chartData.datasets || []).map((dataset) => ({
              label: dataset.label,
              data: dataset.data,
              backgroundColor: dataset.backgroundColor || '#3b82f6',
            })),
          }
        : undefined,
      // 添加 dbConnectionId 和 llmConfigId，用于保存查询日志
      dbConnectionId: response.dbConnectionId,
      llmConfigId: response.llmConfigId,
    } as QueryResultData

    if (currentConversationId.value === requestConversationId) {
      handleAddMessage('ai', result)
      // 注意：后端execute接口返回的id是临时ID（"query_xxxx"），不是真实的queryLogId
      // 需要主动调用queryLogApi.create()保存查询，获取真实的queryLogId
      try {
        const userId = Number(sessionStorage.getItem('userId') || '1')
        // 保存查询到查询日志表（对话历史），获取真实的queryLogId
        const savedLog = await queryLogApi.create({
          userId,
          userPrompt: result.userPrompt,
          sqlQuery: result.sqlQuery,
          queryResult: JSON.stringify({
            tableData: result.tableData,
            chartData: result.chartData,
          }),
          dialogId: result.conversationId || '',
          dbConnectionId: result.dbConnectionId || Number(selectedDatabaseId.value) || 0,
          llmConfigId: result.llmConfigId || Number(selectedModelId.value) || 0,
          queryTime: result.queryTime || new Date().toISOString(),
          executionTime: result.executionTime || '0ms',
        })
        
        // 使用真实的queryLogId更新result
        const realQueryLogId = String(savedLog.id)
        result.id = realQueryLogId
        
        // 更新消息中的ID（如果可能）
        const currentConv = currentConversation.value
        if (currentConv) {
          const lastMessage = currentConv.messages[currentConv.messages.length - 1]
          if (lastMessage && lastMessage.role === 'ai' && typeof lastMessage.content !== 'string') {
            lastMessage.content.id = realQueryLogId
          }
        }
        
        console.log('✅ 查询已保存到对话历史，queryLogId:', realQueryLogId)
      } catch (saveError) {
        console.error('保存查询到对话历史失败:', saveError)
        // 保存失败不影响查询结果显示
      }
    } else {
      console.log(
        `AI回复已丢弃（目标对话已切换）：原对话ID=${requestConversationId}，新对话ID=${currentConversationId.value}`,
      )
    }
  } catch (err) {
    console.error('查询执行异常:', err)
    if (err instanceof Error && err.name === 'AbortError') {
      console.log('查询被手动停止')
      if (currentConversationId.value === requestConversationId) {
        handleAddMessage('ai', '查询已被手动停止')
      }
    } else {
      const errorMessage = err instanceof Error ? err.message : '查询失败，请稍后重试'
      console.error('查询失败:', errorMessage, err)
      if (currentConversationId.value === requestConversationId) {
        error.value = errorMessage
        handleAddMessage('ai', errorMessage)
      }
    }
  } finally {
    console.log('查询处理完成，设置 isLoading = false')
    if (currentConversationId.value === requestConversationId) {
      isLoading.value = false
      abortController.value = null
      pendingConversationId.value = null
    }
  }
}

const handleStop = () => {
  if (abortController.value) {
    abortController.value.abort()
    isLoading.value = false
    abortController.value = null
    pendingConversationId.value = null
  }
}

const handleRecommendationClick = (recommendation: string) => {
  prompt.value = recommendation
  showRecommendations.value = false
  // 【新增】清空后续问题，因为开始新的查询
  currentFollowupQuestions.value = []
  handleSubmit(undefined, recommendation)
}


// 计算相关搜索（用于大模型思考弹出层）
const queryFailed = computed(() => {
  const lastMessage = currentConversation.value?.messages[currentConversation.value.messages.length - 1]
  if (!lastMessage || lastMessage.role !== 'ai') return false
  return typeof lastMessage.content === 'string'
})

const relatedSearches = computed(() => {
  if (!queryFailed.value && currentConversation.value && currentConversation.value.messages.length > 1) {
    const lastMessage = currentConversation.value.messages[currentConversation.value.messages.length - 1]
    if (lastMessage.role === 'ai') {
      return MOCK_SUCCESS_SUGGESTIONS
    }
  }
  return queryFailed.value ? MOCK_FAILURE_SUGGESTIONS : []
})

const handleEnterKey = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSubmit()
  }
}

// ===== 对话管理方法 =====
const handleAddMessage = async (role: MessageRole, content: string | QueryResultData) => {
  if (!currentConversationId.value) return

  conversations.value = conversations.value.map((conv) => {
    if (conv.id === currentConversationId.value) {
      const newMessages = [...conv.messages, { role, content }]
      let newTitle = conv.title
      let newId = conv.id

      // 首次用户消息，截取前20字作为对话标题
      if (conv.messages.length === 1 && role === 'user' && typeof content === 'string') {
        newTitle = content.substring(0, 20)
        emit('update:title', newTitle)
        
        // 如果是初始对话（conv-initial），给它分配一个真实的ID
        if (conv.id === 'conv-initial') {
          newId = 'conv-' + Date.now()
          currentConversationId.value = newId
          console.log('初始对话获得真实ID:', newId)
        }
      }

      const updatedConv = { ...conv, id: newId, title: newTitle, messages: newMessages }
      
      // 异步保存到后端（不阻塞UI）
      saveConversationToBackend(updatedConv).catch((error) => {
        console.error('保存对话失败:', error)
      })
      
      return updatedConv
    }
    return conv
  })
}

  /**
   * 开启新对话
   * @param skipRecommendations 是否跳过推荐词加载
   * @param initialUserMessage 初始用户消息（用于导入执行，确保消息不丢失）
   */
  const handleNewConversation = async (skipRecommendations = false, initialUserMessage?: string) => {
    // 在切换之前，保存当前对话（如果有内容）
    const currentConv = currentConversation.value
    if (currentConv && currentConv.messages.length > 1) {
      // 当前对话有内容，保存到后端
      await saveConversationToBackend(currentConv).catch((error) => {
        console.error('保存当前对话失败:', error)
      })
    }
  
    // 【新增】新建对话时清空后续问题
    currentFollowupQuestions.value = []
    
    const isEmptyConv = (conv: Conversation) => {
      return (
        conv.messages.length === 1 &&
        conv.messages[0].role === 'ai' &&
        typeof conv.messages[0].content === 'string' &&
        conv.messages[0].content.includes('您好！我是数据查询助手')
      )
    }

    const existingEmptyConv = conversations.value.find(isEmptyConv)
    let targetConvId = ''

    if (existingEmptyConv) {
      targetConvId = existingEmptyConv.id
      currentConversationId.value = targetConvId
      
      // 如果有初始消息，注入到这个空对话中
      if (initialUserMessage) {
        conversations.value = conversations.value.map(c => {
          if (c.id === targetConvId) {
            return {
              ...c,
              messages: [...c.messages, { role: 'user', content: initialUserMessage }]
            }
          }
          return c
        })
      }
    } else {
      targetConvId = 'conv-' + Date.now()
      const messages: any[] = [
        {
          role: 'ai',
          content:
            '您好！我是数据查询助手，您可以通过自然语言描述您的查询需求（例如："展示2023年各季度的订单量"），我会为您生成相应的结果。',
        }
      ]
      
      // 如果有初始消息，直接放入新对话
      if (initialUserMessage) {
        messages.push({ role: 'user', content: initialUserMessage })
      }

      const newConv: Conversation = {
        id: targetConvId,
        title: initialUserMessage ? initialUserMessage.substring(0, 20) : '新对话',
        messages,
        createTime: new Date().toISOString(),
      }
      conversations.value = [newConv, ...conversations.value]
      currentConversationId.value = targetConvId
    }

    // 关闭历史侧边栏（如果打开的话）
    if (isHistoryOpen.value) {
      isHistoryOpen.value = false
    }
    emit('update:title', initialUserMessage ? initialUserMessage.substring(0, 20) : '新对话')
    
    // 强制等待 Vue 状态同步
    await nextTick()
    
    // 最后加载动态推荐词
    if (!skipRecommendations) {
      loadDynamicRecommendations()
    }
    
    return targetConvId
  }

const handleSwitchConversation = async (id: string) => {
  console.log('切换对话:', id)
  
  // 在切换之前，保存当前对话（如果有内容）
  const currentConv = currentConversation.value
  if (currentConv && currentConv.messages.length > 1) {
    // 当前对话有内容，保存到后端
    await saveConversationToBackend(currentConv).catch((error) => {
      console.error('保存当前对话失败:', error)
    })
  }
  // 【新增】切换对话时清空后续问题
  currentFollowupQuestions.value = []
  
  // 切换到新对话
  currentConversationId.value = id
  console.log('当前对话ID已更新为:', id)
  
  // 加载对话详情（如果还没有加载）
  await loadConversationDetail(id)
  
  // 关闭历史侧边栏
  if (isHistoryOpen.value) {
    isHistoryOpen.value = false
  }
  
  // 更新标题
  const newConv = conversations.value.find((c) => c.id === id)
  if (newConv) {
    console.log('更新标题为:', newConv.title, '消息数量:', newConv.messages.length)
    emit('update:title', newConv.title || '新对话')
  }
}

const handleDeleteConversation = async (deleteId: string) => {
  try {
    // 调用后端API删除对话
    await dialogApi.delete(deleteId)
    
    // 同时删除对话详情
    try {
      const detail = await dialogDetailApi.getByDialogId(deleteId).catch(() => null)
      if (detail) {
        await dialogDetailApi.delete(detail.id)
      }
    } catch (error) {
      console.error('删除对话详情失败:', error)
      // 不影响主流程
    }
  } catch (error) {
    console.error('删除对话失败:', error)
    // 即使后端删除失败，也继续删除本地数据
  }
  
  // 从本地列表中删除
  const updatedConversations = conversations.value.filter((conv) => conv.id !== deleteId)
  loadedConversationIds.value.delete(deleteId)

  if (currentConversationId.value === deleteId) {
    if (updatedConversations.length > 0) {
      currentConversationId.value = updatedConversations[0].id
    } else {
      const newConv: Conversation = {
        id: 'conv-' + Date.now(),
        title: '新对话',
        messages: [
          {
            role: 'ai',
            content: '您好！我是数据查询助手，您可以通过自然语言描述您的查询需求...',
          },
        ],
        createTime: new Date().toISOString(),
      }
      updatedConversations.unshift(newConv)
      currentConversationId.value = newConv.id
    }
  }

  conversations.value = updatedConversations
  emit('update:title', currentConversation.value?.title || '新对话')
}

const toggleHistory = async () => {
  isHistoryOpen.value = !isHistoryOpen.value
  // 打开历史侧边栏时，从后端加载对话列表
  if (isHistoryOpen.value) {
    await loadConversationHistory()
  }
}

// ===== 对话历史管理（从后端加载） =====
/**
 * 从后端加载对话历史列表
 * 只显示后端保存的有效对话，过滤掉本地临时对话
 */
const loadConversationHistory = async () => {
  try {
    isLoadingHistory.value = true
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const dialogs = await dialogApi.getList()
    
    console.log('从后端加载到的对话列表:', dialogs)
    
    // 过滤掉没有轮次的空对话（totalRounds === 0 或 null）
    const validDialogs = dialogs.filter((dialog) => dialog.totalRounds && dialog.totalRounds > 0)
    
    console.log('过滤后的有效对话:', validDialogs)
    
    // 将后端对话记录转换为前端 Conversation 格式
    const loadedConversations: Conversation[] = validDialogs.map((dialog) => {
      // 检查是否已经加载过这个对话的详情
      const existingConv = conversations.value.find((c) => c.id === dialog.dialogId)
      if (existingConv && loadedConversationIds.value.has(dialog.dialogId)) {
        // 如果已加载，使用现有的对话（保留消息）
        return existingConv
      }
      
      // 创建新的对话对象（只有基本信息，消息稍后加载）
      const newConv: Conversation = {
        id: dialog.dialogId,
        title: dialog.topic || '无标题对话',
        messages: [
          {
            role: 'ai' as MessageRole,
            content:
              '您好！我是数据查询助手，您可以通过自然语言描述您的查询需求（例如："展示2023年各季度的订单量"），我会为您生成相应的结果。',
          },
        ],
        createTime: dialog.startTime,
        totalRounds: dialog.totalRounds, // 添加后端的轮次数，用于区分是否为有效对话
      }
      console.log('创建对话对象:', newConv.id, 'totalRounds:', newConv.totalRounds)
      return newConv
    })
    
    console.log('loadedConversations:', loadedConversations.map(c => ({ id: c.id, title: c.title, totalRounds: c.totalRounds })))
    
    // 保留当前正在使用的对话（如果不在后端列表中）
    const currentConv = conversations.value.find((c) => c.id === currentConversationId.value)
    const backendDialogIds = new Set(validDialogs.map((d) => d.dialogId))
    
    // 只保留当前对话和从后端加载的对话
    if (currentConv && !backendDialogIds.has(currentConv.id)) {
      // 当前对话不在后端列表中，保留它
      conversations.value = [currentConv, ...loadedConversations]
    } else {
      // 完全替换为后端加载的对话
      conversations.value = loadedConversations
    }
    
    // 注意：不在这里标记为已加载，因为消息详情还没有加载
    // 只有在 loadConversationDetail 真正加载了消息后才标记为已加载
  } catch (error) {
    console.error('加载对话历史失败:', error)
  } finally {
    isLoadingHistory.value = false
  }
}

/**
 * 从后端加载对话详情（包括消息）
 */
const loadConversationDetail = async (dialogId: string): Promise<void> => {
  try {
    console.log('开始加载对话详情:', dialogId)
    
    // 如果已经加载过详细消息，跳过
    if (loadedConversationIds.value.has(dialogId)) {
      const existingConv = conversations.value.find((c) => c.id === dialogId)
      // 检查是否真的有消息内容（不只是初始欢迎消息）
      if (existingConv && existingConv.messages.length > 1) {
        console.log('对话详情已加载，跳过:', dialogId, '消息数量:', existingConv.messages.length)
        return
      }
    }
    
    // 加载对话详情
    console.log('从后端获取对话详情:', dialogId)
    const detail: DialogDetail | null = await dialogDetailApi.getByDialogId(dialogId).catch(() => null)
    
    // 如果对话详情不存在，只保留基本信息
    if (!detail || !detail.rounds || detail.rounds.length === 0) {
      console.log('对话详情为空，不标记为已加载:', dialogId)
      // 不标记为已加载，因为没有实际内容
      return
    }
    
    console.log('对话详情加载成功，轮次数量:', detail.rounds.length)
    
    // 将 rounds 转换为 messages（一次多轮对话的所有轮次）
    const messages: Array<{ role: MessageRole; content: string | QueryResultData }> = [
      {
        role: 'ai',
        content:
          '您好！我是数据查询助手，您可以通过自然语言描述您的查询需求（例如："展示2023年各季度的订单量"），我会为您生成相应的结果。',
      },
    ]
    
    // 按轮次排序
    const sortedRounds = [...detail.rounds].sort((a, b) => a.roundNum - b.roundNum)
    
    for (const round of sortedRounds) {
      // 添加用户消息
      if (round.userInput) {
        messages.push({
          role: 'user',
          content: round.userInput,
        })
      }
      
      // 添加AI回复
      // 注意：后端返回的 aiResponse 可能是字符串，但前端需要 QueryResultData
      // 这里先尝试解析为 JSON，如果失败则作为字符串处理
      if (round.aiResponse) {
        try {
          const aiResponse = JSON.parse(round.aiResponse)
          if (aiResponse && typeof aiResponse === 'object' && aiResponse.tableData) {
            // 是 QueryResultData 格式
            messages.push({
              role: 'ai',
              content: aiResponse as QueryResultData,
            })
          } else {
            // 不是 QueryResultData，作为字符串处理
            messages.push({
              role: 'ai',
              content: round.aiResponse,
            })
          }
        } catch {
          // 解析失败，作为字符串处理
          messages.push({
            role: 'ai',
            content: round.aiResponse,
          })
        }
      }
    }
    
    // 更新对话消息
    conversations.value = conversations.value.map((conv) => {
      if (conv.id === dialogId) {
        console.log('更新对话消息:', dialogId, '新消息数量:', messages.length)
        return {
          ...conv,
          messages,
        }
      }
      return conv
    })
    
    // 标记为已加载
    loadedConversationIds.value.add(dialogId)
    console.log('对话详情加载完成并标记为已加载:', dialogId)
  } catch (error) {
    console.error('加载对话详情失败:', error)
    // 如果加载失败，至少保留基本信息
  }
}

/**
 * 保存对话到后端
 * 一次多轮对话保存为一条历史记录，包含所有轮次
 */
const saveConversationToBackend = async (conversation: Conversation): Promise<void> => {
  try {
    console.log('开始保存对话到后端:', conversation.id, '标题:', conversation.title)
    const userId = Number(sessionStorage.getItem('userId') || '1')
    
    // 检查对话是否已存在
    const existingDialog = await dialogApi.getById(conversation.id).catch(() => null)
    console.log('对话是否已存在:', existingDialog ? '是' : '否')
    
    // ---------- 生成轮次数组 ----------
    const rounds: Array<{
      roundNum: number
      userInput: string
      aiResponse: string
      generatedSql: string
      roundTime: string
    }> = []

    let roundNum = 1
    // 跳过第一条AI欢迎消息，然后按用户-AI配对提取轮次
    for (let i = 1; i < conversation.messages.length; i += 2) {
      const userMessage = conversation.messages[i]
      const aiMessage = conversation.messages[i + 1]

      if (userMessage && userMessage.role === 'user' && aiMessage && aiMessage.role === 'ai') {
        const userInput = typeof userMessage.content === 'string' ? userMessage.content : ''
        const aiResponse =
          typeof aiMessage.content === 'string'
            ? aiMessage.content
            : JSON.stringify(aiMessage.content)
        const generatedSql =
          typeof aiMessage.content !== 'string' ? aiMessage.content.sqlQuery || '' : ''
        const roundTime = new Date().toISOString()

        rounds.push({ roundNum, userInput, aiResponse, generatedSql, roundTime })
        roundNum++
      }
    }

    // 轮次数以提取出的 rounds 长度为准
    const userMessageCount = rounds.length
    console.log('提取的轮次数量:', userMessageCount, 'rounds:', rounds)

    if (userMessageCount === 0) {
      console.log('对话没有有效轮次，跳过保存')
      return
    }

    if (existingDialog) {
      // 更新对话记录
      console.log('更新现有对话记录')
      await dialogApi.update(conversation.id, {
        topic: conversation.title || '无标题对话',
        totalRounds: userMessageCount,
        lastTime: new Date().toISOString(),
      })
    } else {
      // 创建新对话记录
      console.log('创建新对话记录')
      await dialogApi.create({
        dialogId: conversation.id,
        userId,
        topic: conversation.title || '无标题对话',
        totalRounds: userMessageCount,
        startTime: conversation.createTime,
        lastTime: new Date().toISOString(),
      })
    }
    console.log('DialogRecord 保存成功')
    
    // 保存或更新对话详情（一次多轮对话的所有轮次）
    try {
      const existingDetail = await dialogDetailApi.getByDialogId(conversation.id).catch(() => null)
      if (existingDetail) {
        // 更新时，完全替换rounds数组（因为前端已经包含了所有轮次）
        console.log('更新现有对话详情，轮次数:', rounds.length)
        await dialogDetailApi.update({
          id: existingDetail.id,
          dialogId: conversation.id,
          rounds,
        })
      } else {
        // 创建新的对话详情，包含所有轮次
        console.log('创建新对话详情，轮次数:', rounds.length)
        await dialogDetailApi.save({
          dialogId: conversation.id,
          rounds,
        })
      }
      console.log('DialogDetail 保存成功')
    } catch (error) {
      console.error('保存对话详情失败:', error)
      // 不影响主流程
    }
    
    console.log('✅ 对话保存完成:', conversation.id)
  } catch (error) {
    console.error('❌ 保存对话到后端失败:', error)
    // 不影响主流程
  }
}


// ===== 查询收藏管理 =====
/**
 * 处理收藏查询事件（由 QueryResult 组件触发）
 * 注意：此函数只是标记查询为已收藏，实际的收藏夹保存逻辑在 QueryResult.vue 中
 * QueryResult 会先保存到收藏夹，然后触发此事件，我们只需要更新 savedQueries 数组
 */
const handleSaveQuery = async (query: QueryResultData) => {
  // 检查是否已收藏（通过内容匹配）
  if (isQuerySavedByContent(query, savedQueries.value)) {
    console.log('查询已在收藏列表中，跳过重复添加')
    return
  }
  
  // 添加到已收藏列表（用于判断查询是否已收藏）
  savedQueries.value = [query, ...savedQueries.value]
  console.log('✅ 查询已标记为收藏，当前收藏数量:', savedQueries.value.length)
}

// ⚠️ 注意：后端API不完整，前端跳过检查
// 后端 QueryShare 实体类缺少 queryLogId 字段，可能导致分享失败
const handleShareQuery = async (queryId: string, friendId: string) => {
  try {
    // 先从savedQueries中查找
    let queryToShare = savedQueries.value.find((q) => q.id === queryId)
    
    // 如果找不到，尝试从当前对话的消息中查找
    if (!queryToShare) {
      const currentConv = currentConversation.value
      if (currentConv) {
        const aiMessage = currentConv.messages.find(
          (msg) => msg.role === 'ai' && typeof msg.content !== 'string' && msg.content.id === queryId
        )
        if (aiMessage && typeof aiMessage.content !== 'string') {
          queryToShare = aiMessage.content
        }
      }
    }
    
    // 使用统一服务分享查询（会自动处理临时ID的情况）
    // ⚠️ 后端可能无法正确处理 queryLogId，但前端继续调用
    const realQueryLogId = await shareQuery(queryId, friendId, queryToShare)
    
    // 如果queryId是临时ID，shareQuery内部已经保存了，需要更新本地ID
    const isRealQueryLogId = !isNaN(Number(queryId)) && Number(queryId) > 0
    if (queryToShare && !isRealQueryLogId && realQueryLogId !== queryId) {
      // shareQuery已经保存了查询，使用返回的真实ID更新本地数据
      const savedQuery: QueryResultData = {
        ...queryToShare,
        id: realQueryLogId,
      }
      // 更新savedQueries和对话消息中的ID
      const index = savedQueries.value.findIndex((q) => q.id === queryId)
      if (index !== -1) {
        savedQueries.value[index] = savedQuery
      } else {
        // 如果不在savedQueries中，添加进去
        savedQueries.value = [savedQuery, ...savedQueries.value]
      }
      const currentConv = currentConversation.value
      if (currentConv) {
        const aiMessage = currentConv.messages.find(
          (msg) => msg.role === 'ai' && typeof msg.content !== 'string' && msg.content.id === queryId
        )
        if (aiMessage && typeof aiMessage.content !== 'string') {
          aiMessage.content.id = realQueryLogId
        }
      }
    }
    
    alert('分享成功！')
    emit('share-query', realQueryLogId, friendId)
  } catch (error) {
    console.error('分享失败:', error)
    alert('分享失败，请重试: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 暴露给父组件的方法（用于从历史页面重新执行查询）
defineExpose({
  conversations,
  savedQueries,
  handleRerunQuery: async (promptText: string, dbConnectionId?: number) => {
    console.log('[QueryPage] 开始重新执行分享查询:', promptText)
    
    // 1. 先重置所有状态（开启新对话），并直接注入用户消息
    // 这样可以确保消息 100% 挂载在对话中，不会因为异步竞态而消失
    await handleNewConversation(true, promptText)
    
    // 2. 如果传了数据库 ID，在重置后重新赋值
    if (dbConnectionId && Number(dbConnectionId) > 0) {
      selectedDatabaseId.value = String(dbConnectionId)
      const conn = databaseOptions.value.find(c => c.id === String(dbConnectionId))
      if (conn) {
        selectedDatabase.value = conn.name
      }
    }
    
    // 3. 执行查询（传入 skipAddMessage=true，因为上面已经在对话中注入过消息了）
    // 注意：我们需要修改 handleSubmit 支持跳过添加消息，或者直接在这里处理
    handleSubmit(undefined, promptText, true)
  },
  handleViewInChat: async (conversationId: string) => {
    const conv = conversations.value.find((c) => c.id === conversationId)
    if (conv) {
      currentConversationId.value = conversationId
      // 加载对话详情
      await loadConversationDetail(conversationId)
      isHistoryOpen.value = true
      emit('update:title', conv.title || '新对话')
    } else {
      // 如果本地没有，尝试从后端加载
      await loadConversationHistory()
      const loadedConv = conversations.value.find((c) => c.id === conversationId)
      if (loadedConv) {
        currentConversationId.value = conversationId
        await loadConversationDetail(conversationId)
        isHistoryOpen.value = true
        emit('update:title', loadedConv.title || '新对话')
      }
    }
  },
  toggleHistory: () => {
    toggleHistory()
  },
  handleNewConversation: (skipRecommendations = false) => {
    handleNewConversation(skipRecommendations)
  },
})
</script>

<style scoped>
/* 确保 select 文本能够正确截断（选择框本身） */
select {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  padding-right: 3rem; /* 增加右边留白，为下拉箭头留出空间 */
}

/* 确保 select 选项文本完整显示（下拉时，不截断） */
select option {
  white-space: normal !important;
  text-overflow: initial !important;
  overflow: visible !important;
  padding-right: 2rem; /* 增加右边留白 */
  max-width: none !important;
}

/* 美化滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.5);
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.7);
}
</style>
