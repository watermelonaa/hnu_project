<!--
  @file views/data-admin/DataAdminDashboardPage.vue
  @description 数据管理员仪表盘页面

  功能：
  - 数据源统计概览
  - 连接状态分布图表
  - 快捷操作入口

  @author Frontend Team
-->
<template>
  <main class="flex-1 overflow-y-auto p-6 space-y-6 bg-neutral">
    <!-- 统计卡片区域 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <StatCard
        title="数据源总数"
        :value="datasourceCount.toString()"
        icon="fa-database"

        :change="{ value: '', type: 'neutral' }"
        color="primary"
        @click="handleCardClick('datasource')"
      />
      <StatCard
        title="当前连接数"
        :value="connectedCount.toString()"
        icon="fa-check-circle"
        :change="{ value: '稳定', type: 'neutral' }"
        color="success"
        @click="handleCardClick('datasource')"
      />
      <StatCard
        title="连接错误数"
        :value="Math.max(0, errorCount).toString()"
        icon="fa-exclamation-triangle"
        :change="{ value: errorCount > 0 ? '需处理' : '正常', type: errorCount > 0 ? 'warning' : 'neutral' }"
        color="danger"
        @click="handleCardClick('connection-log')"
      />
      <StatCard
        title="待处理权限请求"
        :value="(stats.pendingPermissions || 0).toString()"
        icon="fa-key"
        :change="{ value: '', type: 'neutral' }"
        color="warning"
        @click="handleCardClick('user-permission')"
      />
    </div>

    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-5 gap-6">
      <!-- 数据源健康状态饼图 -->
      <div class="lg:col-span-2 bg-white rounded-xl p-6 shadow-sm">
        <h3 class="font-bold mb-4">数据源健康状态</h3>
        <div class="h-64 relative">
          <Pie :data="healthStatusData" :options="pieChartOptions" />
        </div>
      </div>

      <!-- 数据源查询量柱状图 -->
      <div class="lg:col-span-3 bg-white rounded-xl p-6 shadow-sm">
        <h3 class="font-bold mb-4">数据源查询量 Top 5</h3>
        <div class="h-64 relative">
          <Bar :data="queryLoadData" :options="barChartOptions" />
        </div>
      </div>
    </div>

    <!-- 日志和动态区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 近期连接失败日志 -->
      <div class="bg-white rounded-xl p-6 shadow-sm">
        <h3 class="font-bold mb-4">近期连接失败日志</h3>
        <div class="space-y-3">
          <template v-if="recentFailures.length > 0">
            <div
              v-for="log in recentFailures"
              :key="log.id"
              class="flex justify-between items-center text-sm p-2 rounded-lg hover:bg-gray-50"
            >
              <div>
                <p class="font-medium text-danger">{{ log.datasource }}: {{ log.details }}</p>
                <p class="text-xs text-gray-500">{{ log.time }}</p>
              </div>
              <button
                @click="handleCardClick('connection-log')"
                class="text-primary text-xs hover:underline"
              >
                查看详情
              </button>
            </div>
          </template>
          <p v-else class="text-sm text-gray-500 text-center py-4">无失败记录</p>
        </div>
      </div>

      <!-- 近期权限变更动态 -->
      <div class="bg-white rounded-xl p-6 shadow-sm">
        <h3 class="font-bold mb-4">近期权限变更动态</h3>
        <div class="space-y-4">
          <template v-if="recentPermissionLogs.length > 0">
            <div
              v-for="log in recentPermissionLogs"
              :key="log.id"
              class="flex items-start space-x-3 text-sm"
            >
              <i class="fa fa-user-secret text-gray-400 mt-1"></i>
              <div class="flex-1">
                <p v-html="log.text"></p>
                <p class="text-xs text-gray-400">{{ formatTimeAgo(log.timestamp) }}</p>
              </div>
            </div>
          </template>
          <p v-else class="text-sm text-gray-500 text-center py-4">暂无权限变更记录</p>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Pie, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  type ChartOptions,
} from 'chart.js'
import StatCard from '../../components/admin/StatCard.vue'
import { dashboardApi } from '../../services/api'
import type { DashboardStats, DataAdminPageType } from '../../types'

// 注册 Chart.js 组件
ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement)

interface Props {
  setActivePage: (page: DataAdminPageType) => void
}

const props = defineProps<Props>()

// 工具函数：格式化时间
const formatTimeAgo = (timestamp: string): string => {
  const now = new Date()
  const then = new Date(timestamp)
  const diffInSeconds = Math.round((now.getTime() - then.getTime()) / 1000)

  if (diffInSeconds < 60) return `${diffInSeconds}秒前`
  const diffInMinutes = Math.round(diffInSeconds / 60)
  if (diffInMinutes < 60) return `${diffInMinutes}分钟前`
  const diffInHours = Math.round(diffInMinutes / 60)
  if (diffInHours < 24) return `${diffInHours}小时前`
  const diffInDays = Math.round(diffInHours / 24)
  return `${diffInDays}天前`
}

// 响应式数据
const loading = ref(true)
const stats = ref<DashboardStats>({
  datasourceCount: 0,
  connectedCount: 0,
  errorCount: 0,
  pendingPermissions: 0,
})

// 加载统计数据
const loadStats = async () => {
  try {
    loading.value = true
    const data = await dashboardApi.getDataAdminStats()
    
    // 调试日志
    console.log('仪表盘数据:', data)
    console.log('pendingPermissions:', data.pendingPermissions)
    
    // 确保所有数值都是有效的非负数
    const safeNumber = (val: number | null | undefined): number => {
      if (val == null || isNaN(val) || val < 0) return 0
      return Number(val)
    }
    
    stats.value = {
      datasourceCount: safeNumber(data.datasourceCount),
      connectedCount: safeNumber(data.connectedCount),
      errorCount: safeNumber(data.errorCount),
      pendingPermissions: safeNumber(data.pendingPermissions),
      healthStatusData: data.healthStatusData,
      queryLoadData: data.queryLoadData,
      recentFailures: data.recentFailures,
      recentPermissionLogs: data.recentPermissionLogs || [],
    }
    
    console.log('stats.pendingPermissions:', stats.value.pendingPermissions)
  } catch (error) {
    console.error('加载统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadStats()
})

// 计算属性
const datasourceCount = computed(() => {
  const val = stats.value.datasourceCount
  return val != null && !isNaN(val) && val >= 0 ? Number(val) : 0
})

const connectedCount = computed(() => {
  const val = stats.value.connectedCount
  return val != null && !isNaN(val) && val >= 0 ? Number(val) : 0
})

const errorCount = computed(() => {
  const val = stats.value.errorCount
  return val != null && !isNaN(val) && val >= 0 ? Number(val) : 0
})

const recentFailures = computed(() => stats.value.recentFailures || [])

const recentPermissionLogs = computed(() => stats.value.recentPermissionLogs || [])

// 饼图数据
const healthStatusData = computed(() => {
  const data = stats.value.healthStatusData || []
  return {
    labels: data.map(d => d.label),
    datasets: [
      {
        data: data.map(d => d.value),
        backgroundColor: ['#00B42A', '#86909C', '#F53F3F', '#36BFFA', '#C9CDD4'],
        borderWidth: 0,
      },
    ],
  }
})

// 柱状图数据
const queryLoadData = computed(() => {
  const data = stats.value.queryLoadData || []
  return {
    labels: data.map(d => d.label),
    datasets: [
      {
        label: '查询量',
        data: data.map(d => d.value),
        backgroundColor: '#165DFF',
        borderRadius: 4,
      },
    ],
  }
})

// 图表配置
const pieChartOptions: ChartOptions<'pie'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'right' as const,
    },
  },
}

const barChartOptions: ChartOptions<'bar'> = {
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y' as const,
  plugins: {
    legend: {
      display: false,
    },
  },
  scales: {
    x: {
      grid: {
        display: false,
      },
    },
  },
}

// 事件处理
const handleCardClick = (page: DataAdminPageType) => {
  props.setActivePage(page)
}
</script>

<style scoped>
.card-hover {
  cursor: pointer;
  transition: all 0.2s ease;
}

.card-hover:hover {
  transform: translateY(-1px);
  box-shadow:
    0 10px 25px -5px rgba(0, 0, 0, 0.1),
    0 10px 10px -5px rgba(0, 0, 0, 0.04);
}
</style>
