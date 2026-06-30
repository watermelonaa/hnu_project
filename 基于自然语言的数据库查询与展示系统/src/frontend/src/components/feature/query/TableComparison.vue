<!--
  @file components/feature/query/TableComparison.vue
  @description 表格对比组件

  功能：
  - 新旧查询表格并排对比
  - 差异高亮显示
  - 响应式布局

  @author Frontend Team
-->
<template>
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
            <tr v-for="({ oldRow }, i) in changes.modified" :key="`mod-old-${i}`" class="border-b">
              <td v-for="(cell, j) in oldRow" :key="j" class="p-2 text-center">{{ cell }}</td>
            </tr>
            <!-- 显示未变化的共同行 -->
            <tr v-for="(row, i) in changes.common" :key="`com-${i}`" class="border-b">
              <td v-for="(cell, j) in row" :key="j" class="p-2 text-center">{{ cell }}</td>
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
                <span v-if="oldRow[j] !== cell" class="bg-yellow-100 text-yellow-800 p-1 rounded">
                  {{ cell }}
                  <span class="text-xs text-gray-500 line-through ml-1">{{ oldRow[j] }}</span>
                </span>
                <span v-else>{{ cell }}</span>
              </td>
            </tr>
            <!-- 显示未变化的共同行 -->
            <tr v-for="(row, i) in changes.common" :key="`com-${i}`" class="border-b">
              <td v-for="(cell, j) in row" :key="j" class="p-2 text-center">{{ cell }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { QueryResultData } from '../../../types'

interface Props {
  oldQuery: QueryResultData
  newQuery: QueryResultData
  changes: {
    added: string[][]
    deleted: string[][]
    modified: { oldRow: string[]; newRow: string[] }[]
    common: string[][]
  }
}

defineProps<Props>()

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleString()
}
</script>
