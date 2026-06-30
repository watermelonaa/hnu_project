<!--
  @file views/data-admin/ConnectionLogPage.vue
  @description 数据源连接日志页面

  功能：
  - 连接日志列表展示
  - 按数据源筛选
  - 日志详情查看
  - 连接状态统计

  @author Frontend Team
-->
<template>
  <div class="w-full p-6 space-y-6">
    <!-- 加载状态 -->
    <div
      v-if="loading"
      class="flex-1 overflow-y-auto p-6 space-y-6 flex items-center justify-center"
    >
      <div class="text-center">
        <div class="dot-flashing"></div>
        <p class="mt-4 text-gray-500">加载连接日志中...</p>
      </div>
    </div>

    <!-- 主内容 -->
    <div v-else class="space-y-6">
      <!-- 搜索和导出区域 -->
      <div class="bg-white rounded-xl p-6 shadow-sm">
        <div class="flex flex-col md:flex-row gap-4">
          <div class="flex-1">
            <div class="relative">
              <i class="fa fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></i>
              <input
                type="text"
                placeholder="搜索日志..."
                v-model="searchTerm"
                class="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-primary/30"
              />
            </div>
          </div>
          <div class="w-full md:w-40">
            <select v-model="searchType" class="w-full px-4 py-2 border border-gray-300 rounded-lg text-sm">
              <option value="all">全部字段</option>
              <option value="time">时间</option>
              <option value="datasource">数据源</option>
              <option value="status">状态</option>
            </select>
          </div>
          <div class="w-full md:w-auto">
            <button
              @click="setExportModalOpen(true)"
              class="w-full md:w-auto bg-white border border-gray-300 rounded-lg px-4 py-2 text-sm btn-effect hover:bg-gray-50"
            >
              <i class="fa fa-download mr-1"></i> 导出日志
            </button>
          </div>
        </div>
      </div>

      <!-- 日志表格 -->
      <div class="bg-white rounded-xl shadow-sm border overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="bg-gray-50 border-b">
              <tr>
                <th class="px-4 py-3 text-left font-semibold">时间</th>
                <th class="px-4 py-3 text-left font-semibold">数据源</th>
                <th class="px-4 py-3 text-left font-semibold">状态</th>
                <th class="px-4 py-3 text-center font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="filteredLogs.length > 0">
                <tr v-for="log in filteredLogs" :key="log.id" class="border-b hover:bg-gray-50">
                  <td class="px-4 py-3">{{ log.time }}</td>
                  <td class="px-4 py-3">{{ log.datasource }}</td>
                  <td class="px-4 py-3">
                    <span
                      :class="`px-2 py-1 rounded text-xs ${log.status === '成功' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'}`"
                    >
                      {{ log.status }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-center">
                    <button
                      @click="setViewingLog(log)"
                      class="text-primary hover:underline text-xs"
                    >
                      查看详情
                    </button>
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr>
                  <td colspan="4" class="px-4 py-8 text-center text-gray-500">
                    <i class="fa fa-inbox text-3xl mb-2 block"></i>
                    暂无日志记录
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 导出确认模态框 -->
      <AdminModal :isOpen="isExportModalOpen" @close="setExportModalOpen(false)" title="导出日志">
        <p class="text-sm text-gray-700 mb-4">
          确定要导出当前筛选的 {{ filteredLogs.length }} 条日志记录吗？
        </p>
        <div class="flex justify-end space-x-3">
          <button
            @click="setExportModalOpen(false)"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm"
          >
            取消
          </button>
          <button @click="handleExport" class="px-4 py-2 bg-primary text-white rounded-lg text-sm">
            确认导出
          </button>
        </div>
      </AdminModal>

      <!-- 日志详情模态框 -->
      <AdminModal :isOpen="!!viewingLog" @close="setViewingLog(null)" title="日志详情">
        <div v-if="viewingLog" class="space-y-3 text-sm">
          <div>
            <span class="font-semibold">时间：</span>
            {{ viewingLog.time }}
          </div>
          <div>
            <span class="font-semibold">数据源：</span>
            {{ viewingLog.datasource }}
          </div>
          <div>
            <span class="font-semibold">状态：</span>
            <span
              :class="`ml-2 px-2 py-1 rounded text-xs ${viewingLog.status === '成功' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'}`"
            >
              {{ viewingLog.status }}
            </span>
          </div>
          <div>
            <span class="font-semibold">详情：</span>
            {{ viewingLog.details }}
          </div>
        </div>
      </AdminModal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import { dbConnectionLogApi, dbConnectionApi } from '../../services/api.real'

// 类型定义
interface ConnectionLog {
  id: string
  time: string
  datasource: string
  status: '成功' | '失败'
  details: string
}

// 状态定义
const logs = ref<ConnectionLog[]>([])
const loading = ref(true)
const isExportModalOpen = ref(false)
const viewingLog = ref<ConnectionLog | null>(null)
const searchTerm = ref('')
const searchType = ref<'all' | 'time' | 'datasource' | 'status'>('all')

// 计算属性 - 过滤后的日志
const filteredLogs = computed(() => {
  if (!searchTerm.value.trim()) return logs.value

  const term = searchTerm.value.toLowerCase()
  return logs.value.filter((log) => {
    switch (searchType.value) {
      case 'time':
        return new Date(log.time).toLocaleString().toLowerCase().includes(term)
      case 'datasource':
        return log.datasource.toLowerCase().includes(term)
      case 'status':
        return log.status.toLowerCase().includes(term)
      default:
        return (
          new Date(log.time).toLocaleString().toLowerCase().includes(term) ||
          log.datasource.toLowerCase().includes(term) ||
          log.status.toLowerCase().includes(term)
        )
    }
  })
})

// 生命周期
onMounted(() => {
  loadLogs()
})

// 解析状态
const parseStatus = (status: string) => {
  const s = (status || '').toLowerCase()
  if (['connected', 'success', 'ok', '1', 'true'].includes(s)) {
    return { text: '成功', isSuccess: true }
  }
  if (['disconnected', 'error', 'fail', 'failed', '0', 'false'].includes(s)) {
    return { text: '失败', isSuccess: false }
  }
  return { text: status || '未知', isSuccess: false }
}

// 加载日志
const loadLogs = async () => {
  try {
    loading.value = true
    const connectionLogs = await dbConnectionLogApi.getList()

    const logData: ConnectionLog[] = await Promise.all(
      connectionLogs.map(async (log: any) => {
        let datasourceName = 'Unknown'
        try {
          //const connection = await dbConnectionApi.getById(log.dbConnectionId)
          datasourceName = log.dbName
        } catch (error) {
          console.error('获取数据源信息失败:', error)
        }

        const statusParsed = parseStatus(log.status)

        return {
          id: String(log.id),
          time: new Date(log.connectTime).toLocaleString(),
          datasource: datasourceName,
          status: statusParsed.text as ConnectionLog['status'],
          details: log.remark || log.errorMessage || '连接成功',
        }
      }),
    )

    logs.value = logData
  } catch (error) {
    console.error('加载连接日志失败:', error)
  } finally {
    loading.value = false
  }
}

// 导出日志
const handleExport = () => {
  const csvContent =
    'data:text/csv;charset=utf-8,' +
    '时间,数据源,状态,详情\n' +
    filteredLogs.value
      .map((log) => `${log.time},${log.datasource},${log.status},"${log.details}"`)
      .join('\n')

  const encodedUri = encodeURI(csvContent)
  const link = document.createElement('a')
  link.setAttribute('href', encodedUri)
  link.setAttribute('download', `connection_logs_${new Date().toISOString().split('T')[0]}.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  isExportModalOpen.value = false
  alert('日志已导出')
}

// 设置导出模态框状态
const setExportModalOpen = (value: boolean) => {
  isExportModalOpen.value = value
}

// 设置查看日志
const setViewingLog = (log: ConnectionLog | null) => {
  viewingLog.value = log
}
</script>

<!-- 使用全局 index.css 中的 .dot-flashing 样式 -->
