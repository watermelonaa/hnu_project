<!--
  @file views/admin/SystemLogPage.vue
  @description 系统日志页面

  功能：
  - 操作日志列表展示
  - 日志筛选（用户、模块、状态）
  - 日志详情查看
  - 导出功能

  @author Frontend Team
-->
<template>
  <div class="p-6 space-y-6">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="text-center">
        <div class="dot-flashing"></div>
        <p class="mt-4 text-gray-500">加载系统日志中...</p>
      </div>
    </div>

    <!-- 主内容 -->
    <template v-else>
      <!-- 导出按钮 -->
      <div class="flex justify-between items-center">
        <button
          @click="setExportModalOpen(true)"
          class="bg-white border border-gray-300 rounded-lg px-4 py-2 text-sm btn-effect"
        >
          <i class="fa fa-download mr-1"></i> 导出日志
        </button>
      </div>

      <!-- 筛选区域 -->
      <div class="bg-white rounded-xl p-4 shadow-sm">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <div class="md:col-span-2 lg:col-span-2">
            <label class="block text-sm mb-1">操作时间</label>
            <div class="flex space-x-2">
          <input
            type="date"
            name="startDate"
            v-model="filters.startDate"
            @change="handleFilterChange"
                class="flex-1 px-3 py-2 border rounded-lg"
          />
              <span class="flex items-center">-</span>
          <input
            type="date"
            name="endDate"
            v-model="filters.endDate"
            @change="handleFilterChange"
                class="flex-1 px-3 py-2 border rounded-lg"
          />
            </div>
          </div>
          <div>
            <label class="block text-sm mb-1">操作用户</label>
          <input
            type="text"
            name="user"
            v-model="filters.user"
            @input="handleFilterChange"
              placeholder="输入用户名"
              class="w-full px-3 py-2 border rounded-lg"
            />
          </div>
          <div>
            <label class="block text-sm mb-1">模块</label>
            <select
              name="module"
              v-model="filters.module"
              @change="handleFilterChange"
              class="w-full px-3 py-2 border rounded-lg"
            >
              <option value="">全部模块</option>
              <option value="系统异常">系统异常</option>
              <option value="业务异常">业务异常</option>
              <option value="数据库异常">数据库异常</option>
              <option value="网络异常">网络异常</option>
              <option value="系统操作">系统操作</option>
              <option value="查询操作">查询操作</option>
              <option value="大模型配置">大模型配置</option>
            </select>
          </div>
          <div>
            <label class="block text-sm mb-1">状态</label>
          <select
            name="status"
            v-model="filters.status"
            @change="handleFilterChange"
              class="w-full px-3 py-2 border rounded-lg"
          >
            <option value="">全部状态</option>
            <option value="成功">成功</option>
            <option value="失败">失败</option>
          </select>
        </div>
          <div>
            <label class="block text-sm mb-1 invisible">占位</label>
            <button @click="resetFilters" class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm btn-effect">
              <i class="fa fa-refresh mr-1"></i> 重置
        </button>
          </div>
        </div>
      </div>

      <!-- 日志表格 -->
      <div class="bg-white rounded-xl shadow-sm overflow-hidden system-log-table-container">
        <div class="overflow-x-auto system-log-table-wrapper">
          <table class="w-full system-log-table">
            <thead>
              <tr class="bg-gray-50 border-b">
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">ID</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">操作时间</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">操作用户</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">模块</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">操作内容</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">涉及模型</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">状态</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700 whitespace-nowrap">操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="paginatedLogs.length > 0">
                <tr v-for="log in paginatedLogs" :key="log.id" class="border-b hover:bg-gray-50 transition-colors">
                  <td class="px-6 py-4 text-gray-900 whitespace-nowrap">{{ log.id }}</td>
                  <td class="px-6 py-4 text-gray-700 whitespace-nowrap">{{ log.time }}</td>
                  <td class="px-6 py-4 text-gray-700 whitespace-nowrap">{{ log.user }}</td>
                  <td class="px-6 py-4 text-gray-700 whitespace-nowrap">
                    <span class="px-2 py-1 text-xs rounded bg-blue-50 text-blue-700">
                      {{ log.module }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-gray-700">{{ log.action }}</td>
                  <td class="px-6 py-4 text-gray-600 whitespace-nowrap">{{ log.model || '-' }}</td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span
                      :class="`px-2 py-1 text-xs rounded-full ${log.status === '成功' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'}`"
                    >
                      {{ log.status }}
                    </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <button
                      v-if="log.status === '失败' && log.details"
                      @click="setViewingLog(log)"
                      class="text-primary hover:text-primary-dark hover:underline transition-colors text-sm"
                    >
                      查看详情
                    </button>
                    <span v-else class="text-gray-400">-</span>
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr>
                  <td colspan="8" class="px-6 py-12 text-center text-gray-500">
                    <i class="fa fa-inbox text-4xl mb-3 block text-gray-400"></i>
                    <p class="text-sm">暂无日志记录</p>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <div class="px-4 md:px-6 py-4 border-t bg-gray-50 flex justify-between items-center">
          <p class="text-sm text-gray-500">显示 {{ paginatedLogs.length }} 条，共 {{ filteredLogs.length }} 条</p>
          <!-- 分页控件 -->
          <div class="flex items-center space-x-2">
            <button
              @click="goToPage(currentPage - 1)"
              :disabled="currentPage === 1"
              class="px-3 py-1 border rounded-lg text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              <i class="fa fa-chevron-left"></i>
            </button>
            <span class="text-sm text-gray-700">
              第 {{ currentPage }} / {{ totalPages }} 页
            </span>
            <button
              @click="goToPage(currentPage + 1)"
              :disabled="currentPage === totalPages"
              class="px-3 py-1 border rounded-lg text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              <i class="fa fa-chevron-right"></i>
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- 导出模态框 -->
    <AdminModal :isOpen="isExportModalOpen" @close="handleCloseExportModal" title="导出日志">
      <div class="space-y-4">
      <p class="text-sm text-gray-700 mb-4">
        确定要导出当前筛选的 {{ filteredLogs.length }} 条日志记录吗？
      </p>
      <div class="flex justify-end space-x-3">
        <button
            type="button"
            @click.stop="handleCloseExportModal"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm btn-effect hover:bg-gray-50"
        >
          取消
        </button>
          <button
            type="button"
            @click.stop="handleExport"
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm btn-effect hover:bg-primary-dark"
          >
          确认导出
        </button>
        </div>
      </div>
    </AdminModal>

    <!-- 日志详情模态框 -->
    <AdminModal
      :isOpen="!!viewingLog"
      @close="setViewingLog(null)"
      title="日志详情"
    >
      <div v-if="viewingLog" class="space-y-3 text-sm">
        <div><span class="font-semibold">时间：</span>{{ viewingLog.time }}</div>
        <div><span class="font-semibold">用户：</span>{{ viewingLog.user }}</div>
        <div><span class="font-semibold">操作：</span>{{ viewingLog.action }}</div>
        <div><span class="font-semibold">模块：</span>{{ viewingLog.module }}</div>
        <div><span class="font-semibold">状态：</span>
          <span
            :class="`ml-2 px-2 py-1 rounded text-xs ${viewingLog.status === '成功' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'}`"
          >
            {{ viewingLog.status }}
          </span>
        </div>
        <div><span class="font-semibold">详情：</span>{{ viewingLog.details }}</div>
      </div>
      <div class="flex justify-end mt-4 pt-4 border-t">
        <button
          type="button"
          @click="setViewingLog(null)"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm"
        >
          关闭
        </button>
      </div>
    </AdminModal>

    <!-- 消息提示模态框 -->
    <AdminModal :isOpen="messageModal.isOpen" @close="closeMessageModal" :title="messageModal.title">
      <div class="space-y-4">
        <div class="flex items-center space-x-3">
          <i
            :class="`fa text-2xl ${
              messageModal.type === 'success' ? 'fa-check-circle text-success' : 'fa-exclamation-circle text-danger'
            }`"
          ></i>
          <p class="text-gray-700 flex-1">{{ messageModal.message }}</p>
        </div>
        <div class="flex justify-end">
          <button
            type="button"
            @click="closeMessageModal"
            :class="`px-4 py-2 rounded-lg text-sm ${
              messageModal.type === 'success'
                ? 'bg-primary text-white hover:bg-primary-dark'
                : 'bg-danger text-white hover:bg-danger-dark'
            }`"
          >
            确定
          </button>
        </div>
      </div>
    </AdminModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import { operationLogApi, userApi } from '../../services/api.real'
import { config } from '../../config'

// 类型定义 - 与旧版本类型定义保持一致，但状态使用中文显示
interface SystemLog {
  id: string
  time: string
  user: string
  action: string
  module: string
  model: string
  status: '成功' | '失败' // 显示用中文，但内部可以映射 'success' | 'failure'
  details: string
}

// Props
interface Props {
  initialStatusFilter: string
  clearInitialFilter: () => void
}

const props = defineProps<Props>()

// 状态定义
const logs = ref<SystemLog[]>([])
const loading = ref(true)
const filters = ref({
  startDate: '',
  endDate: '',
  user: '',
  module: '',
  status: props.initialStatusFilter || '',
})
const isExportModalOpen = ref(false)
const viewingLog = ref<SystemLog | null>(null)

// 消息模态框状态
const messageModal = ref<{
  isOpen: boolean
  type: 'success' | 'error'
  title: string
  message: string
}>({
  isOpen: false,
  type: 'success',
  title: '提示',
  message: '',
})

// 分页状态
const currentPage = ref(1)
const pageSize = ref(10)

// 监听 initialStatusFilter 变化
watch(
  () => props.initialStatusFilter,
  (newValue) => {
    if (newValue) {
      filters.value.status = newValue
      props.clearInitialFilter()
    }
  },
)

// 计算属性 - 过滤后的日志
const filteredLogs = computed(() => {
  let result = logs.value.filter(
    (log) => {
      // 用户筛选
      const userMatch = !filters.value.user || log.user.toLowerCase().includes(filters.value.user.toLowerCase())
      
      // 模块筛选
      const moduleMatch = !filters.value.module || log.module === filters.value.module
      
      // 状态筛选
      const statusMatch = !filters.value.status || log.status === filters.value.status
      
      // 时间筛选 - 正确解析时间字符串进行比较
      let timeMatch = true
      if (filters.value.startDate || filters.value.endDate) {
        try {
          // 解析日志时间（格式：YYYY-MM-DD HH:mm:ss 或 YYYY/MM/DD HH:mm:ss）
          const logTimeStr = log.time.replace(/\//g, '-')
          const logDate = new Date(logTimeStr)
          
          if (filters.value.startDate) {
            const startDate = new Date(filters.value.startDate + ' 00:00:00')
            if (logDate < startDate) {
              timeMatch = false
            }
          }
          
          if (filters.value.endDate && timeMatch) {
            const endDate = new Date(filters.value.endDate + ' 23:59:59')
            if (logDate > endDate) {
              timeMatch = false
            }
          }
        } catch (e) {
          console.error('时间解析失败:', e, log.time)
          // 如果时间解析失败，默认不匹配（更安全）
          timeMatch = false
        }
      }
      
      return userMatch && moduleMatch && statusMatch && timeMatch
    },
  )
  
  // 按时间倒序排序（最新的在前）
  result = result.sort((a, b) => {
    try {
      const timeA = new Date(a.time.replace(/\//g, '-')).getTime()
      const timeB = new Date(b.time.replace(/\//g, '-')).getTime()
      return timeB - timeA // 倒序
    } catch (e) {
      return 0
    }
  })
  
  return result
})

// 计算属性 - 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredLogs.value.length / pageSize.value) || 1
})

// 计算属性 - 当前页的日志
const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  const result = filteredLogs.value.slice(start, end)
  // 确保每页显示10条（如果数据足够）
  return result
})

// 监听筛选变化，重置到第一页
watch(filteredLogs, () => {
  currentPage.value = 1
})

// 生命周期
onMounted(() => {
  loadLogs()
})

// 方法
const loadLogs = async (useTimeFilter = false) => {
  try {
    loading.value = true
    console.log('开始加载系统日志...')
    
    try {
    // 如果有时间筛选条件，使用时间范围查询接口
    let operationLogs
    if (useTimeFilter && (filters.value.startDate || filters.value.endDate)) {
      const startTime = filters.value.startDate ? filters.value.startDate + ' 00:00:00' : undefined
      const endTime = filters.value.endDate ? filters.value.endDate + ' 23:59:59' : undefined
      operationLogs = await operationLogApi.getByTimeRange(startTime, endTime)
    } else {
      operationLogs = await operationLogApi.getList()
    }
      console.log('API 返回数据:', operationLogs)
      console.log('API 返回数据数量:', operationLogs?.length || 0)

      if (!operationLogs || !Array.isArray(operationLogs)) {
        console.error('API 返回数据格式错误:', operationLogs)
        logs.value = []
        loading.value = false
        return
      }
      
      // 调试：打印所有日志的模块和操作
      console.log('所有日志的模块和操作:', operationLogs.map((log: any) => ({
        id: log.id,
        module: log.module,
        operation: log.operation,
        result: log.result,
        status: log.status
      })))

      // 根据React版本SystemLogPageWithAPI的逻辑进行数据映射
      // 实际API返回字段：id, userId, username, module, operateTime, operation, result, relatedLlm, errorMsg
      // React版本期望字段：operateType, operateDesc, status
      // 需要兼容两种字段名
    const systemLogs: SystemLog[] = await Promise.all(
      operationLogs.map(async (log: any) => {
          // 获取用户名：与React版本一致，优先使用username，否则通过userId获取
          let username = 'Unknown'
          if (log.username) {
            username = log.username
          } else if (log.userId) {
            try {
            const user = await userApi.getById(log.userId)
            username = user.username
        } catch (error) {
          console.error('获取用户信息失败:', error)
            }
        }

          // 处理状态：优先使用status，否则使用result（React版本使用status）
          // status/result: 1=成功, 0或其他=失败
          const statusValue = log.status !== undefined ? log.status : (log.result !== undefined ? log.result : 0)
        const statusText: SystemLog['status'] = statusValue === 1 ? '成功' : '失败'

          // 处理操作类型：优先使用operateType，否则使用operation（React版本使用operateType）
          const actionText = log.operateType || log.operation || '未知操作'

          // 处理详情：优先使用operateDesc，否则使用errorMsg（React版本使用operateDesc）
          const detailText = log.operateDesc || log.errorMsg || ''

          // 处理时间：与React版本一致
          const timeText = log.operateTime ? new Date(log.operateTime).toLocaleString('zh-CN') : ''

          // 处理模型：使用 relatedLlm 字段
          const modelText = log.relatedLlm || ''

        return {
          id: String(log.id),
          time: timeText,
          user: username,
          action: actionText,
          module: log.module || '系统操作',
            model: modelText,
          status: statusText,
          details: detailText,
        }
      }),
    )

      console.log('处理后的日志数据:', systemLogs)
      console.log('处理后的日志数量:', systemLogs.length)
      console.log('当前筛选条件:', {
        module: filters.value.module,
        status: filters.value.status,
        user: filters.value.user,
        startDate: filters.value.startDate,
        endDate: filters.value.endDate
      })
      // 调试：统计各模块的日志数量
      const moduleStats = systemLogs.reduce((acc: any, log: SystemLog) => {
        acc[log.module] = (acc[log.module] || 0) + 1
        return acc
      }, {})
      console.log('各模块日志统计:', moduleStats)
    logs.value = systemLogs
    } catch (apiError: any) {
      console.error('API调用失败:', apiError)
      console.error('错误详情:', apiError?.message || String(apiError))
      
      // 生产环境显示错误提示
      if (apiError?.message?.includes('500') || apiError?.message?.includes('Internal Server Error')) {
        console.error('服务器内部错误，可能是后端接口问题')
        showMessage('服务器错误，请稍后重试或联系管理员', 'error', '服务器错误')
      } else {
        showMessage(`加载系统日志失败: ${apiError?.message || '未知错误'}`, 'error', '加载失败')
      }
      logs.value = []
    }
  } catch (error) {
    console.error('加载系统日志失败:', error)
    console.error('错误详情:', error instanceof Error ? error.message : String(error))
    // 显示错误提示
    showMessage(`加载系统日志失败: ${error instanceof Error ? error.message : '未知错误'}`, 'error', '加载失败')
    logs.value = []
  } finally {
    loading.value = false
  }
}

const handleFilterChange = (e: Event) => {
  const target = e.target as HTMLInputElement | HTMLSelectElement
  const oldValue = filters.value[target.name as keyof typeof filters.value]
  filters.value = { ...filters.value, [target.name]: target.value }
  
  // 如果时间筛选条件变化，重新从服务器加载数据
  if (target.name === 'startDate' || target.name === 'endDate') {
    if (oldValue !== target.value) {
      // 使用nextTick确保filters已更新
      setTimeout(() => {
        loadLogs(true)
      }, 0)
    }
  }
}

const resetFilters = () => {
  filters.value = { startDate: '', endDate: '', user: '', module: '', status: '' }
  currentPage.value = 1
}

// 分页方法
const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

// CSV转义函数：处理包含逗号、引号、换行符的字段
const escapeCsvField = (field: string): string => {
  if (field === null || field === undefined) {
    return ''
  }
  const str = String(field)
  // 如果字段包含逗号、引号或换行符，需要用引号包裹，并转义内部引号
  if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
    return `"${str.replace(/"/g, '""')}"`
  }
  return str
}

const handleExport = () => {
  try {
    // 检查是否有数据
    if (!filteredLogs.value || filteredLogs.value.length === 0) {
      isExportModalOpen.value = false
      return
    }

    // 生成CSV表头
    const headers = ['ID', '时间', '用户', '操作', '模块', '涉及模型', '状态', '详情']
    
    // 生成CSV数据行
    const rows = filteredLogs.value.map((log) => [
      log.id,
      log.time,
      log.user,
      log.action,
      log.module,
      log.model || '-',
      log.status,
      log.details,
    ])

    // 构建CSV内容
    const csvRows = [
      headers.map(escapeCsvField).join(','),
      ...rows.map((row) => row.map(escapeCsvField).join(',')),
    ]
    
    const csvContent = csvRows.join('\n')
    
    // 添加BOM（Byte Order Mark）以支持Excel正确识别UTF-8编码
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    
    // 先关闭导出确认模态框
    isExportModalOpen.value = false
    
    // 创建下载链接
    const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
    link.setAttribute('href', url)
  link.setAttribute('download', `system_logs_${new Date().toISOString().split('T')[0]}.csv`)
  document.body.appendChild(link)
    
    // 触发下载
  link.click()
    
    // 清理资源
    setTimeout(() => {
      try {
  document.body.removeChild(link)
        URL.revokeObjectURL(url)
      } catch (cleanupError) {
        console.error('清理下载资源失败:', cleanupError)
      }
    }, 100)
  } catch (error) {
    console.error('导出日志失败:', error)
    // 确保导出模态框已关闭
  isExportModalOpen.value = false
  }
}

const setExportModalOpen = (value: boolean) => {
  isExportModalOpen.value = value
}

const handleCloseExportModal = () => {
  isExportModalOpen.value = false
}

const setViewingLog = (log: SystemLog | null) => {
  viewingLog.value = log
}

// 显示消息模态框
const showMessage = (message: string, type: 'success' | 'error' = 'success', title?: string) => {
  messageModal.value = {
    isOpen: true,
    type,
    title: title || (type === 'success' ? '操作成功' : '操作失败'),
    message,
  }
}

// 关闭消息模态框
const closeMessageModal = () => {
  messageModal.value.isOpen = false
}
</script>

<!-- 使用全局 index.css 中的 .dot-flashing 样式 -->
