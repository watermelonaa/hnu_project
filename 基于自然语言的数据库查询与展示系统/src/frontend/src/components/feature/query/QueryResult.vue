<!--
  @file components/feature/query/QueryResult.vue
  @description 查询结果展示组件

  功能：
  - 表格/图表双视图切换
  - SQL 代码展示
  - 保存/分享/导出操作
  - 响应式布局

  @author Frontend Team
-->
<template>
  <div class="w-full max-w-full">
    <!-- 标题和元信息 -->
    <div class="flex items-center justify-between mb-2">
      <h3 class="text-base font-bold text-dark">查询结果（{{ formatQueryTime }}）</h3>
      <p class="text-sm">
        <!-- 可选内容 -->
      </p>
    </div>

    <div class="text-xs text-gray-500 space-y-0.5 mb-2">
      <p>
        数据库：<span>{{ result.database || '销售数据库' }}</span>
      </p>
      <p>
        大模型：<span>{{ result.model || 'gemini-2.5-pro' }}</span>
      </p>
      <p>
        执行耗时：<span>{{ result.executionTime }}</span>
      </p>
    </div>

    <!-- 标签页导航 -->
    <div class="border-b mb-4">
      <div class="flex space-x-6">
        <button
          v-for="tab in tabs"
          :key="tab.view"
          @click="activeView = tab.view"
          :class="[
            'py-2 text-sm transition-colors duration-200',
            activeView === tab.view
              ? 'border-b-2 border-primary text-primary font-medium'
              : 'text-gray-500 hover:text-dark',
          ]"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="min-h-[200px]">
      <div v-if="activeView === 'table'">
        <!-- 空数据提示 -->
        <div
          v-if="isEmptyData"
          class="flex flex-col items-center justify-center py-16 text-gray-500"
        >
          <i class="fa fa-database text-6xl mb-4 opacity-20"></i>
          <p class="text-lg font-medium">暂无数据</p>
          <p class="text-sm mt-2">查询结果为空，请尝试其他查询条件</p>
        </div>
        <!-- 有数据时显示表格 -->
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b">
                <th
                  v-for="(header, i) in result.tableData.headers"
                  :key="i"
                  class="px-4 py-3 font-semibold text-dark text-center"
                >
                  {{ header }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, i) in result.tableData.rows"
                :key="i"
                class="border-b last:border-b-0 hover:bg-gray-50"
              >
                <td
                  v-for="(cell, j) in row"
                  :key="j"
                  :class="[
                    'px-4 py-3 text-center',
                    typeof cell === 'string' && cell.startsWith('+')
                      ? 'text-success font-medium'
                      : 'text-gray-700',
                  ]"
                >
                  {{ cell }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-else-if="activeView === 'chart'">
        <div
          v-if="!hasChartData"
          class="flex flex-col items-center justify-center py-12 text-gray-500"
        >
          <i class="fa fa-chart-bar text-6xl mb-4 opacity-20"></i>
          <p class="text-lg">暂无可视化数据</p>
          <p class="text-sm mt-2">当前查询结果不适合生成图表</p>
        </div>

        <div v-else class="bg-white p-2 rounded-lg">
          <div class="flex justify-center space-x-2 mb-4">
            <button
              v-for="type in chartTypes"
              :key="type.value"
              @click="chartType = type.value"
              :class="[
                'px-3 py-1.5 text-sm rounded-md flex items-center transition-colors',
                chartType === type.value
                  ? 'bg-primary text-white'
                  : 'bg-gray-200 hover:bg-gray-300',
              ]"
            >
              <i :class="`fa ${type.icon} mr-2`"></i>{{ type.label }}
            </button>
          </div>
          <div class="chart-container">
            <canvas ref="chartCanvas"></canvas>
          </div>
        </div>
      </div>

      <div v-else-if="activeView === 'prompt'">
        <div class="p-4 bg-gray-50 rounded-md">
          <p class="text-gray-800 whitespace-pre-wrap">{{ result.userPrompt }}</p>
        </div>
      </div>

      <div v-else-if="activeView === 'sql'">
        <div class="relative">
          <button
            @click="handleCopySql"
            class="absolute top-2 right-2 px-2 py-1 text-xs bg-gray-600 hover:bg-gray-700 text-white rounded transition-colors"
          >
            <i :class="`fa ${copySuccess ? 'fa-check' : 'fa-copy'}`"></i>
            {{ copySuccess ? '已复制' : '复制' }}
          </button>
          <pre class="p-4 bg-gray-800 text-white rounded-md text-sm overflow-x-auto">
                        <code>{{ result.sqlQuery }}</code>
                    </pre>
        </div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="flex gap-3 mt-4 ml-auto justify-end">
      <button
        v-if="showActions.save"
        @click="handleSave"
        :class="[
          'px-3 py-2 border rounded-lg text-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 min-w-[80px] max-w-[100px]',
          isSaved ? 'border-primary bg-primary/10 text-primary' : 'border-gray-300'
        ]"
      >
        <i :class="['fa flex-shrink-0', isSaved ? 'fa-star' : 'fa-star-o']"></i>
        <span class="truncate">{{ isSaved ? '已收藏' : '收藏' }}</span>
      </button>
      <button
        v-if="showActions.share"
        @click="handleOpenShareModal"
        class="px-3 py-2 border border-gray-300 rounded-lg text-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 min-w-[80px] max-w-[100px]"
      >
        <i class="fa fa-share-alt flex-shrink-0"></i>
        <span class="truncate">分享</span>
      </button>
      <button
        v-if="showActions.export"
        @click="handleExport"
        class="px-3 py-2 border border-gray-300 rounded-lg text-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-2 min-w-[80px] max-w-[100px]"
      >
        <i class="fa fa-download flex-shrink-0"></i>
        <span class="truncate">导出</span>
      </button>
    </div>

    <!-- 分享模态框 -->
    <Modal :is-open="activeModal === 'share'" @close="activeModal = null" title="分享查询结果">
      <div class="space-y-4">
        <p class="text-sm text-gray-600">选择一位好友来分享本次的查询结果：</p>
        <div class="max-h-60 overflow-y-auto space-y-2 p-2 bg-gray-50 rounded-lg border">
          <template v-if="isLoadingFriends">
            <div class="text-center text-gray-500 text-sm p-4">
              <div class="dot-flashing mx-auto mb-2"></div>
              <p>正在加载好友列表...</p>
            </div>
          </template>
          <template v-else-if="friendsList.length > 0">
            <label
              v-for="friend in friendsList"
              :key="friend.id"
              :class="[
                'flex items-center p-3 rounded-lg cursor-pointer transition-colors',
                String(selectedFriendId) === String(friend.id) ? 'bg-primary/20' : 'hover:bg-gray-200',
              ]"
            >
              <input
                type="radio"
                name="friend-share-selection"
                :value="String(friend.id)"
                v-model="selectedFriendId"
                class="mr-3"
              />
              <div class="flex items-center space-x-3">
                <img 
                  :src="friend.avatarUrl || '/default-avatar.png'" 
                  :alt="friend.name" 
                  class="w-8 h-8 rounded-full" 
                />
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium text-dark truncate" :title="friend.name">{{
                    friend.name
                  }}</span>
                  <span
                    v-if="friend.onlineStatus === 1"
                    class="text-xs text-green-500"
                  >
                    (在线)
                  </span>
                </div>
              </div>
            </label>
          </template>
          <p v-else class="text-center text-gray-500 text-sm p-4">没有可分享的好友</p>
        </div>
      </div>
      <div class="mt-6 flex justify-end space-x-3">
        <button
          @click="activeModal = null"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200"
        >
          取消
        </button>
        <button
          @click="handleConfirmShare"
          :disabled="!selectedFriendId || selectedFriendId === ''"
          :class="[
            'px-4 py-2 rounded-lg text-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200',
            !selectedFriendId || selectedFriendId === '' 
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed' 
              : 'bg-primary text-white hover:bg-primary/90',
          ]"
        >
          确认分享
        </button>
      </div>
    </Modal>

    <!-- 选择收藏夹模态框 -->
    <Modal :is-open="activeModal === 'selectCollection'" @close="activeModal = null" title="选择收藏夹">
      <div class="space-y-4">
        <p class="text-sm text-gray-600">请选择要保存到的收藏夹（不选择将保存到默认收藏夹）：</p>
        
        <!-- 收藏夹列表 -->
        <div class="max-h-60 overflow-y-auto space-y-2 p-2 bg-gray-50 rounded-lg border">
          <template v-if="collections.length > 0">
            <label
              v-for="collection in collections"
              :key="collection.id"
              :class="`flex items-center p-3 rounded-lg cursor-pointer transition-colors ${
                String(selectedCollectionId) === String(collection.id) ? 'bg-primary/20' : 'hover:bg-gray-200'
              }`"
            >
              <input
                type="radio"
                name="collection-selection"
                :checked="String(selectedCollectionId) === String(collection.id)"
                @change="selectedCollectionId = collection.id"
                class="mr-3"
              />
              <div class="flex-1">
                <span class="text-sm font-medium text-dark">{{ collection.collectionName }}</span>
                <p v-if="collection.description" class="text-xs text-gray-500 mt-1">
                  {{ collection.description }}
                </p>
              </div>
            </label>
          </template>
          <div v-else class="text-center text-gray-500 text-sm p-4">
            <div class="dot-flashing mx-auto mb-2"></div>
            <p>正在加载收藏夹...</p>
          </div>
        </div>
        
        <!-- 创建新收藏夹按钮 -->
        <button
          @click="activeModal = 'createCollection'"
          class="w-full px-4 py-2 border border-dashed border-gray-300 rounded-lg text-sm text-gray-600 hover:border-primary hover:text-primary transition-colors"
        >
          <i class="fa fa-plus mr-2"></i>创建新收藏夹
        </button>
      </div>
      <div class="mt-6 flex justify-end space-x-3">
        <button
          @click="activeModal = null"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm"
        >
          取消
        </button>
        <button
          @click="handleConfirmSave"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm"
        >
          确认收藏
        </button>
      </div>
    </Modal>

    <!-- 创建收藏夹模态框 -->
    <Modal :is-open="activeModal === 'createCollection'" @close="activeModal = null" title="创建新收藏夹">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">收藏夹名称 *</label>
          <input
            type="text"
            placeholder="输入收藏夹名称"
            v-model="newCollectionName"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg"
            maxlength="50"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">描述（可选）</label>
          <textarea
            placeholder="输入收藏夹描述"
            v-model="newCollectionDescription"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg resize-none"
            rows="3"
            maxlength="200"
          />
        </div>
      </div>
      <div class="mt-6 flex justify-end space-x-3">
        <button
          @click="activeModal = 'selectCollection'"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm"
        >
          返回
        </button>
        <button
          @click="handleCreateAndSave"
          :disabled="!newCollectionName.trim() || collectionLoading"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm disabled:bg-primary/50 disabled:cursor-not-allowed"
        >
          {{ collectionLoading ? '创建中...' : '创建并收藏' }}
        </button>
      </div>
    </Modal>

    <!-- 成功模态框 -->
    <Modal :is-open="activeModal === 'saveSuccess'" @close="activeModal = null" hide-title>
      <div class="text-center">
        <div
          class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4"
        >
          <i class="fa fa-check text-green-500 text-3xl"></i>
        </div>
        <h3 class="text-xl font-bold text-gray-800 mb-2">收藏成功</h3>
        <p class="text-sm text-gray-500">您可以在收藏夹页面查看已收藏的查询。</p>
        <button @click="activeModal = null" class="mt-4 px-6 py-2 bg-primary text-white rounded-lg">
          确定
        </button>
      </div>
    </Modal>

    <Modal :is-open="activeModal === 'exportSuccess'" @close="activeModal = null" hide-title>
      <div class="text-center">
        <div
          class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4"
        >
          <i class="fa fa-download text-blue-500 text-2xl"></i>
        </div>
        <h3 class="text-xl font-bold text-gray-800 mb-2">导出已开始</h3>
        <p class="text-sm text-gray-500">文件将保存到您的下载文件夹中。</p>
        <button @click="activeModal = null" class="mt-4 px-6 py-2 bg-primary text-white rounded-lg">
          确定
        </button>
      </div>
    </Modal>

    <Modal :is-open="activeModal === 'shareSuccess'" @close="activeModal = null" hide-title>
      <div class="text-center">
        <div
          class="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center mx-auto mb-4"
        >
          <i class="fa fa-share-alt text-orange-500 text-2xl"></i>
        </div>
        <h3 class="text-xl font-bold text-gray-800 mb-2">分享成功</h3>
        <button @click="activeModal = null" class="mt-4 px-6 py-2 bg-primary text-white rounded-lg">
          确定
        </button>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, onMounted, nextTick } from 'vue'
import { Chart, registerables, type ChartType } from 'chart.js'
import Modal from '../../ui/Modal.vue'
import type { QueryResultData } from '../../../types'
import { useQueryCollection } from '../../../composables/useQueryCollection'
import { friendRelationApi, queryShareChatApi, userApi } from '../../../services/api.real'
import { useToast } from '../../../composables/useToast'

// 注册 Chart.js 组件
Chart.register(...registerables)

// 定义 Props
interface QueryResultProps {
  result: QueryResultData
  onSaveQuery?: (query: QueryResultData) => void
  onShareQuery?: (queryId: string, friendId: string) => void
  savedQueries?: QueryResultData[]
  showActions?: {
    save?: boolean
    share?: boolean
    export?: boolean
  }
}

// 定义 Emits
const emit = defineEmits<{
  (e: 'save-query', query: QueryResultData): void
  (e: 'share-query', queryId: string, friendId: string): void
}>()

// withDefaults 是编译器宏，不需要导入
const props = withDefaults(defineProps<QueryResultProps>(), {
  savedQueries: () => [],
  showActions: () => ({ save: true, share: true, export: true }),
})

// 状态定义
type ViewType = 'table' | 'chart' | 'prompt' | 'sql'

const activeView = ref<ViewType>('table')
const chartType = ref<ChartType>(props.result.chartData?.type || 'bar')
const activeModal = ref<string | null>(null)
const copySuccess = ref(false)
const selectedFriendId = ref<string | null>(null)
const selectedCollectionId = ref<number | string | null>(null)
const newCollectionName = ref('')
const newCollectionDescription = ref('')
const chartCanvas = ref<HTMLCanvasElement | null>(null)
const chartInstance = ref<Chart | null>(null)
const friendsList = ref<Array<{ id: number; name: string; avatarUrl?: string; onlineStatus?: number }>>([])
const isLoadingFriends = ref(false)
const { success, error, warning } = useToast()

// 收藏夹相关
const userId = ref(Number(sessionStorage.getItem('userId') || '1'))
const { collections, loading: collectionLoading, createCollection, addQueryToCollection, getOrCreateDefaultCollection, loadCollections } = useQueryCollection(userId.value)

// 组件挂载时加载收藏夹，并确保有默认收藏夹
onMounted(async () => {
  await loadCollections()
  // 如果没有收藏夹，自动创建默认收藏夹
  if (collections.value.length === 0) {
    await getOrCreateDefaultCollection()
  }
})

// 标签页配置
const tabs = [
  { view: 'table' as ViewType, label: '表格视图' },
  { view: 'chart' as ViewType, label: '图表视图' },
  { view: 'prompt' as ViewType, label: '原始提问' },
  { view: 'sql' as ViewType, label: 'SQL语句' },
]

// 图表类型配置
const chartTypes = [
  { value: 'bar' as ChartType, label: '竖状图', icon: 'fa-bar-chart' },
  { value: 'line' as ChartType, label: '折线图', icon: 'fa-line-chart' },
  { value: 'pie' as ChartType, label: '饼状图', icon: 'fa-pie-chart' },
]

// 计算属性
const hasChartData = computed(() => {
  return (
    props.result.chartData &&
    props.result.chartData.labels &&
    props.result.chartData.labels.length > 0
  )
})

// 判断数据是否为空
const isEmptyData = computed(() => {
  const tableData = props.result.tableData
  if (!tableData || !tableData.rows || tableData.rows.length === 0) {
    return true
  }
  
  // 检查是否所有行都是空数据（null、空字符串、undefined等）
  const hasValidData = tableData.rows.some((row) => {
    if (!row || row.length === 0) {
      return false
    }
    // 检查行中是否有非空的有效数据
    return row.some((cell) => {
      if (cell === null || cell === undefined) {
        return false
      }
      const cellStr = String(cell).trim()
      return cellStr !== '' && cellStr !== 'null' && cellStr !== 'undefined'
    })
  })
  
  return !hasValidData
})

const formatQueryTime = computed(() => {
  return new Date(props.result.queryTime).toLocaleString()
})

// 判断是否已收藏 - 移除自动判断，让用户主动选择收藏
// 注意：这里不再自动判断，用户需要主动点击收藏按钮
const isSaved = ref(false)

// 创建图表
const createChart = () => {
  if (!chartCanvas.value || !props.result.chartData || !hasChartData.value) return

  const ctx = chartCanvas.value.getContext('2d')
  if (!ctx) return

  chartInstance.value = new Chart(ctx, {
    type: chartType.value,
    data: {
      labels: props.result.chartData.labels,
      datasets: props.result.chartData.datasets,
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: chartType.value === 'pie',
        },
      },
    },
  })
}

// 销毁图表
const destroyChart = () => {
  if (chartInstance.value) {
    chartInstance.value.destroy()
    chartInstance.value = null
  }
}
// 监听图表类型变化
watch(
  () => props.result.chartData?.type,
  (newType) => {
    if (newType) {
      chartType.value = newType
    }
  },
)

// 监听视图和图表数据变化，更新图表
watch(
  [activeView, chartType, () => props.result.chartData],
  ([view, _type, chartData]) => {
    if (view === 'chart' && chartData && hasChartData.value) {
      destroyChart()
      nextTick(() => {
        createChart()
      })
    } else {
      destroyChart()
    }
  },
  { immediate: true },
)

// 组件卸载时销毁图表
onUnmounted(() => {
  destroyChart()
})

// 复制 SQL 语句
const handleCopySql = async () => {
  try {
    await navigator.clipboard.writeText(props.result.sqlQuery)
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}

// 加载好友列表
const loadFriendsList = async () => {
  try {
    isLoadingFriends.value = true
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const relations = await friendRelationApi.getByUser(userId)
    
    // 为每个好友获取用户信息
    const friendsData = await Promise.all(
      relations.map(async (relation) => {
        try {
          const friendUser = await userApi.getById(relation.friendId)
          return {
            id: relation.friendId,
            name: relation.remarkName || friendUser.username,
            avatarUrl: friendUser.avatarUrl || '/default-avatar.png',
            onlineStatus: relation.onlineStatus || 0
          }
        } catch (err) {
          console.error(`获取好友 ${relation.friendId} 信息失败:`, err)
          // 如果获取失败，使用基本信息
          return {
            id: relation.friendId,
            name: relation.remarkName || `用户${relation.friendId}`,
            avatarUrl: '/default-avatar.png',
            onlineStatus: relation.onlineStatus || 0
          }
        }
      })
    )
    
    friendsList.value = friendsData
  } catch (err) {
    console.error('加载好友列表失败:', err)
    error('加载好友列表失败')
    friendsList.value = []
  } finally {
    isLoadingFriends.value = false
  }
}

// 分享相关方法
const handleOpenShareModal = async () => {
  selectedFriendId.value = null
  activeModal.value = 'share'
  // 打开模态框时加载好友列表
  if (friendsList.value.length === 0) {
    await loadFriendsList()
  }
}

const handleConfirmShare = async () => {
  if (!selectedFriendId.value) {
    warning('请选择要分享的好友')
    return
  }

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const shareData = {
      shareUserId: userId,
      receiveUserId: Number(selectedFriendId.value),
      queryTitle: props.result.userPrompt || '查询结果',
      sqlQuery: props.result.sqlQuery || '',
      databaseName: props.result.database || '',
      llmName: props.result.model || '',
      executionTime: typeof props.result.executionTime === 'number' 
        ? props.result.executionTime 
        : parseExecutionTime(props.result.executionTime),
      queryTime: props.result.queryTime || new Date().toISOString(),
      dbConnectionId: props.result.dbConnectionId || null,
      llmConfigId: props.result.llmConfigId || null,
      dialogId: props.result.conversationId || '',
      tableData: props.result.tableData ? {
        headers: props.result.tableData.headers,
        rows: props.result.tableData.rows
      } : undefined,
      chartData: props.result.chartData ? {
        type: props.result.chartData.type,
        labels: props.result.chartData.labels || [],
        datasets: props.result.chartData.datasets || []
      } : undefined
    }

    await queryShareChatApi.share(shareData as any)
    
    success('分享成功')
    activeModal.value = null
    
    // 调用父组件的分享函数或触发事件（用于更新状态）
    if (props.onShareQuery) {
      props.onShareQuery(props.result.id, selectedFriendId.value)
    } else {
      emit('share-query', props.result.id, selectedFriendId.value)
    }
    
    setTimeout(() => {
      activeModal.value = 'shareSuccess'
    }, 350)
  } catch (err) {
    console.error('分享失败:', err)
    error(err instanceof Error ? err.message : '分享失败')
  }
}

// 解析执行时间
function parseExecutionTime(timeStr: string | number | undefined): number {
  if (typeof timeStr === 'number') return timeStr
  if (!timeStr) return 0
  
  // 如果是 "123ms" 或 "1.23s" 格式
  if (typeof timeStr === 'string') {
    if (timeStr.endsWith('ms')) {
      return parseInt(timeStr)
    }
    if (timeStr.endsWith('s')) {
      return parseFloat(timeStr) * 1000
    }
    return parseInt(timeStr) || 0
  }
  
  return 0
}

// 收藏查询 - 打开选择收藏夹对话框
const handleSave = async () => {
  activeModal.value = 'selectCollection'
  // 确保收藏夹列表已加载
  if (collections.value.length === 0) {
    await loadCollections()
    // 如果没有收藏夹，自动创建默认收藏夹
    if (collections.value.length === 0) {
      await getOrCreateDefaultCollection()
    }
  }
  // 如果没有选择收藏夹，默认选择第一个（通常是默认收藏夹）
  if (!selectedCollectionId.value && collections.value.length > 0) {
    selectedCollectionId.value = collections.value[0].id
  }
}

// 确认收藏 - 保存到选中的收藏夹（如果没有选择则使用默认收藏夹）
const handleConfirmSave = async () => {
  try {
    // 确保查询已保存到历史记录（获取真实的queryLogId）
    let queryLogId = Number(props.result.id)
    const isRealQueryLogId = !isNaN(queryLogId) && queryLogId > 0

    // 如果不是真实的queryLogId，需要先保存查询
    if (!isRealQueryLogId) {
      // 先保存查询到历史记录
      const { queryLogApi } = await import('../../../services/api.real')
      const userId = Number(sessionStorage.getItem('userId') || '1')
      const savedLog = await queryLogApi.create({
        userId,
        userPrompt: props.result.userPrompt,
        sqlQuery: props.result.sqlQuery,
        queryResult: JSON.stringify({
          tableData: props.result.tableData,
          chartData: props.result.chartData,
        }),
        dialogId: props.result.conversationId || '',
        dbConnectionId: props.result.dbConnectionId || Number(props.result.database) || 0,
        llmConfigId: props.result.llmConfigId || Number(props.result.model) || 0,
        queryTime: props.result.queryTime || new Date().toISOString(),
        executionTime: props.result.executionTime || '0ms',
      })
      queryLogId = savedLog.id
    }

    // 确定收藏夹ID：如果用户选择了收藏夹，使用选择的；否则使用默认收藏夹
    let collectionId: number | string | null = selectedCollectionId.value
    
    console.log('收藏确认: selectedCollectionId=', selectedCollectionId.value, 'type=', typeof selectedCollectionId.value)
    
    // 如果没有选择收藏夹，获取或创建默认收藏夹
    if (!collectionId) {
      console.log('没有选择收藏夹，获取默认收藏夹...')
      const defaultCollection = await getOrCreateDefaultCollection()
      if (!defaultCollection) {
        alert('无法创建默认收藏夹，请重试')
        return
      }
      collectionId = defaultCollection.id
      console.log('获取到默认收藏夹: id=', collectionId, 'type=', typeof collectionId)
    }

    // 保存到收藏夹，传递完整数据（包括执行耗时）
    if (!collectionId) {
      console.error('无法确定收藏夹，collectionId=', collectionId)
      alert('无法确定收藏夹，请重试')
      return
    }

    console.log('准备添加到收藏夹: collectionId=', collectionId, 'queryLogId=', queryLogId)

    const success = await addQueryToCollection(
      collectionId,
      queryLogId,
      {
        userId: Number(sessionStorage.getItem('userId') || '1'),
        sqlContent: props.result.sqlQuery,
        userPrompt: props.result.userPrompt,
        queryResult: {
          tableData: props.result.tableData,
          chartData: props.result.chartData,
          database: props.result.database,
          model: props.result.model,
          executionTime: props.result.executionTime,
          conversationId: props.result.conversationId,
        },
        dbConnectionId: props.result.dbConnectionId || Number(props.result.database) || 0,
        llmConfigId: props.result.llmConfigId || Number(props.result.model) || 0,
      }
    )

    if (success) {
      isSaved.value = true  // 标记为已收藏
      activeModal.value = null
      setTimeout(() => {
        activeModal.value = 'saveSuccess'
      }, 350)
      
      // 触发父组件事件
      if (props.onSaveQuery) {
        props.onSaveQuery(props.result)
      } else {
        emit('save-query', props.result)
      }
      
      // 触发全局事件，通知收藏页面刷新
      window.dispatchEvent(new CustomEvent('collection-updated', {
        detail: { queryId: queryLogId, collectionId }
      }))
      console.log('[QueryResult] 已触发收藏更新事件')
    } else {
      alert('收藏失败，请重试')
    }
  } catch (error) {
    console.error('收藏失败:', error)
    alert('收藏失败，请重试')
  }
}

// 创建收藏夹并保存
const handleCreateAndSave = async () => {
  if (!newCollectionName.value.trim()) {
    alert('请输入收藏夹名称')
    return
  }

  try {
    const newCollection = await createCollection(newCollectionName.value, newCollectionDescription.value)
    if (newCollection) {
      // MongoDB 返回的 id 是 String，需要转换为 number（如果可能）
      selectedCollectionId.value = typeof newCollection.id === 'string' 
        ? Number(newCollection.id) || newCollection.id 
        : newCollection.id
      newCollectionName.value = ''
      newCollectionDescription.value = ''
      // 创建成功后，保存查询
      await handleConfirmSave()
    } else {
      alert('创建收藏夹失败')
    }
  } catch (error) {
    console.error('创建收藏夹失败:', error)
    alert('创建收藏夹失败，请重试')
  }
}

// 导出 CSV
const handleExport = () => {
  const { headers, rows } = props.result.tableData
  const csvContent =
    'data:text/csv;charset=utf-8,' +
    [headers.join(','), ...rows.map((row) => row.join(','))].join('\n')

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement('a')
  link.setAttribute('href', encodedUri)
  link.setAttribute('download', `query_result_${props.result.id}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  activeModal.value = 'exportSuccess'
}
</script>

<style scoped></style>
