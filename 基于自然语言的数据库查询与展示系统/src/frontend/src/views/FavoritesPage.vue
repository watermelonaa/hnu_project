<!--
  @file views/HistoryPage.vue
  @description 收藏夹页面

  功能：
  - 显示用户收藏夹和收藏记录
  - 支持查看、重新执行、删除
  - 支持收藏夹重命名和删除
  - 结果详情弹窗

  @author Frontend Team
-->
<template>
  <section class="p-4 md:p-6 space-y-4 md:space-y-6 overflow-y-auto h-full">
    <!-- 加载状态 -->
    <div
      v-if="loading"
      class="p-6 space-y-6 overflow-y-auto h-full flex items-center justify-center"
    >
      <div class="text-center">
        <div class="dot-flashing"></div>
        <p class="mt-4 text-gray-500">加载收藏夹中...</p>
      </div>
    </div>

    <!-- 主要内容 -->
    <div v-else>
      <!-- 搜索和筛选栏 -->
      <div class="bg-white p-4 rounded-xl shadow-sm border sticky top-0 z-10 mb-4 space-y-4">
        <!-- 第一行：搜索框和筛选条件 -->
        <div class="flex flex-wrap items-center gap-3">
          <!-- 搜索框 -->
          <div class="relative flex-1 min-w-[200px]">
            <i class="fa fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></i>
            <input
              type="text"
              placeholder="按查询内容搜索..."
              :value="searchTerm"
              @input="(e) => (searchTerm = (e.target as HTMLInputElement).value)"
              class="w-full pl-9 pr-3 py-1.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30"
            />
          </div>
          
          <!-- 大模型筛选 -->
          <div class="relative">
            <select
              :value="activeFilters.model"
              @change="(e) => handleFilterChange('model', (e.target as HTMLSelectElement).value)"
              class="px-3 py-1.5 text-sm rounded-md border border-gray-300 bg-white font-bold focus:ring-2 focus:ring-primary/30 pr-6 appearance-none"
              :style="selectStyle"
            >
              <option v-for="option in modelOptions" :key="String(option.value)" :value="String(option.value)">
                {{ option.label }}
              </option>
            </select>
          </div>

          <!-- 数据库筛选 -->
          <div class="relative">
            <select
              :value="activeFilters.database"
              @change="
                (e) => handleFilterChange('database', (e.target as HTMLSelectElement).value)
              "
              class="px-3 py-1.5 text-sm rounded-md border border-gray-300 bg-white font-bold focus:ring-2 focus:ring-primary/30 pr-6 appearance-none"
              :style="selectStyle"
            >
              <option v-for="option in databaseOptions" :key="String(option.value)" :value="String(option.value)">
                {{ option.label }}
              </option>
            </select>
          </div>

          <!-- 日期筛选 -->
          <div class="relative">
            <select
              :value="activeFilters.date"
              @change="(e) => handleFilterChange('date', (e.target as HTMLSelectElement).value)"
              class="px-3 py-1.5 text-sm rounded-md border border-gray-300 bg-white font-bold focus:ring-2 focus:ring-primary/30 pr-6 appearance-none"
              :style="selectStyle"
            >
              <option v-for="option in dateOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>

          <!-- 重置按钮 -->
          <button
            @click="handleResetFilters"
            class="px-3 py-1.5 text-sm rounded-md border border-gray-300 hover:bg-gray-50 transition-colors whitespace-nowrap"
          >
            <i class="fa fa-refresh mr-1"></i> 重置
          </button>
        </div>

        <!-- 第二行：操作按钮（靠右对齐） -->
        <div class="flex flex-wrap items-center gap-3 justify-end">
          <!-- 刷新按钮 -->
          <button
            @click="loadAllData"
            class="px-3 py-1.5 text-sm rounded-md border border-gray-300 hover:bg-gray-50 transition-colors flex items-center gap-2"
            title="刷新收藏夹"
          >
            <i class="fa fa-refresh"></i>
            <span>刷新</span>
          </button>
          
          <!-- 新建收藏夹按钮 -->
          <button
            @click="handleOpenCreateCollectionModal"
            class="px-4 py-1.5 text-sm rounded-md bg-primary text-white hover:bg-primary/90 transition-colors flex items-center gap-2"
            title="新建收藏夹"
          >
            <i class="fa fa-plus"></i>
            <span>新建收藏夹</span>
          </button>

          <!-- 批量删除按钮 -->
          <button
            @click="openBulkDeleteConfirm"
            :disabled="selectedIds.size === 0"
            class="px-4 py-1.5 text-sm rounded-md transition-colors disabled:bg-gray-200 disabled:text-gray-400 disabled:cursor-not-allowed enabled:bg-red-600 enabled:text-white enabled:hover:bg-red-700 flex items-center gap-2"
          >
            <i class="fa fa-trash-o"></i>
            <span>批量删除</span>
          </button>
        </div>
      </div>

      <!-- 查询列表 -->
      <div class="space-y-4">
        <div
          v-if="filteredGroups.length === 0"
          class="text-center text-gray-500 py-16 bg-white rounded-xl shadow-sm"
        >
          <i class="fa fa-search-minus text-4xl mb-3 text-gray-400"></i>
          <p>未找到匹配的查询记录</p>
        </div>

        <div v-else>
          <div
            v-for="[prompt, snapshots] in filteredGroups"
            :key="prompt"
            class="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-200"
          >
            <!-- 分组头部 -->
            <div
              class="p-4 cursor-pointer hover:bg-gray-50 flex justify-between items-center bg-gradient-to-r from-white to-gray-50"
              @click="() => handleToggleGroup(prompt)"
            >
              <div class="flex-1">
                <div class="flex items-center space-x-2">
                  <i class="fa fa-folder text-primary"></i>
                  <p class="font-semibold text-dark truncate max-w-lg" :title="prompt">
                    {{ prompt }}
                  </p>
                </div>
                <p class="text-xs text-gray-500 mt-1 ml-6">{{ snapshots.length }} 条收藏</p>
              </div>
              <div class="flex items-center space-x-2">
                <template v-if="snapshots.length > 1 && expandedGroup === prompt">
                  <button
                    @click.stop="handleCompare"
                    :disabled="selectedIds.size !== 2"
                    class="px-3 py-1 bg-primary text-white rounded-md text-xs disabled:bg-primary/50 disabled:cursor-not-allowed"
                  >
                    对比差异 ({{ selectedIds.size }}/2)
                  </button>
                </template>
                <button
                  @click.stop="() => handleRenameGroup(prompt)"
                  class="px-3 py-1.5 bg-primary/10 text-primary rounded-lg text-xs hover:bg-primary/20 transition-all duration-200 flex items-center space-x-1.5 shadow-sm hover:shadow-md"
                  title="重命名分组"
                >
                  <i class="fa fa-edit text-xs"></i>
                  <span>重命名</span>
                </button>
                <button
                  @click.stop="() => handleDeleteGroup(prompt)"
                  class="px-3 py-1.5 bg-red-50 text-red-600 rounded-lg text-xs hover:bg-red-100 transition-all duration-200 flex items-center space-x-1.5 shadow-sm hover:shadow-md"
                  title="删除分组"
                >
                  <i class="fa fa-trash text-xs"></i>
                  <span>删除</span>
                </button>
                <button class="text-gray-500">
                  <i
                    :class="`fa fa-chevron-down transition-transform ${expandedGroup === prompt ? 'rotate-180' : ''}`"
                  ></i>
                </button>
              </div>
            </div>

            <!-- 分组内容 -->
            <div
              v-if="expandedGroup === prompt"
              class="p-4 border-t border-gray-200 bg-neutral"
            >
              <QuerySnapshotList
                :snapshots="snapshots"
                :selected-ids="selectedIds"
                :selectable="true"
                :show-conversation="true"
                :get-conversation-title="getConversationTitle"
                @select="handleSelectSnapshot"
                @view-detail="setViewingQuery"
                @view-in-chat="handleViewInChat"
                @rerun="openRerunConfirm"
                @delete="openDeleteConfirm"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 查看详情模态框 -->
    <Modal :is-open="!!viewingQuery" @close="setViewingQuery(null)" :title="viewingQuery ? `查看查询: ${viewingQuery.userPrompt}` : '查看查询'">
      <div v-if="viewingQuery" class="max-h-[70vh] overflow-y-auto -m-6 p-6 pt-0">
        <QueryResult
          :result="viewingQuery"
          :show-actions="{ save: false, share: true, export: true }"
        />
      </div>
      <div v-else class="p-6 text-center text-gray-500">
        <i class="fa fa-exclamation-circle text-4xl mb-4 text-gray-400"></i>
        <p>查询结果数据加载失败，请稍后重试</p>
      </div>
    </Modal>

    <!-- 确认操作模态框 -->
    <Modal
      :is-open="confirmModalState.isOpen"
      @close="handleCancelConfirm"
      :title="confirmModalContent.title"
    >
      <p class="text-gray-700 mb-6">{{ confirmModalContent.message }}</p>

      <div class="flex justify-end space-x-3">
        <button
          @click="handleCancelConfirm"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:shadow-md transition-all duration-200"
        >
          取消
        </button>
        <button
          @click="handleConfirmAction"
          :class="`px-4 py-2 text-white rounded-lg text-sm hover:shadow-md transition-all duration-200 ${confirmModalContent.buttonClass}`"
        >
          {{ confirmModalContent.buttonText }}
        </button>
      </div>
    </Modal>

    <!-- 对比差异模态框 -->
    <Modal
      :is-open="isCompareModalOpen"
      @close="() => (isCompareModalOpen = false)"
      title="对比查询结果差异"
      :content-class-name="'max-w-7xl overflow-hidden'"
    >
      <div v-if="compareQuery1 && compareQuery2" class="space-y-6 -m-6 p-6 max-h-[calc(90vh-80px)] overflow-y-auto">
        <!-- 差异统计卡片（顶部） -->
        <div class="grid grid-cols-4 gap-4 mb-6">
          <div class="bg-gradient-to-br from-blue-50 to-blue-100 p-4 rounded-xl border border-blue-200 shadow-sm">
            <div class="flex items-center space-x-2 mb-1">
              <i class="fa fa-list text-blue-600"></i>
              <span class="text-sm text-gray-600">行数差异</span>
            </div>
            <p class="text-2xl font-bold text-blue-700">
              {{ Math.abs((compareQuery1.tableData?.rows?.length || 0) - (compareQuery2.tableData?.rows?.length || 0)) }}
            </p>
          </div>
          <div class="bg-gradient-to-br from-purple-50 to-purple-100 p-4 rounded-xl border border-purple-200 shadow-sm">
            <div class="flex items-center space-x-2 mb-1">
              <i class="fa fa-columns text-purple-600"></i>
              <span class="text-sm text-gray-600">列数差异</span>
            </div>
            <p class="text-2xl font-bold text-purple-700">
              {{ Math.abs((compareQuery1.tableData?.headers?.length || 0) - (compareQuery2.tableData?.headers?.length || 0)) }}
            </p>
          </div>
          <div class="bg-gradient-to-br from-green-50 to-green-100 p-4 rounded-xl border border-green-200 shadow-sm">
            <div class="flex items-center space-x-2 mb-1">
              <i class="fa fa-check-circle text-green-600"></i>
              <span class="text-sm text-gray-600">查询 1 行数</span>
            </div>
            <p class="text-2xl font-bold text-green-700">
              {{ compareQuery1.tableData?.rows?.length || 0 }}
            </p>
          </div>
          <div class="bg-gradient-to-br from-orange-50 to-orange-100 p-4 rounded-xl border border-orange-200 shadow-sm">
            <div class="flex items-center space-x-2 mb-1">
              <i class="fa fa-check-circle text-orange-600"></i>
              <span class="text-sm text-gray-600">查询 2 行数</span>
            </div>
            <p class="text-2xl font-bold text-orange-700">
              {{ compareQuery2.tableData?.rows?.length || 0 }}
            </p>
          </div>
        </div>

        <!-- 查询信息对比 -->
        <div class="grid grid-cols-2 gap-4">
          <div class="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
            <div class="flex items-center space-x-2 mb-4 pb-3 border-b border-gray-200">
              <div class="w-3 h-3 rounded-full bg-primary"></div>
              <h4 class="font-semibold text-lg text-dark">查询 1</h4>
            </div>
            <div class="space-y-2.5 text-sm">
              <div class="flex items-start">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">查询内容:</span>
                <span class="text-dark flex-1">{{ compareQuery1.userPrompt }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">执行时间:</span>
                <span class="text-dark">{{ formatDate(compareQuery1.queryTime) }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">耗时:</span>
                <span class="text-dark">{{ compareQuery1.executionTime }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">大模型:</span>
                <span class="text-dark">{{ compareQuery1.model }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">数据库:</span>
                <span class="text-dark">{{ compareQuery1.database }}</span>
              </div>
            </div>
          </div>
          <div class="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
            <div class="flex items-center space-x-2 mb-4 pb-3 border-b border-gray-200">
              <div class="w-3 h-3 rounded-full bg-secondary"></div>
              <h4 class="font-semibold text-lg text-dark">查询 2</h4>
            </div>
            <div class="space-y-2.5 text-sm">
              <div class="flex items-start">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">查询内容:</span>
                <span class="text-dark flex-1">{{ compareQuery2.userPrompt }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">执行时间:</span>
                <span class="text-dark">{{ formatDate(compareQuery2.queryTime) }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">耗时:</span>
                <span class="text-dark">{{ compareQuery2.executionTime }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">大模型:</span>
                <span class="text-dark">{{ compareQuery2.model }}</span>
              </div>
              <div class="flex items-center">
                <span class="font-medium text-gray-600 w-24 flex-shrink-0">数据库:</span>
                <span class="text-dark">{{ compareQuery2.database }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- SQL对比 -->
        <div>
          <h4 class="font-semibold mb-3 text-dark flex items-center">
            <i class="fa fa-code mr-2 text-primary"></i>
            SQL 语句对比
          </h4>
          <div class="grid grid-cols-2 gap-4">
            <div class="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm">
              <div class="bg-primary/10 px-4 py-2 border-b border-gray-200">
                <span class="text-sm font-semibold text-primary">SQL 1</span>
              </div>
              <pre class="p-4 text-xs overflow-x-auto bg-gray-50 font-mono text-gray-800 max-h-40 overflow-y-auto">{{ compareQuery1.sqlQuery }}</pre>
            </div>
            <div class="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm">
              <div class="bg-secondary/10 px-4 py-2 border-b border-gray-200">
                <span class="text-sm font-semibold text-secondary">SQL 2</span>
              </div>
              <pre class="p-4 text-xs overflow-x-auto bg-gray-50 font-mono text-gray-800 max-h-40 overflow-y-auto">{{ compareQuery2.sqlQuery }}</pre>
            </div>
          </div>
          <div class="mt-3 flex items-center justify-center">
            <div class="flex items-center space-x-2 px-4 py-2 rounded-lg" :class="compareQuery1.sqlQuery === compareQuery2.sqlQuery ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'">
              <i :class="compareQuery1.sqlQuery === compareQuery2.sqlQuery ? 'fa fa-check-circle' : 'fa fa-times-circle'"></i>
              <span class="text-sm font-medium">
                SQL {{ compareQuery1.sqlQuery === compareQuery2.sqlQuery ? '相同' : '不同' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 结果数据对比 -->
        <div>
          <h4 class="font-semibold mb-3 text-dark flex items-center">
            <i class="fa fa-table mr-2 text-primary"></i>
            结果数据对比
          </h4>
          <div class="grid grid-cols-2 gap-4">
            <!-- 查询1结果 -->
            <div class="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm">
              <div class="bg-primary/10 px-4 py-3 border-b border-gray-200 sticky top-0 z-10">
                <div class="flex items-center justify-between">
                  <span class="text-sm font-semibold text-primary flex items-center">
                    <i class="fa fa-list mr-2"></i>
                    查询 1 结果
                  </span>
                  <span class="text-xs text-gray-600 bg-white px-2 py-1 rounded-full">
                    {{ compareQuery1.tableData?.rows?.length || 0 }} 行
                  </span>
                </div>
              </div>
              <div class="overflow-x-auto max-h-[50vh] overflow-y-auto">
                <table class="w-full text-xs">
                  <thead class="bg-gray-50 sticky top-0 z-10">
                    <tr>
                      <th
                        v-for="header in compareQuery1.tableData?.headers"
                        :key="header"
                        class="px-3 py-2 text-left border-b border-gray-200 font-semibold text-gray-700"
                      >
                        {{ header }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(row, rowIndex) in compareQuery1.tableData?.rows?.slice(0, 50)"
                      :key="rowIndex"
                      class="border-b border-gray-100 hover:bg-gray-50 transition-colors"
                    >
                      <td
                        v-for="(cell, cellIndex) in row"
                        :key="cellIndex"
                        class="px-3 py-2 text-gray-800"
                      >
                        {{ cell }}
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="(compareQuery1.tableData?.rows?.length || 0) > 50" class="p-3 text-xs text-gray-500 text-center bg-gray-50 border-t border-gray-200">
                  <i class="fa fa-info-circle mr-1"></i>
                  仅显示前50行，共 {{ compareQuery1.tableData?.rows?.length }} 行
                </div>
              </div>
            </div>

            <!-- 查询2结果 -->
            <div class="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm">
              <div class="bg-secondary/10 px-4 py-3 border-b border-gray-200 sticky top-0 z-10">
                <div class="flex items-center justify-between">
                  <span class="text-sm font-semibold text-secondary flex items-center">
                    <i class="fa fa-list mr-2"></i>
                    查询 2 结果
                  </span>
                  <span class="text-xs text-gray-600 bg-white px-2 py-1 rounded-full">
                    {{ compareQuery2.tableData?.rows?.length || 0 }} 行
                  </span>
                </div>
              </div>
              <div class="overflow-x-auto max-h-[50vh] overflow-y-auto">
                <table class="w-full text-xs">
                  <thead class="bg-gray-50 sticky top-0 z-10">
                    <tr>
                      <th
                        v-for="header in compareQuery2.tableData?.headers"
                        :key="header"
                        class="px-3 py-2 text-left border-b border-gray-200 font-semibold text-gray-700"
                      >
                        {{ header }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(row, rowIndex) in compareQuery2.tableData?.rows?.slice(0, 50)"
                      :key="rowIndex"
                      class="border-b border-gray-100 hover:bg-gray-50 transition-colors"
                    >
                      <td
                        v-for="(cell, cellIndex) in row"
                        :key="cellIndex"
                        class="px-3 py-2 text-gray-800"
                      >
                        {{ cell }}
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="(compareQuery2.tableData?.rows?.length || 0) > 50" class="p-3 text-xs text-gray-500 text-center bg-gray-50 border-t border-gray-200">
                  <i class="fa fa-info-circle mr-1"></i>
                  仅显示前50行，共 {{ compareQuery2.tableData?.rows?.length }} 行
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <!-- 创建收藏夹模态框 -->
    <Modal :is-open="isCreateCollectionModalOpen" @close="() => (isCreateCollectionModalOpen = false)" title="新建收藏夹">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">收藏夹名称 *</label>
          <input
            type="text"
            v-model="newCollectionName"
            placeholder="输入收藏夹名称"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30"
            maxlength="50"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">描述（可选）</label>
          <textarea
            v-model="newCollectionDescription"
            placeholder="输入收藏夹描述"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none focus:ring-2 focus:ring-primary/30"
            rows="3"
            maxlength="200"
          />
        </div>
        <div class="flex justify-end space-x-3 mt-6">
          <button
            @click="() => (isCreateCollectionModalOpen = false)"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleConfirmCreateCollection"
            :disabled="!newCollectionName.trim() || collectionsLoading"
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary/90 transition-colors disabled:bg-primary/50 disabled:cursor-not-allowed"
          >
            {{ collectionsLoading ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </Modal>

    <!-- 重命名分组模态框 -->
    <Modal :is-open="isRenameModalOpen" @close="() => (isRenameModalOpen = false)" title="重命名分组">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">原分组名称</label>
          <input
            type="text"
            :value="renameGroupPrompt"
            disabled
            class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">新分组名称</label>
          <input
            type="text"
            v-model="renameGroupNewName"
            placeholder="请输入新分组名称"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30"
          />
        </div>
        <div class="flex justify-end space-x-3 mt-6">
          <button
            @click="() => (isRenameModalOpen = false)"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleConfirmRenameGroup"
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary/90 transition-colors"
          >
            确认重命名
          </button>
        </div>
      </div>
    </Modal>

    <!-- 删除分组模态框 -->
    <Modal :is-open="isDeleteGroupModalOpen" @close="() => (isDeleteGroupModalOpen = false)" title="删除分组">
      <div class="space-y-4">
        <p class="text-sm text-gray-700">
          确定要删除分组 "{{ deleteGroupPrompt }}" 吗？
          此操作将删除该分组下的所有查询记录，且无法撤销。
        </p>
        <div class="flex justify-end space-x-3 mt-6">
          <button
            @click="() => (isDeleteGroupModalOpen = false)"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition-colors"
          >
            取消
          </button>
          <button
            @click="handleConfirmDeleteGroup"
            class="px-4 py-2 bg-red-600 text-white rounded-lg text-sm hover:bg-red-700 transition-colors"
          >
            确认删除
          </button>
        </div>
      </div>
    </Modal>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import Modal from '../components/ui/Modal.vue'
import QueryResult from '../components/feature/query/QueryResult.vue'
import QuerySnapshotList from '../components/feature/query/QuerySnapshotList.vue'
import { collectionRecordApi, queryCollectionApi, dialogApi } from '../services/api.real'
import { useQueryCollection } from '../composables/useQueryCollection'
import type { QueryResultData } from '../types'
import type { QueryCollection, CollectionRecord } from '../services/api/query'

// --- 类型定义 ---
type FilterType = 'all' | 'today' | '7days' | '30days'
type ConfirmAction = 'delete' | 'rerun' | 'bulkDelete'

interface Props {
  // 不再需要从外部传入，页面自己管理
}

interface ConfirmModalState {
  isOpen: boolean
  action: ConfirmAction | null
  targetId: string | null
  targetPrompt: string | null
  targetIds: string[] | null
}

// --- Props 和 Emits ---
const props = defineProps<Props>()

const emit = defineEmits<{
  'view-in-chat': [conversationId: string]
  rerun: [prompt: string]
  compare: [id1: string, id2: string]
}>()

// --- 响应式状态 ---
const userId = Number(sessionStorage.getItem('userId') || '1')
const loading = ref(true)

// 使用收藏夹组合式函数
const { collections, loading: collectionsLoading, createCollection, updateCollection, deleteCollection, loadCollections } = useQueryCollection(userId)

// 收藏记录映射（collectionId -> records）
const collectionRecordsMap = ref<Record<string, CollectionRecord[]>>({})
const loadingRecordsMap = ref<Record<string, boolean>>({})

// 收藏记录ID映射（queryLogId -> collectionRecordId）
const recordIdMap = ref<Record<string, string>>({})

// 对话标题缓存（conversationId -> title）
const conversationTitleMap = ref<Record<string, string>>({})
const loadingConversationTitles = ref<Set<string>>(new Set())

// 将所有收藏记录转换为 QueryResultData 格式用于显示
const allCollectionRecords = computed(() => {
  const records: QueryResultData[] = []
  Object.values(collectionRecordsMap.value).forEach(recordList => {
    recordList.forEach(record => {
      const queryData = recordToQueryResult(record)
      if (queryData) {
        // 保存ID映射：queryLogId -> collectionRecordId
        recordIdMap.value[queryData.id] = record.id
        records.push(queryData)
      }
    })
  })
  return records
})

// 将 CollectionRecord 转换为 QueryResultData
const recordToQueryResult = (record: CollectionRecord): QueryResultData | null => {
  if (!record || !record.id) {
    console.warn('[FavoritesPage] 无效的收藏记录:', record)
    return null
  }
  
  // 如果没有queryResult，尝试从其他字段构建
  let result: any = {}
  if (record.queryResult) {
    try {
      result = typeof record.queryResult === 'string' 
        ? JSON.parse(record.queryResult) 
        : record.queryResult
    } catch (e) {
      console.warn('[FavoritesPage] 解析queryResult失败:', e, 'record:', record)
      result = {}
    }
  }
  
  // 确保tableData格式正确
  let tableData = result?.tableData
  if (!tableData || !Array.isArray(tableData.headers) || !Array.isArray(tableData.rows)) {
    tableData = { headers: [], rows: [] }
  }
  
  // 使用record.id作为唯一标识，如果没有queryLogId则使用record.id
  const recordId = record.queryLogId ? String(record.queryLogId) : String(record.id)
  
  return {
    id: recordId,
    userPrompt: record.userPrompt || '查询结果',
    sqlQuery: record.sqlContent || '',
    conversationId: result?.conversationId || '',
    queryTime: record.createTime || record.addTime || new Date().toISOString(),
    executionTime: result?.executionTime || '0ms',
    database: result?.database || String(record.dbConnectionId || ''),
    model: result?.model || String(record.llmConfigId || ''),
    tableData: tableData,
    chartData: result?.chartData,
    dbConnectionId: record.dbConnectionId,
    llmConfigId: record.llmConfigId,
  }
}

// 加载收藏记录
const loadCollectionRecords = async (collectionId: string | number) => {
  const idStr = String(collectionId)
  if (loadingRecordsMap.value[idStr]) return
  
  loadingRecordsMap.value[idStr] = true
  try {
    console.log('[FavoritesPage] 加载收藏记录，collectionId:', idStr)
    const records = await collectionRecordApi.getByCollection(idStr)
    console.log('[FavoritesPage] 收到收藏记录:', records?.length || 0, '条')
    
    // 过滤掉无效的记录
    const validRecords = (records || []).filter(record => 
      record && record.id && (record.queryLogId || record.userPrompt)
    )
    console.log('[FavoritesPage] 有效记录:', validRecords.length, '条')
    
    collectionRecordsMap.value[idStr] = validRecords
  } catch (err) {
    console.error('[FavoritesPage] 加载收藏记录失败:', err, 'collectionId:', idStr)
    collectionRecordsMap.value[idStr] = []
  } finally {
    loadingRecordsMap.value[idStr] = false
  }
}


const searchTerm = ref('')
const activeFilters = ref({
  date: 'all' as FilterType,
  model: 'all',
  database: 'all',
})
const expandedGroup = ref<string | null>(null)
const selectedIds = ref<Set<string>>(new Set())
const viewingQuery = ref<QueryResultData | null>(null)

const confirmModalState = ref<ConfirmModalState>({
  isOpen: false,
  action: null,
  targetId: null,
  targetPrompt: null,
  targetIds: null,
})

// --- 样式配置 ---
const selectStyle = {
  backgroundImage:
    "url(\"data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23333' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e\")",
  backgroundRepeat: 'no-repeat',
  backgroundPosition: 'right 0.5rem center',
  backgroundSize: '1em',
}

// --- 工具函数 ---
const isDateInRange = (date: Date, days: number): boolean => {
  const today = new Date()
  const pastDate = new Date()
  if (days > 0) {
    pastDate.setDate(today.getDate() - (days - 1))
  }
  today.setHours(23, 59, 59, 999)
  pastDate.setHours(0, 0, 0, 0)
  return date >= pastDate && date <= today
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleString()
}

// 获取对话标题（带缓存）
const getConversationTitle = (id: string): string => {
  if (!id || id.trim() === '') {
    return '无对话'
  }
  
  // 如果缓存中有，直接返回
  if (conversationTitleMap.value[id]) {
    return conversationTitleMap.value[id]
  }
  
  // 如果正在加载，返回加载中提示
  if (loadingConversationTitles.value.has(id)) {
    return '加载中...'
  }
  
  // 异步加载对话标题
  loadConversationTitle(id)
  
  // 暂时返回ID，加载完成后会更新
  return id
}

// 异步加载对话标题
const loadConversationTitle = async (conversationId: string) => {
  if (!conversationId || conversationId.trim() === '' || loadingConversationTitles.value.has(conversationId)) {
    return
  }
  
  try {
    loadingConversationTitles.value.add(conversationId)
    const dialog = await dialogApi.getById(conversationId)
    conversationTitleMap.value[conversationId] = dialog.topic || '无标题对话'
  } catch (error) {
    console.error(`[FavoritesPage] 加载对话标题失败 (${conversationId}):`, error)
    conversationTitleMap.value[conversationId] = conversationId // 失败时显示ID
  } finally {
    loadingConversationTitles.value.delete(conversationId)
  }
}

// 收藏更新事件处理函数
const handleCollectionUpdate = () => {
  console.log('[FavoritesPage] 收到收藏更新事件，刷新数据')
  loadAllData()
}

// --- 数据加载 ---
onMounted(() => {
  loadAllData()
  
  // 预加载所有收藏记录的对话标题
  watch(
    () => allCollectionRecords.value,
    (records) => {
      records.forEach(record => {
        if (record.conversationId && !conversationTitleMap.value[record.conversationId]) {
          loadConversationTitle(record.conversationId)
        }
      })
    },
    { immediate: true }
  )
  
  // 监听自定义事件（从查询界面触发）
  window.addEventListener('collection-updated', handleCollectionUpdate)
})

// 清理事件监听
onUnmounted(() => {
  window.removeEventListener('collection-updated', handleCollectionUpdate)
})

const loadAllData = async (): Promise<void> => {
  try {
    loading.value = true
    // 清空ID映射
    recordIdMap.value = {}
    // 清空收藏记录映射
    collectionRecordsMap.value = {}
    
    console.log('[FavoritesPage] 开始加载数据，userId:', userId)
    
    // 加载收藏夹列表
    await loadCollections()
    console.log('[FavoritesPage] 收藏夹数量:', collections.value.length)
    
    // 加载每个收藏夹的记录
    const validCollections = collections.value.filter(c => 
      c && c.id && c.collectionName && 
      c.collectionName !== 'undefined' && 
      c.collectionName !== 'null' && 
      c.collectionName.trim() !== ''
    )
    
    console.log('[FavoritesPage] 有效收藏夹数量:', validCollections.length)
    
    if (validCollections.length > 0) {
      await Promise.all(validCollections.map(collection => {
        console.log('[FavoritesPage] 加载收藏夹:', collection.id, collection.collectionName)
        return loadCollectionRecords(collection.id)
      }))
      
      // 统计总记录数
      const totalRecords = Object.values(collectionRecordsMap.value).reduce((sum, records) => sum + records.length, 0)
      console.log('[FavoritesPage] 总收藏记录数:', totalRecords)
    } else {
      console.log('[FavoritesPage] 没有有效的收藏夹')
    }
  } catch (error) {
    console.error('[FavoritesPage] 加载数据失败:', error)
    alert('加载收藏夹数据失败，请刷新页面重试')
  } finally {
    loading.value = false
  }
}

// 监听收藏夹列表变化，自动加载每个收藏夹的记录
watch(collections, (newCollections) => {
  const validCollections = newCollections.filter(c => 
    c && c.id && c.collectionName && 
    c.collectionName !== 'undefined' && 
    c.collectionName !== 'null' && 
    c.collectionName.trim() !== ''
  )
  validCollections.forEach(collection => {
    loadCollectionRecords(collection.id)
  })
}, { immediate: true })

// --- 计算属性 ---
const modelOptions = computed(() => {
  // 从实际的查询记录中提取唯一的大模型名称
  const uniqueModels = Array.from(
    new Set(allCollectionRecords.value.map((q) => q.model).filter(Boolean)),
  )
  return [
    { value: 'all', label: '全部大模型' },
    ...uniqueModels.map((model) => ({ value: String(model), label: String(model) })),
  ]
})

const databaseOptions = computed(() => {
  // 从实际的查询记录中提取唯一的数据库名称
  const uniqueDatabases = Array.from(
    new Set(allCollectionRecords.value.map((q) => q.database).filter(Boolean)),
  )
  return [
    { value: 'all', label: '全部数据库' },
    ...uniqueDatabases.map((db) => ({ value: String(db), label: String(db) })),
  ]
})

const dateOptions = [
  { value: 'all', label: '全部日期' },
  { value: 'today', label: '今天' },
  { value: '7days', label: '近7天' },
  { value: '30days', label: '近30天' },
]

// 按收藏夹分组显示
const queryGroups = computed(() => {
  const groups: Record<string, QueryResultData[]> = {}
  
  console.log('[FavoritesPage] queryGroups computed，收藏夹数量:', collections.value.length)
  console.log('[FavoritesPage] collectionRecordsMap keys:', Object.keys(collectionRecordsMap.value))
  
  // 遍历所有收藏夹（包括空收藏夹）
  collections.value.forEach((collection) => {
    const collectionId = String(collection.id)
    const records = collectionRecordsMap.value[collectionId] || []
    const groupKey = collection.collectionName || '默认收藏夹'
    
    console.log('[FavoritesPage] 收藏夹:', collection.collectionName, '记录数:', records.length)
    
    // 初始化分组（即使是空收藏夹也要显示）
    if (!groups[groupKey]) {
      groups[groupKey] = []
    }
    
    // 将收藏记录转换为 QueryResultData
    records.forEach(record => {
      const queryData = recordToQueryResult(record)
      if (queryData) {
        groups[groupKey].push(queryData)
      } else {
        console.warn('[FavoritesPage] 记录转换失败:', record)
      }
    })
  })

  // 按时间排序
  Object.values(groups).forEach((snapshots) => {
    snapshots.sort((a, b) => new Date(b.queryTime).getTime() - new Date(a.queryTime).getTime())
  })
  
  return groups
})

const filteredGroups = computed(() => {
  // 筛选单个查询记录的辅助函数
  const filterQuery = (query: QueryResultData): boolean => {
    if (activeFilters.value.date !== 'all') {
      const queryDate = new Date(query.queryTime)
      const dateMatch =
        activeFilters.value.date === 'today'
          ? isDateInRange(queryDate, 1)
          : activeFilters.value.date === '7days'
            ? isDateInRange(queryDate, 7)
            : activeFilters.value.date === '30days'
              ? isDateInRange(queryDate, 30)
              : false
      if (!dateMatch) return false
    }

    if (activeFilters.value.model !== 'all' && query.model !== activeFilters.value.model) {
      return false
    }

    if (
      activeFilters.value.database !== 'all' &&
      query.database !== activeFilters.value.database
    ) {
      return false
    }

    return true
  }

  return Object.entries(queryGroups.value)
    .map(([prompt, snapshots]) => {
      // 检查收藏夹名称是否匹配搜索条件
      const matchesSearch = prompt.toLowerCase().includes(searchTerm.value.toLowerCase())
      if (!matchesSearch) {
        return null
      }

      // 如果是空收藏夹，直接返回（只要名称匹配就显示）
      if (snapshots.length === 0) {
        return [prompt, snapshots] as [string, QueryResultData[]]
      }

      // 对于有记录的收藏夹，应用筛选条件
      const filteredSnapshots = snapshots.filter(filterQuery)
      return [prompt, filteredSnapshots] as [string, QueryResultData[]]
    })
    .filter((item): item is [string, QueryResultData[]] => {
      if (!item) return false
      const [prompt, snapshots] = item
      
      // 空收藏夹只要名称匹配就显示
      if (snapshots.length === 0) {
        return true
      }
      
      // 有记录的收藏夹，筛选后至少要有1条记录
      return snapshots.length > 0
    })
})

const confirmModalContent = computed(() => {
  if (confirmModalState.value.action === 'delete') {
    return {
      title: '确认删除查询记录?',
      message: '此操作将永久删除该条查询快照。请确认是否继续?',
      buttonText: '确认删除',
      buttonClass: 'bg-red-600 hover:bg-red-700',
    }
  }
  if (confirmModalState.value.action === 'rerun') {
    return {
      title: '确认重新执行查询?',
      message: `您确定要重新执行查询："${confirmModalState.value.targetPrompt}" 吗? 这将消耗新的计算资源。`,
      buttonText: '重新执行',
      buttonClass: 'bg-primary hover:bg-primary/90',
    }
  }
  if (confirmModalState.value.action === 'bulkDelete' && confirmModalState.value.targetIds) {
    return {
      title: '确认批量删除?',
      message: `您确定要永久删除选中的 ${confirmModalState.value.targetIds.length} 条查询记录吗？此操作不可恢复。`,
      buttonText: '批量删除',
      buttonClass: 'bg-red-600 hover:bg-red-700',
    }
  }
  return { title: '', message: '', buttonText: '', buttonClass: '' }
})

// --- 事件处理函数 ---
const handleToggleGroup = (prompt: string): void => {
  expandedGroup.value = expandedGroup.value === prompt ? null : prompt
  selectedIds.value = new Set()
}

const handleSelectSnapshot = (id: string, checked: boolean): void => {
  const newSet = new Set(selectedIds.value)
  
  if (checked) {
    // 如果要选中，检查是否已达到最大选择数量（对比功能最多选择2条）
    if (newSet.size >= 2) {
      // 如果已经选择了2条，提示用户并阻止选中
      alert('对比功能最多只能选择2条记录，请先取消选择其他记录')
      return
    }
    // 添加选中
    newSet.add(id)
  } else {
    // 如果要取消选中，直接删除
    newSet.delete(id)
  }
  
  selectedIds.value = newSet
}

// 对比差异相关状态
const isCompareModalOpen = ref(false)
const compareQuery1 = ref<QueryResultData | null>(null)
const compareQuery2 = ref<QueryResultData | null>(null)

const handleCompare = (): void => {
  if (selectedIds.value.size !== 2) return
  const [id1, id2] = Array.from(selectedIds.value)
  
  // 查找对应的查询数据（从所有收藏记录中查找）
  const query1 = allCollectionRecords.value.find(q => q.id === id1)
  const query2 = allCollectionRecords.value.find(q => q.id === id2)
  
  if (query1 && query2) {
    compareQuery1.value = query1
    compareQuery2.value = query2
    isCompareModalOpen.value = true
    selectedIds.value = new Set()
  } else {
    alert('无法找到要对比的查询记录')
  }
}

const openDeleteConfirm = (id: string): void => {
  confirmModalState.value = {
    isOpen: true,
    action: 'delete',
    targetId: id,
    targetPrompt: null,
    targetIds: null,
  }
}

const openRerunConfirm = (prompt: string): void => {
  confirmModalState.value = {
    isOpen: true,
    action: 'rerun',
    targetId: null,
    targetPrompt: prompt,
    targetIds: null,
  }
}

const openBulkDeleteConfirm = (): void => {
  confirmModalState.value = {
    isOpen: true,
    action: 'bulkDelete',
    targetId: null,
    targetPrompt: null,
    targetIds: Array.from(selectedIds.value),
  }
}

const handleConfirmAction = async (): Promise<void> => {
  try {
    if (confirmModalState.value.action === 'delete' && confirmModalState.value.targetId) {
      // 删除收藏记录：需要将queryLogId转换为collectionRecordId
      const collectionRecordId = recordIdMap.value[confirmModalState.value.targetId]
      if (!collectionRecordId) {
        alert('找不到要删除的收藏记录')
        return
      }
      await collectionRecordApi.delete(collectionRecordId)
      await loadAllData() // 重新加载所有数据
    } else if (confirmModalState.value.action === 'rerun' && confirmModalState.value.targetPrompt) {
      emit('rerun', confirmModalState.value.targetPrompt)
    } else if (
      confirmModalState.value.action === 'bulkDelete' &&
      confirmModalState.value.targetIds
    ) {
      // 批量删除收藏记录：需要将queryLogId转换为collectionRecordId
      const collectionRecordIds = confirmModalState.value.targetIds
        .map(id => recordIdMap.value[id])
        .filter(Boolean)
      
      if (collectionRecordIds.length === 0) {
        alert('找不到要删除的收藏记录')
        return
      }
      
      await Promise.all(
        collectionRecordIds.map((id) => collectionRecordApi.delete(id)),
      )
      await loadAllData() // 重新加载所有数据
      selectedIds.value = new Set()
    }
  } catch (error) {
    console.error('操作失败:', error)
    alert('操作失败，请稍后重试')
  }
  confirmModalState.value = {
    isOpen: false,
    action: null,
    targetId: null,
    targetPrompt: null,
    targetIds: null,
  }
}

const handleCancelConfirm = (): void => {
  confirmModalState.value = {
    isOpen: false,
    action: null,
    targetId: null,
    targetPrompt: null,
    targetIds: null,
  }
}

const handleFilterChange = (type: 'date' | 'model' | 'database', value: string): void => {
  activeFilters.value = {
    ...activeFilters.value,
    [type]: value,
  }
}

const handleResetFilters = (): void => {
  activeFilters.value = {
    date: 'all',
    model: 'all',
    database: 'all',
  }
  searchTerm.value = ''
}

const setViewingQuery = (query: QueryResultData | null): void => {
  viewingQuery.value = query
}

// 处理查看对话
const handleViewInChat = (conversationId: string): void => {
  if (!conversationId || conversationId.trim() === '') {
    alert('该查询记录没有关联的对话，无法查看')
    return
  }
  emit('view-in-chat', conversationId)
}

// 创建收藏夹相关状态
const isCreateCollectionModalOpen = ref(false)
const newCollectionName = ref('')
const newCollectionDescription = ref('')

// 重命名分组相关状态
const isRenameModalOpen = ref(false)
const renameGroupPrompt = ref('')
const renameGroupNewName = ref('')

// 删除分组相关状态
const isDeleteGroupModalOpen = ref(false)
const deleteGroupPrompt = ref('')

// 打开创建收藏夹模态框
const handleOpenCreateCollectionModal = (): void => {
  newCollectionName.value = ''
  newCollectionDescription.value = ''
  isCreateCollectionModalOpen.value = true
}

// 确认创建收藏夹
const handleConfirmCreateCollection = async (): Promise<void> => {
  if (!newCollectionName.value.trim()) {
    alert('请输入收藏夹名称')
    return
  }
  
  // 防止重复提交
  if (collectionsLoading.value) {
    return
  }
  
  try {
    console.log('[FavoritesPage] 开始创建收藏夹:', {
      name: newCollectionName.value.trim(),
      description: newCollectionDescription.value.trim(),
      userId
    })
    
    const result = await createCollection(newCollectionName.value.trim(), newCollectionDescription.value.trim())
    
    if (result) {
      console.log('[FavoritesPage] 收藏夹创建成功:', result)
      // 关闭模态框
      isCreateCollectionModalOpen.value = false
      // 清空输入
      newCollectionName.value = ''
      newCollectionDescription.value = ''
      // 重新加载数据
      await loadAllData()
      alert('收藏夹创建成功')
    } else {
      console.error('[FavoritesPage] 创建收藏夹失败: result为null')
      alert('创建失败，请检查收藏夹名称是否重复或重试')
    }
  } catch (error) {
    console.error('[FavoritesPage] 创建收藏夹异常:', error)
    const errorMessage = error instanceof Error ? error.message : '未知错误'
    alert(`创建收藏夹失败: ${errorMessage}`)
  }
}

// 打开重命名分组模态框
const handleRenameGroup = (prompt: string): void => {
  renameGroupPrompt.value = prompt
  renameGroupNewName.value = prompt
  isRenameModalOpen.value = true
}

// 确认重命名分组（收藏夹）
const handleConfirmRenameGroup = async (): Promise<void> => {
  if (!renameGroupNewName.value.trim()) {
    alert('收藏夹名称不能为空')
    return
  }
  
  try {
    // 找到对应的收藏夹
    const collection = collections.value.find(c => c.collectionName === renameGroupPrompt.value)
    if (!collection) {
      alert('找不到要重命名的收藏夹')
      return
    }
    
    // 调用后端API更新收藏夹名称
    const result = await updateCollection(
      collection.id,
      renameGroupNewName.value.trim(),
      collection.description
    )
    
    if (result) {
      isRenameModalOpen.value = false
      await loadAllData() // 重新加载数据
      alert('收藏夹重命名成功')
    } else {
      alert('重命名失败，请重试')
    }
  } catch (error) {
    console.error('重命名收藏夹失败:', error)
    alert('重命名收藏夹失败，请稍后重试')
  }
}

// 打开删除分组模态框
const handleDeleteGroup = (prompt: string): void => {
  deleteGroupPrompt.value = prompt
  isDeleteGroupModalOpen.value = true
}

// 确认删除分组（收藏夹）
const handleConfirmDeleteGroup = async (): Promise<void> => {
  try {
    // 找到对应的收藏夹
    const collection = collections.value.find(c => c.collectionName === deleteGroupPrompt.value)
    if (!collection) {
      alert('找不到要删除的收藏夹')
      return
    }
    
    // 调用后端API删除收藏夹
    const result = await deleteCollection(collection.id)
    
    if (result) {
      isDeleteGroupModalOpen.value = false
      await loadAllData() // 重新加载数据
      alert('收藏夹删除成功')
    } else {
      alert('删除失败，请重试')
    }
  } catch (error) {
    console.error('删除收藏夹失败:', error)
    alert('删除收藏夹失败，请稍后重试')
  }
}
</script>

<!-- 使用全局 index.css 中的 .dot-flashing 样式 -->
