<!--
  @file views/admin/DashboardPage.vue
  @description 系统管理员仪表盘页面

  功能：
  - 系统概览统计卡片
  - 用户数量、查询数量等指标
  - 快捷操作入口

  @author Frontend Team
-->
<template>
  <div class="flex-1 overflow-y-auto p-6 space-y-6">
    <!-- 操作按钮 -->
    <div class="flex justify-between items-center">
      <div class="flex items-center space-x-2">
        <button
          @click="handleRefresh"
          :disabled="isRefreshing"
          class="bg-white border border-gray-300 rounded-lg px-3 py-2 text-sm btn-effect disabled:opacity-50 flex items-center"
        >
          <i :class="`fa ${isRefreshing ? 'fa-refresh fa-spin' : 'fa-refresh'} mr-1`"></i>
          <span>{{ isRefreshing ? '刷新中...' : '刷新' }}</span>
        </button>
        <button
          @click="isExportModalOpen = true"
          class="bg-primary text-white rounded-lg px-3 py-2 text-sm btn-effect flex items-center"
        >
          <i class="fa fa-download mr-1"></i>
          <span>导出报告</span>
        </button>
      </div>
    </div>

    <!-- 系统健康状态 -->
    <div class="bg-white rounded-xl p-6 shadow-sm card-hover">
      <h3 class="font-bold mb-4">系统健康状态</h3>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="flex items-center p-4 border rounded-lg">
          <span :class="`health-status-indicator health-status-${getHealthStatus(healthStatus.dbDelay || 0, 'delay')}`"></span>
          <div>
            <p class="font-medium">数据库服务</p>
            <p class="text-sm text-gray-500">延迟: {{ healthStatus.dbDelay || 0 }}ms</p>
          </div>
        </div>
        <div class="flex items-center p-4 border rounded-lg">
          <span :class="`health-status-indicator health-status-${getHealthStatus(healthStatus.cacheDelay || 0, 'delay')}`"></span>
          <div>
            <p class="font-medium">缓存服务</p>
            <p class="text-sm text-gray-500">延迟: {{ healthStatus.cacheDelay || 0 }}ms</p>
          </div>
        </div>
        <div class="flex items-center p-4 border rounded-lg">
          <span :class="`health-status-indicator health-status-${getHealthStatus(healthStatus.llmDelay || 0, 'delay')}`"></span>
          <div>
            <p class="font-medium">大模型服务</p>
            <p class="text-sm text-gray-500">延迟: {{ healthStatus.llmDelay || 0 }}ms</p>
          </div>
        </div>
        <div class="flex items-center p-4 border rounded-lg">
          <span :class="`health-status-indicator health-status-${getHealthStatus(healthStatus.storageUsage || 0, 'usage')}`"></span>
          <div>
            <p class="font-medium">存储服务</p>
            <p class="text-sm text-gray-500">使用率: {{ healthStatus.storageUsage || 0 }}%</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6">
      <div 
        v-if="props.setActivePage"
        class="cursor-pointer transition-transform hover:scale-105"
        @click="handleCardClick('user-management')"
      >
        <StatCard
          icon="fa-user-circle-o"
          title="总用户数"
          :value="stats.totalUsers.toString()"
          :change="{ value: '', type: 'neutral' }"
          color="primary"
        />
      </div>
      <StatCard
        v-else
        icon="fa-user-circle-o"
        title="总用户数"
        :value="stats.totalUsers.toString()"
        :change="{ value: '', type: 'neutral' }"
        color="primary"
      />
      <StatCard
        icon="fa-plug"
        title="数据源"
        :value="stats.totalDataSources.toString()"
        :change="{ value: '', type: 'neutral' }"
        color="secondary"
      />
      <StatCard
        icon="fa-search"
        title="今日查询"
        :value="stats.todayQueries.toString()"
        :change="{ value: '', type: 'neutral' }"
        color="success"
      />
      <div 
        v-if="props.setActivePage"
        class="cursor-pointer transition-transform hover:scale-105"
        @click="handleCardClick('llm-config')"
      >
        <StatCard
          icon="fa-bolt"
          title="今日Token消耗"
          :value="formatTokenUsage(stats.todayTokenUsage)"
          :change="{ value: '', type: 'neutral' }"
          color="warning"
        />
      </div>
      <StatCard
        v-else
        icon="fa-bolt"
        title="今日Token消耗"
        :value="formatTokenUsage(stats.todayTokenUsage)"
        :change="{ value: '', type: 'neutral' }"
        color="warning"
      />
      <div class="cursor-pointer transition-transform hover:scale-105" @click="handleViewAbnormalLogs">
        <StatCard
          icon="fa-exclamation-triangle"
          title="异常日志"
          :value="stats.errorLogs.toString()"
          :change="{ value: '', type: 'neutral' }"
          color="danger"
        />
      </div>
    </div>

    <!-- 性能趋势 -->
    <div class="bg-white rounded-xl p-6 shadow-sm card-hover">
      <h3 class="font-bold mb-4">性能趋势</h3>
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div>
          <h4 class="font-semibold text-sm text-center mb-2">过去24小时查询量</h4>
          <div class="h-64">
            <Line :data="queryVolumeData" :options="commonLineChartOptions" />
          </div>
        </div>
        <div>
          <h4 class="font-semibold text-sm text-center mb-2">平均查询响应时间 (近7日)</h4>
          <div class="h-64">
            <Line :data="responseTimeData" :options="commonLineChartOptions" />
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 错误分类 -->
      <div class="bg-white rounded-xl p-6 shadow-sm card-hover">
        <h3 class="font-bold mb-4">关键错误分类</h3>
        <div class="h-64 relative">
          <Doughnut :data="errorChartData" :options="doughnutChartOptions" />
        </div>
      </div>

      <!-- 模型成本 -->
      <div class="bg-white rounded-xl p-6 shadow-sm card-hover">
        <h3 class="font-bold mb-4">模型成本预估</h3>
        <div class="h-64 relative">
          <Bar :data="costChartData" :options="barChartOptions" />
        </div>
      </div>
    </div>

    <!-- 导出报告模态框 -->
    <AdminModal
      :is-open="isExportModalOpen"
      title="导出系统报告"
      @close="isExportModalOpen = false"
    >
      <form @submit.prevent="handleExport" class="space-y-4">
        <div>
          <label class="block text-gray-700 text-sm mb-2">报告类型</label>
          <select
            v-model="exportForm.reportType"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg"
          >
            <option value="system-overview">系统概览报告</option>
            <option value="user-statistics">用户统计报告</option>
          </select>
        </div>
        <div>
          <label class="block text-gray-700 text-sm mb-2">时间范围</label>
          <div class="grid grid-cols-2 gap-2">
            <input v-model="exportForm.startDate" type="date" class="px-3 py-2 border rounded-lg" />
            <input v-model="exportForm.endDate" type="date" class="px-3 py-2 border rounded-lg" />
          </div>
        </div>
        <div>
          <label class="block text-gray-700 text-sm mb-2">导出格式</label>
          <div class="flex space-x-4">
            <label class="flex items-center">
              <input v-model="exportForm.exportFormat" type="radio" value="csv" class="mr-2" />
              <span>CSV</span>
            </label>
            <label class="flex items-center">
              <input v-model="exportForm.exportFormat" type="radio" value="json" class="mr-2" />
              <span>JSON</span>
            </label>
          </div>
        </div>
        <div class="pt-4 flex justify-end space-x-2">
          <button
            type="button"
            @click="isExportModalOpen = false"
            class="px-4 py-2 border rounded-lg"
          >
            取消
          </button>
          <button type="submit" class="px-4 py-2 bg-primary text-white rounded-lg">导出</button>
        </div>
      </form>
    </AdminModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import StatCard from '../../components/admin/StatCard.vue'
import { dashboardApi, systemHealthApi } from '../../services/api'
import type { DashboardStats, SystemHealth } from '../../services/api'
import type { SysAdminPageType } from '../../types'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  Title,
  type ChartOptions,
} from 'chart.js'
import { Line, Bar, Doughnut } from 'vue-chartjs'

// 注册 Chart.js 组件
ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  Title,
)

// Props
interface DashboardPageProps {
  setActivePage?: (page: SysAdminPageType) => void
  onViewAbnormalLogs: () => void
}

const props = defineProps<DashboardPageProps>()

// 响应式数据
const isExportModalOpen = ref(false)
const isRefreshing = ref(false)
const loading = ref(true)
const exportForm = ref({
  reportType: 'system-overview',
  startDate: '',
  endDate: '',
  exportFormat: 'csv',
})

// 统计数据
const stats = ref({
  totalUsers: 0,
  totalDataSources: 0,
  todayQueries: 0,
  todayTokenUsage: 0,
  errorLogs: 0,
})

// 系统健康状态
const healthStatus = ref<SystemHealth>({
  dbDelay: 0,
  cacheDelay: 0,
  llmDelay: 0,
  storageUsage: 0,
})

// 图表数据
const queryVolumeData = ref({
  labels: Array.from({ length: 24 }, (_, i) => `${i.toString().padStart(2, '0')}:00`),
  datasets: [
    {
      label: '查询量',
      data: Array(24).fill(0),
      borderColor: '#165DFF',
      backgroundColor: 'rgba(22, 93, 255, 0.1)',
      fill: true,
      tension: 0.4,
      pointRadius: 0,
    },
  ],
})

const responseTimeData = ref({
  labels: ['7天前', '6天前', '5天前', '4天前', '3天前', '昨天', '今天'],
  datasets: [
    {
      label: '平均响应时间 (ms)',
      data: Array(7).fill(0),
      borderColor: '#00B42A',
      backgroundColor: 'rgba(0, 180, 42, 0.1)',
      fill: true,
      tension: 0.4,
      pointRadius: 0,
    },
  ],
})

const errorChartData = ref({
  labels: ['模型调用超时', '数据库连接错误', '用户认证失败', '其他'],
  datasets: [
    {
      data: [0, 0, 0, 0],
      backgroundColor: ['#FF7D00', '#F53F3F', '#FADC19', '#86909C'],
      borderWidth: 0,
    },
  ],
})

const costChartData = ref({
  labels: ['gemini-2.5-pro', 'GPT-4', 'GLM-4.6', 'qwen3-max'],
  datasets: [
    {
      label: '今日预估成本 (USD)',
      data: [0, 0, 0, 0],
      backgroundColor: ['#165DFF', '#00B42A', '#86909C', '#36BFFA'],
      borderRadius: 4,
    },
  ],
})

// 图表选项
const commonLineChartOptions: ChartOptions<'line'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
  },
  scales: {
    y: { beginAtZero: false },
    x: { grid: { display: false } },
  },
}

const doughnutChartOptions: ChartOptions<'doughnut'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'right' },
  },
}

const barChartOptions: ChartOptions<'bar'> = {
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y',
  plugins: {
    legend: { display: false },
  },
  scales: {
    x: { grid: { display: false } },
  },
}

// 格式化Token使用量
const formatTokenUsage = (tokens: number): string => {
  if (tokens >= 1000000) {
    return (tokens / 1000000).toFixed(1) + 'M'
  } else if (tokens >= 1000) {
    return (tokens / 1000).toFixed(1) + 'K'
  }
  return tokens.toString()
}

// 加载统计数据
const loadStats = async () => {
  try {
    loading.value = true
    const data = await dashboardApi.getSysAdminStats()
    
    // 更新统计数据
    stats.value = {
      totalUsers: data.totalUsers || 0,
      totalDataSources: data.totalDataSources || 0,
      todayQueries: data.todayQueries || 0,
      todayTokenUsage: data.todayTokenUsage || 0,
      errorLogs: data.errorLogs || 0,
    }
    
    // 更新查询量图表数据
    if (data.queryVolumeData && data.queryVolumeData.length > 0) {
      queryVolumeData.value = {
        labels: data.queryVolumeData.map((item: any) => item.label),
        datasets: [
          {
            label: '查询量',
            data: data.queryVolumeData.map((item: any) => item.value),
            borderColor: '#165DFF',
            backgroundColor: 'rgba(22, 93, 255, 0.1)',
            fill: true,
            tension: 0.4,
            pointRadius: 0,
          },
        ],
      }
    }
    
    // 更新响应时间图表数据
    if (data.responseTimeData && data.responseTimeData.length > 0) {
      responseTimeData.value = {
        labels: data.responseTimeData.map((item: any) => item.label),
        datasets: [
          {
            label: '平均响应时间 (ms)',
            data: data.responseTimeData.map((item: any) => item.value),
            borderColor: '#00B42A',
            backgroundColor: 'rgba(0, 180, 42, 0.1)',
            fill: true,
            tension: 0.4,
            pointRadius: 0,
          },
        ],
      }
    }
    
    // 更新错误图表数据
    if (data.errorChartData && data.errorChartData.length > 0) {
      errorChartData.value = {
        labels: data.errorChartData.map((item: any) => item.label),
        datasets: [
          {
            data: data.errorChartData.map((item: any) => item.value),
            backgroundColor: ['#FF7D00', '#F53F3F', '#FADC19', '#86909C'],
            borderWidth: 0,
          },
        ],
      }
    }
    
    // 更新成本分布图表
    if (data.costChartData && data.costChartData.length > 0) {
      const costLabels = data.costChartData.map((item: any) => item.label)
      const costValues = data.costChartData.map((item: any) => item.value)
      
      // 动态生成颜色数组（根据模型数量）
      const colors = ['#165DFF', '#00B42A', '#86909C', '#36BFFA', '#FF7D00', '#F53F3F', '#FADC19', '#722ED1']
      const backgroundColor = costLabels.map((_: any, index: number) => colors[index % colors.length])
      
      costChartData.value = {
        labels: costLabels,
        datasets: [
          {
            label: '今日预估成本 (USD)',
            data: costValues,
            backgroundColor: backgroundColor,
            borderRadius: 4,
          },
        ],
      }
    } else {
      // 如果没有数据，显示空状态
      costChartData.value = {
        labels: [],
        datasets: [
          {
            label: '今日预估成本 (USD)',
            data: [],
            backgroundColor: [],
            borderRadius: 4,
          },
        ],
      }
    }
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
    showToast('加载仪表盘数据失败', 'error')
  } finally {
    loading.value = false
  }
}

// 方法
const handleRefresh = async () => {
  isRefreshing.value = true
  await Promise.all([loadStats(), loadHealthStatus()])
  isRefreshing.value = false
  showToast('仪表盘数据已刷新', 'success')
}

// 组件挂载时加载数据
onMounted(() => {
  loadStats()
  loadHealthStatus()
})

// 加载系统健康状态
const loadHealthStatus = async () => {
  try {
    const data = await systemHealthApi.getLatest()
    if (data) {
      healthStatus.value = {
        dbDelay: data.dbDelay || 0,
        cacheDelay: data.cacheDelay || 0,
        llmDelay: data.llmDelay || 0,
        storageUsage: data.storageUsage || 0,
      }
    }
  } catch (error) {
    console.error('加载健康状态失败:', error)
    // 保持默认值
  }
}

// 计算健康状态等级（用于显示不同颜色）
const getHealthStatus = (value: number, type: 'delay' | 'usage') => {
  if (type === 'delay') {
    // 延迟类型：0-100ms 正常，100-300ms 警告，>300ms 危险
    if (value <= 100) return 'healthy'
    if (value <= 300) return 'warning'
    return 'danger'
  } else {
    // 使用率类型：0-70% 正常，70-90% 警告，>90% 危险
    if (value <= 70) return 'healthy'
    if (value <= 90) return 'warning'
    return 'danger'
  }
}

const handleExport = () => {
  try {
    // 生成报告内容
    let content = ''
    const reportType = exportForm.value.reportType
    const format = exportForm.value.exportFormat
    
    if (format === 'json') {
      const reportData = {
        reportType: reportType === 'system-overview' ? '系统概览报告' : '用户统计报告',
        generateTime: new Date().toISOString(),
        timeRange: {
          startDate: exportForm.value.startDate || '全部',
          endDate: exportForm.value.endDate || '全部',
        },
        statistics: {
          totalUsers: stats.value.totalUsers,
          totalDataSources: stats.value.totalDataSources,
          todayQueries: stats.value.todayQueries,
          todayTokenUsage: stats.value.todayTokenUsage,
          errorLogs: stats.value.errorLogs,
        },
      }
      content = JSON.stringify(reportData, null, 2)
    } else {
      // CSV格式
      const headers = ['指标', '数值']
      const rows = [
        ['报告类型', reportType === 'system-overview' ? '系统概览报告' : '用户统计报告'],
        ['生成时间', new Date().toLocaleString('zh-CN')],
        ['时间范围', `${exportForm.value.startDate || '全部'} 至 ${exportForm.value.endDate || '全部'}`],
        ['总用户数', stats.value.totalUsers.toString()],
        ['数据源总数', stats.value.totalDataSources.toString()],
        ['今日查询数', stats.value.todayQueries.toString()],
        ['今日Token消耗', stats.value.todayTokenUsage.toString()],
        ['异常日志数', stats.value.errorLogs.toString()],
      ]
      content = [headers, ...rows].map(row => row.join(',')).join('\n')
    }
    
    // 创建并下载文件
    const blob = new Blob([content], { 
      type: format === 'json' ? 'application/json' : 'text/csv;charset=utf-8;' 
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `report-${reportType}-${new Date().toISOString().split('T')[0]}.${format}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    showToast('报告导出成功', 'success')
  } catch (error) {
    console.error('导出报告失败:', error)
    showToast('导出报告失败', 'error')
  }
  
  isExportModalOpen.value = false
}

// 处理卡片点击跳转
const handleCardClick = (page: SysAdminPageType) => {
  if (props.setActivePage) {
    props.setActivePage(page)
  }
}

const handleViewAbnormalLogs = () => {
  props.onViewAbnormalLogs()
}

// Toast 提示函数
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  const toast = document.createElement('div')
  const colors = {
    success: 'bg-green-500 text-white',
    error: 'bg-red-500 text-white',
  }
  toast.className = `fixed top-5 right-5 px-6 py-3 rounded-lg shadow-lg z-50 transition-all duration-300 transform translate-x-full ${colors[type]}`
  toast.textContent = message
  document.body.appendChild(toast)

  setTimeout(() => {
    toast.classList.remove('translate-x-full')
    toast.classList.add('translate-x-0')
  }, 10)

  setTimeout(() => {
    toast.classList.remove('translate-x-0')
    toast.classList.add('translate-x-full')
    setTimeout(() => {
      toast.remove()
    }, 300)
  }, 3000)
}
</script>

<style scoped>
.btn-effect:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-hover:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.health-status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.health-status-healthy {
  background-color: #00b42a;
  box-shadow: 0 0 0 3px rgba(0, 180, 42, 0.1);
}

.health-status-warning {
  background-color: #fadc19;
  box-shadow: 0 0 0 3px rgba(250, 220, 25, 0.1);
}

.health-status-danger {
  background-color: #f53f3f;
  box-shadow: 0 0 0 3px rgba(245, 63, 63, 0.1);
}
</style>
