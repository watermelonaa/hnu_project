<!--
  @file components/feature/query/ComparisonModal.vue
  @description 查询对比模态框

  功能：
  - 新旧查询结果对比
  - 表格和图表视图切换
  - 差异分析展示

  @author Frontend Team
-->
<template>
  <Modal
    :is-open="isOpen"
    @close="onClose"
    title="查询快照对比"
    content-class="max-w-6xl max-h-[90vh] min-h-[60vh]"
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

        <!-- 表格对比组件 -->
        <div v-if="activeView === 'table'">
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <!-- 旧数据表格 -->
            <div>
              <h3 class="font-bold mb-2">旧数据 ({{ formatDate(oldQuery.queryTime) }})</h3>
              <div class="overflow-x-auto border rounded-lg max-h-[40vh]">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-b bg-gray-50">
                      <th v-for="header in oldQuery.tableData.headers" :key="header" class="p-2">
                        {{ header }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <!-- 显示删除的行 -->
                    <tr v-for="(row, i) in changes.deleted" :key="`del-${i}`" class="bg-red-100">
                      <td :colspan="row.length" class="p-2 text-center text-red-700 line-through">
                        {{ row.join(' | ') }}
                      </td>
                    </tr>

                    <!-- 显示修改的旧行 -->
                    <tr
                      v-for="({ oldRow }, i) in changes.modified"
                      :key="`mod-old-${i}`"
                      class="border-b"
                    >
                      <td v-for="(cell, j) in oldRow" :key="j" class="p-2 text-center">
                        {{ cell }}
                      </td>
                    </tr>

                    <!-- 显示未变化的共同行 -->
                    <tr v-for="(row, i) in changes.common" :key="`com-${i}`" class="border-b">
                      <td v-for="(cell, j) in row" :key="j" class="p-2 text-center">
                        {{ cell }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- 新数据表格 -->
            <div>
              <h3 class="font-bold mb-2">新数据 ({{ formatDate(newQuery.queryTime) }})</h3>
              <div class="overflow-x-auto border rounded-lg max-h-[40vh]">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-b bg-gray-50">
                      <th v-for="header in newQuery.tableData.headers" :key="header" class="p-2">
                        {{ header }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <!-- 显示新增的行 -->
                    <tr v-for="(row, i) in changes.added" :key="`add-${i}`" class="bg-green-100">
                      <td v-for="(cell, j) in row" :key="j" class="p-2 text-center text-green-700">
                        {{ cell }}
                      </td>
                    </tr>

                    <!-- 显示修改的新行 -->
                    <tr
                      v-for="({ oldRow, newRow }, i) in changes.modified"
                      :key="`mod-new-${i}`"
                      class="border-b"
                    >
                      <td v-for="(cell, j) in newRow" :key="j" class="p-2 text-center">
                        <span
                          v-if="oldRow[j] !== cell"
                          class="bg-yellow-100 text-yellow-800 p-1 rounded"
                        >
                          {{ cell }}
                          <span class="text-xs text-gray-500 line-through ml-1">
                            {{ oldRow[j] }}
                          </span>
                        </span>
                        <span v-else>
                          {{ cell }}
                        </span>
                      </td>
                    </tr>

                    <!-- 显示未变化的共同行 -->
                    <tr v-for="(row, i) in changes.common" :key="`com-${i}`" class="border-b">
                      <td v-for="(cell, j) in row" :key="j" class="p-2 text-center">
                        {{ cell }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <!-- 图表对比组件 -->
        <div v-else class="bg-white p-4 rounded-lg border">
          <div class="chart-container h-64">
            <Bar
              v-if="newQuery.chartData.type === 'bar'"
              :data="chartData"
              :options="chartOptions as any"
            />
            <Line
              v-else-if="newQuery.chartData.type === 'line'"
              :data="chartData"
              :options="chartOptions as any"
            />
            <Pie
              v-else-if="newQuery.chartData.type === 'pie'"
              :data="chartData"
              :options="chartOptions as any"
            />
            <Doughnut
              v-else-if="newQuery.chartData.type === 'doughnut'"
              :data="chartData"
              :options="chartOptions as any"
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
import { Bar, Line, Pie, Doughnut } from 'vue-chartjs'
import Modal from '../../ui/Modal.vue'
import type { QueryResultData } from '../../../types'
import type { ChartOptions } from 'chart.js'

ChartJS.register(...registerables)

interface Props {
  oldQuery: QueryResultData
  newQuery: QueryResultData
  isOpen: boolean
  onClose: () => void
}

const props = defineProps<Props>()

// 视图切换状态
const activeView = ref<'table' | 'chart'>('table')

// 计算表格行差异
const changes = computed(() =>
  findRowChanges(props.oldQuery.tableData.rows, props.newQuery.tableData.rows),
)

// 对比摘要统计项
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

// 构建图表数据
const chartData = computed(() => {
  const oldLabels = props.oldQuery.chartData?.labels || []
  const newLabels = props.newQuery.chartData?.labels || []
  const labels = [...new Set([...oldLabels, ...newLabels])]

  const oldDataset = props.oldQuery.chartData?.datasets?.[0] || { label: '旧数据', data: [] }
  const newDataset = props.newQuery.chartData?.datasets?.[0] || { label: '新数据', data: [] }

  return {
    labels,
    datasets: [
      {
        ...oldDataset,
        label: `${oldDataset.label} (旧)`,
        backgroundColor: 'rgba(22, 93, 255, 0.3)',
        borderColor: 'rgba(22, 93, 255, 0.5)',
        borderDash: [5, 5],
      },
      {
        ...newDataset,
        label: `${newDataset.label} (新)`,
        backgroundColor: 'rgba(54, 162, 235, 0.6)',
        borderColor: 'rgba(54, 162, 235, 1)',
      },
    ],
  }
})

// 图表配置
const chartOptions: ChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top' as const,
    },
  },
  scales:
    props.newQuery.chartData?.type === 'bar'
      ? {
          x: {
            grid: {
              display: false,
            },
          },
          y: {
            beginAtZero: true,
          },
        }
      : undefined,
}

// 查找行差异的辅助函数
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

// 格式化日期
const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleString()
}

// 类型定义
type DiffResult = {
  added: string[][]
  deleted: string[][]
  modified: { oldRow: string[]; newRow: string[] }[]
  common: string[][]
}
</script>

<style scoped>
.chart-container {
  position: relative;
  width: 100%;
  height: 16rem; /* h-64 */
}
</style>
