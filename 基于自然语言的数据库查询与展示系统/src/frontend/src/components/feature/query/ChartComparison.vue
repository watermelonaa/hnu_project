<!--
  @file components/feature/query/ChartComparison.vue
  @description 图表对比组件

  功能：
  - 新旧查询图表并排对比
  - 支持多种图表类型
  - 模态框展示

  @author Frontend Team
-->
<template>
  <Modal
    :isOpen="isOpen"
    @close="onClose"
    title="查询快照对比"
    :contentClass="'max-w-3xl max-h-[90vh] min-h-[60vh]'"
  >
    <!-- 弹窗内容区域 -->
    <div class="p-4 space-y-6 max-h-[calc(100%-2rem)] overflow-y-auto">
      <!-- 对比说明 -->
      <div>
        <p class="text-gray-600 text-sm">对比查询 "{{ oldQuery.userPrompt }}" 的两个不同版本。</p>
      </div>

      <!-- 对比摘要卡片 -->
      <div class="bg-white p-4 rounded-xl shadow-sm border">
        <h3 class="font-bold mb-3">对比摘要</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div
            v-for="item in summaryItems"
            :key="item.label"
            class="flex items-center p-3 bg-gray-50 rounded-lg"
          >
            <i :class="`fa ${item.icon} ${item.color} text-2xl mr-3`"></i>
            <div>
              <p class="text-xl font-bold">{{ item.value }}</p>
              <p class="text-sm text-gray-500">{{ item.label }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 对比视图切换区域 -->
      <div class="bg-white p-4 rounded-xl shadow-sm border">
        <div class="border-b mb-3">
          <div class="flex space-x-6">
            <button
              @click="activeView = 'table'"
              :class="`py-2 text-sm ${activeView === 'table' ? 'border-b-2 border-primary text-primary' : 'text-gray-500'}`"
            >
              表格对比
            </button>
            <button
              @click="activeView = 'chart'"
              :class="`py-2 text-sm ${activeView === 'chart' ? 'border-b-2 border-primary text-primary' : 'text-gray-500'}`"
            >
              图表对比
            </button>
          </div>
        </div>

        <!-- 根据当前视图状态渲染对应的对比组件 -->
        <TableComparison
          v-if="activeView === 'table'"
          :old-query="oldQuery"
          :new-query="newQuery"
          :changes="changes"
        />
        <!-- 直接在这里实现图表对比视图 -->
        <div v-else class="bg-white p-4 rounded-lg border">
          <div class="chart-container h-64">
            <Chart
              :type="newQuery.chartData.type"
              :data="comparisonChartData"
              :options="{ responsive: true, maintainAspectRatio: false }"
            />
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Chart as ChartJS, registerables } from 'chart.js'
import { Chart } from 'vue-chartjs'
import type { QueryResultData } from '../../../types'
import Modal from '../../ui/Modal.vue'
import TableComparison from './TableComparison.vue'

// 注册 ChartJS
ChartJS.register(...registerables)

// 定义 Props 接口
interface ComparisonModalProps {
  oldQuery: QueryResultData
  newQuery: QueryResultData
  isOpen: boolean
  onClose: () => void
}

// 定义 DiffResult 类型
interface DiffResult {
  added: string[][]
  deleted: string[][]
  modified: { oldRow: string[]; newRow: string[] }[]
  common: string[][]
}

// 定义 Props
const props = defineProps<ComparisonModalProps>()

// 响应式状态
const activeView = ref<'table' | 'chart'>('table')

// 计算属性：差异结果
const changes = computed<DiffResult>(() => {
  return findRowChanges(props.oldQuery.tableData.rows, props.newQuery.tableData.rows)
})

// 计算属性：摘要项目
const summaryItems = computed(() => [
  {
    icon: 'fa-plus-circle',
    color: 'text-green-500',
    value: changes.value.added.length,
    label: '新增行',
  },
  {
    icon: 'fa-minus-circle',
    color: 'text-red-500',
    value: changes.value.deleted.length,
    label: '删除行',
  },
  {
    icon: 'fa-pencil-square-o',
    color: 'text-yellow-500',
    value: changes.value.modified.length,
    label: '变更行',
  },
])

// 计算属性：图表对比数据
const comparisonChartData = computed(() => {
  const oldChart = props.oldQuery.chartData
  const newChart = props.newQuery.chartData

  // 合并标签并去重
  const allLabels = [...new Set([...oldChart.labels, ...newChart.labels])]

  return {
    labels: allLabels,
    datasets: [
      {
        ...oldChart.datasets[0],
        label: `${oldChart.datasets[0].label} (旧)`,
        backgroundColor: 'rgba(22, 93, 255, 0.3)',
        borderColor: 'rgba(22, 93, 255, 0.5)',
        borderDash: [5, 5],
      },
      {
        ...newChart.datasets[0],
        label: `${newChart.datasets[0].label} (新)`,
        backgroundColor: 'rgba(54, 162, 235, 0.6)',
        borderColor: 'rgba(54, 162, 235, 1)',
      },
    ],
  }
})

// 辅助函数：查找行差异
const findRowChanges = (oldRows: string[][], newRows: string[][]): DiffResult => {
  const oldMap = new Map(oldRows.map((row) => [row[0], row]))
  const newMap = new Map(newRows.map((row) => [row[0], row]))

  const result: DiffResult = { added: [], deleted: [], modified: [], common: [] }

  // 检查删除和修改的行
  oldMap.forEach((oldRow, key) => {
    if (!newMap.has(key)) {
      result.deleted.push(oldRow)
    } else {
      const newRow = newMap.get(key)!
      if (JSON.stringify(oldRow) !== JSON.stringify(newRow)) {
        result.modified.push({ oldRow, newRow })
      } else {
        result.common.push(oldRow)
      }
    }
  })

  // 检查新增的行
  newMap.forEach((newRow, key) => {
    if (!oldMap.has(key)) {
      result.added.push(newRow)
    }
  })

  return result
}
</script>

<style scoped>
.chart-container {
  position: relative;
  width: 100%;
}
</style>
