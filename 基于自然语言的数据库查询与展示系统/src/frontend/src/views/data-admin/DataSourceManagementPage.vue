<!--
  @file views/data-admin/DataSourceManagementPage.vue
  @description 数据源管理页面

  功能：
  - 数据源列表展示（筛选、搜索）
  - 添加/编辑/删除数据源
  - 测试数据源连接
  - 数据源状态管理

  @author Frontend Team
-->
<template>
  <main class="flex-1 overflow-y-auto p-6 space-y-6">
    <!-- 过滤器区域 -->
    <div class="bg-white rounded-xl p-4 shadow-sm">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <div class="relative">
            <i class="fa fa-search absolute left-3 top-3 text-gray-400"></i>
            <input
              v-model="filters.search"
              name="search"
              type="text"
              placeholder="搜索数据源名称"
              class="w-full pl-10 pr-4 py-2 border rounded-lg"
            />
          </div>
        </div>
        <div class="w-full md:w-40">
          <select v-model="filters.type" name="type" class="w-full px-4 py-2 border rounded-lg">
            <option value="">所有类型</option>
            <option value="MySQL">MySQL</option>
            <option value="PostgreSQL">PostgreSQL</option>
            <option value="Oracle">Oracle</option>
            <option value="SQL Server">SQL Server</option>
          </select>
        </div>
        <div class="w-full md:w-32">
          <select v-model="filters.status" name="status" class="w-full px-4 py-2 border rounded-lg">
            <option value="">所有状态</option>
            <option value="connected">已连接</option>
            <option value="disconnected">未连接</option>
            <option value="error">连接错误</option>
            <option value="disabled">已禁用</option>
          </select>
        </div>
        <div class="self-end">
          <button @click="resetFilters" class="bg-white border rounded-lg px-4 py-2 btn-effect">
            重置
          </button>
        </div>
        <button
          @click="openModal('add')"
          class="bg-primary text-white rounded-lg px-4 py-2 text-sm btn-effect"
        >
          <i class="fa fa-plus mr-1"></i> 添加数据源
        </button>
      </div>
    </div>

    <!-- 数据源表格 -->
    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
      <div v-if="loading" class="flex items-center justify-center h-64">
        <div class="text-center">
          <i class="fa fa-spinner fa-spin text-3xl text-primary mb-4"></i>
          <p class="text-gray-500">加载中...</p>
        </div>
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-gray-50">
              <th class="text-left px-6 py-3">数据源名称</th>
              <th class="text-left px-6 py-3">数据库类型</th>
              <th class="text-left px-6 py-3">连接地址</th>
              <th class="text-left px-6 py-3">状态</th>
              <th class="text-left px-6 py-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ds in filteredDataSources" :key="ds.id" class="border-b hover:bg-gray-50">
              <td class="px-6 py-4">{{ ds.name }}</td>
              <td class="px-6 py-4">{{ ds.type }}</td>
              <td class="px-6 py-4">{{ ds.address }}</td>
              <td class="px-6 py-4">
                <span :class="getStatusChipClass(ds.status)">
                  {{ getStatusText(ds.status) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <button @click="openModal('edit', ds)" class="text-primary hover:underline mr-3">
                  编辑
                </button>
                <button
                  @click="handleTestConnection(ds.id)"
                  :disabled="ds.status === 'testing' || ds.status === 'disabled'"
                  class="text-secondary hover:underline mr-3 disabled:text-gray-400"
                >
                  {{ ds.status === 'testing' ? '测试中...' : '测试连接' }}
                </button>
                <button
                  @click="requestToggleDisable(ds)"
                  :class="[
                    'hover:underline mr-3',
                    ds.status === 'disabled' ? 'text-success' : 'text-warning',
                  ]"
                >
                  {{ ds.status === 'disabled' ? '启用' : '禁用' }}
                </button>
                <button @click="openModal('delete', ds)" class="text-danger hover:underline">
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 添加/编辑模态框 -->
    <AdminModal
      :is-open="modal === 'add' || modal === 'edit'"
      @close="modal = null"
      :title="modal === 'add' ? '添加数据源' : '编辑数据源'"
    >
      <form @submit.prevent="handleSave" class="space-y-4">
        <div>
          <label class="block text-sm mb-1">数据源名称</label>
          <input
            v-model="formData.name"
            name="name"
            required
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">数据库类型</label>
          <select v-model="formData.type" name="type" class="w-full px-3 py-2 border rounded-lg">
            <option value="MySQL">MySQL</option>
            <option value="PostgreSQL">PostgreSQL</option>
            <option value="Oracle">Oracle</option>
            <option value="SQL Server">SQL Server</option>
          </select>
        </div>
        <div>
          <label class="block text-sm mb-1">连接地址（主机名或IP）</label>
          <input
            v-model="formData.host"
            name="host"
            required
            class="w-full px-3 py-2 border rounded-lg"
            placeholder="例如：localhost 或 192.168.1.100"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">端口</label>
          <input
            v-model="formData.port"
            name="port"
            required
            class="w-full px-3 py-2 border rounded-lg"
            placeholder="例如：3306"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">数据库名称</label>
          <input
            v-model="formData.database"
            name="database"
            required
            class="w-full px-3 py-2 border rounded-lg"
            placeholder="例如：natural_language_query_system"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">数据库账号</label>
          <input
            v-model="formData.username"
            name="username"
            type="text"
            required
            class="w-full px-3 py-2 border rounded-lg"
            :placeholder="modal === 'edit' ? '请输入新的数据库账号' : ''"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">数据库密码</label>
          <input
            v-model="formData.password"
            name="password"
            type="password"
            required
            class="w-full px-3 py-2 border rounded-lg"
            :placeholder="modal === 'edit' ? '请输入新的数据库密码' : ''"
          />
          <p v-if="modal === 'edit'" class="text-xs text-gray-500 mt-1">
            编辑时需要重新输入密码以更新数据库连接信息
          </p>
        </div>
        <div class="flex justify-end space-x-2 pt-4">
          <button type="button" @click="modal = null" class="px-4 py-2 border rounded-lg">
            取消
          </button>
          <button type="submit" class="px-4 py-2 bg-primary text-white rounded-lg">确认</button>
        </div>
      </form>
    </AdminModal>

    <!-- 删除确认模态框 -->
    <AdminModal :is-open="modal === 'delete'" @close="modal = null" title="确认删除数据源">
      <p>您确定要删除数据源 "{{ currentItem?.name }}" 吗？此操作不可撤销。</p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="modal = null" class="px-4 py-2 border rounded-lg">取消</button>
        <button @click="confirmDelete" class="px-4 py-2 bg-danger text-white rounded-lg">
          确认删除
        </button>
      </div>
    </AdminModal>

    <!-- 启用/禁用确认模态框 -->
    <AdminModal
      :is-open="modal === 'confirmDisable'"
      @close="modal = null"
      :title="`确认${currentItem?.status === 'disabled' ? '启用' : '禁用'}数据源`"
    >
      <p>
        您确定要{{ currentItem?.status === 'disabled' ? '启用' : '禁用' }}数据源 "{{
          currentItem?.name
        }}" 吗？
      </p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="modal = null" class="px-4 py-2 border rounded-lg">取消</button>
        <button
          @click="confirmToggleDisable"
          :class="[
            'px-4 py-2 text-white rounded-lg',
            currentItem?.status === 'disabled' ? 'bg-success' : 'bg-danger',
          ]"
        >
          确认
        </button>
      </div>
    </AdminModal>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import type { DataSource } from '../../types'
import { dbConnectionApi, type DbConnection } from '../../services/api.real'
import { logOperation, LogModule, LogOperationType, LogStatus } from '../../utils/logger'

// 响应式数据
const dataSources = ref<DataSource[]>([])
const loading = ref(true)
const modal = ref<'add' | 'edit' | 'delete' | 'confirmDisable' | null>(null)
const currentItem = ref<DataSource | null>(null)

// 过滤器
const filters = reactive({
  search: '',
  type: '',
  status: '',
})

// 表单数据
const formData = reactive({
  name: '',
  type: 'MySQL' as DataSource['type'],
  host: '',
  port: '',
  database: '',
  username: '',
  password: '',
})

// 计算属性
const filteredDataSources = computed(() => {
  return dataSources.value.filter(
    (ds) =>
      ds.name.toLowerCase().includes(filters.search.toLowerCase()) &&
      (filters.type ? ds.type === filters.type : true) &&
      (filters.status ? ds.status === filters.status : true),
  )
})

// 生命周期
onMounted(() => {
  loadDataSources()
})

// 方法
const loadDataSources = async () => {
  try {
    loading.value = true
    const connections = await dbConnectionApi.getList()
    // 转换后端DbConnection格式到前端DataSource格式
    const frontendDataSources: DataSource[] = connections.map((conn) => ({
      id: String(conn.id),
      name: conn.name,
      type: mapDbTypeIdToType(conn.dbTypeId), // 需要根据dbTypeId映射
      address: conn.url,
      status: mapBackendStatusToFrontend(conn.status),
    }))
    dataSources.value = frontendDataSources
  } catch (error) {
    console.error('加载数据源列表失败:', error)
    alert('加载数据源列表失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.value = false
  }
}

// 映射数据库类型ID到前端类型
const mapDbTypeIdToType = (dbTypeId: number): DataSource['type'] => {
  // 简化映射，实际应该从后端获取类型列表
  if (dbTypeId === 1) return 'MySQL'
  if (dbTypeId === 2) return 'PostgreSQL'
  if (dbTypeId === 3) return 'Oracle'
  if (dbTypeId === 4) return 'SQL Server'
  return 'MySQL'
}

// 映射前端类型到数据库类型ID
const mapTypeToDbTypeId = (type: DataSource['type']): number => {
  if (type === 'MySQL') return 1
  if (type === 'PostgreSQL') return 2
  if (type === 'Oracle') return 3
  if (type === 'SQL Server') return 4
  return 1
}

// 映射后端状态到前端状态
const mapBackendStatusToFrontend = (status: string): DataSource['status'] => {
  if (status === 'connected') return 'connected'
  if (status === 'disconnected') return 'disconnected'
  if (status === 'error') return 'error'
  if (status === 'disabled') return 'disabled'
  return 'disconnected'
}

// 重置过滤器
const resetFilters = () => {
  filters.search = ''
  filters.type = ''
  filters.status = ''
}

// 打开模态框
const openModal = (type: 'add' | 'edit' | 'delete', item?: DataSource) => {
  currentItem.value = item || null
  modal.value = type

  // 如果是编辑模式，填充表单数据
  if (type === 'edit' && item) {
    const [host, rest] = item.address.split(':')
    const [port, database] = rest ? rest.split('/') : ['', '']

    formData.name = item.name
    formData.type = item.type
    formData.host = host
    formData.port = port
    formData.database = database
    formData.username = '' // 安全考虑，不显示密码和用户名
    formData.password = ''
  } else if (type === 'add') {
    // 重置表单数据
    Object.assign(formData, {
      name: '',
      type: 'MySQL',
      host: '',
      port: '',
      database: '',
      username: '',
      password: '',
    })
  }
}

// 保存数据源
const handleSave = async () => {
  // 验证必填字段
  if (!formData.name.trim()) {
    alert('连接名称不能为空')
    return
  }
  if (!formData.host.trim()) {
    alert('数据库主机地址不能为空')
    return
  }
  if (!formData.port.trim()) {
    alert('数据库端口不能为空')
    return
  }
  if (!formData.database.trim()) {
    alert('数据库名称不能为空')
    return
  }
  if (!formData.username.trim()) {
    alert('数据库账号不能为空')
    return
  }
  if (!formData.password.trim()) {
    alert('数据库密码不能为空')
    return
  }

  try {
    if (modal.value === 'add') {
      const backendConnection: Partial<DbConnection> = {
        name: formData.name.trim(),
        dbTypeId: mapTypeToDbTypeId(formData.type),
        url: `${formData.host.trim()}:${formData.port.trim()}/${formData.database.trim()}`,
        username: formData.username.trim(),
        password: formData.password.trim(),
        status: 'disconnected',
        createUserId: Number(sessionStorage.getItem('userId') || '1'),
      }
      await dbConnectionApi.create(backendConnection)
      await logOperation(
        LogModule.DATA_SOURCE,
        LogOperationType.CREATE,
        `创建数据源：${backendConnection.name}`,
        LogStatus.SUCCESS,
      )
      alert('添加成功')
      await loadDataSources()
    } else if (modal.value === 'edit' && currentItem.value) {
      const backendConnection: Partial<DbConnection> = {
        id: Number(currentItem.value.id),
        name: formData.name,
        dbTypeId: mapTypeToDbTypeId(formData.type),
        url: `${formData.host}:${formData.port}/${formData.database}`,
        username: formData.username.trim(),
        password: formData.password.trim(),
      }
      await dbConnectionApi.update(backendConnection)
      await logOperation(
        LogModule.DATA_SOURCE,
        LogOperationType.UPDATE,
        `更新数据源：${backendConnection.name}`,
        LogStatus.SUCCESS,
      )
      alert('更新成功')
      await loadDataSources()
    }
  } catch (error) {
    console.error('保存数据源失败:', error)
    const operationType = modal.value === 'add' ? LogOperationType.CREATE : LogOperationType.UPDATE
    await logOperation(
      LogModule.DATA_SOURCE,
      operationType,
      `保存数据源失败：${error instanceof Error ? error.message : '未知错误'}`,
      LogStatus.FAILURE,
    )
    alert('保存失败: ' + (error instanceof Error ? error.message : '未知错误'))
    return
  }
  modal.value = null
}

// 确认删除
const confirmDelete = async () => {
  if (currentItem.value) {
    try {
      await dbConnectionApi.delete(Number(currentItem.value.id))
      await logOperation(
        LogModule.DATA_SOURCE,
        LogOperationType.DELETE,
        `删除数据源：${currentItem.value.name}`,
        LogStatus.SUCCESS,
      )
      alert('删除成功')
      await loadDataSources()
    } catch (error) {
      console.error('删除数据源失败:', error)
      await logOperation(
        LogModule.DATA_SOURCE,
        LogOperationType.DELETE,
        `删除数据源失败：${error instanceof Error ? error.message : '未知错误'}`,
        LogStatus.FAILURE,
      )
      alert('删除失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
  modal.value = null
}

// 测试连接
const handleTestConnection = async (id: string) => {
  const dataSource = dataSources.value.find((ds) => ds.id === id)
  dataSources.value = dataSources.value.map((ds) =>
    ds.id === id ? { ...ds, status: 'testing' } : ds,
  )

  try {
    const result = await dbConnectionApi.test(Number(id))
    // 测试连接成功后，重新加载数据源列表以获取后端更新后的状态
    await loadDataSources()
    if (result) {
      await logOperation(
        LogModule.DATA_SOURCE,
        LogOperationType.TEST,
        `测试数据源连接成功：${dataSource?.name}`,
        LogStatus.SUCCESS,
      )
      alert('连接测试成功')
    } else {
      await logOperation(
        LogModule.DATA_SOURCE,
        LogOperationType.TEST,
        `测试数据源连接失败：${dataSource?.name}`,
        LogStatus.FAILURE,
      )
      alert('连接测试失败')
    }
  } catch (error) {
    console.error('测试连接失败:', error)
    // 即使失败也重新加载列表，确保状态同步
    await loadDataSources()
    await logOperation(
      LogModule.DATA_SOURCE,
      LogOperationType.TEST,
      `测试数据源连接异常：${dataSource?.name} - ${
        error instanceof Error ? error.message : '未知错误'
      }`,
      LogStatus.FAILURE,
    )
    alert('测试连接失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 请求切换禁用状态
const requestToggleDisable = (dataSource: DataSource) => {
  currentItem.value = dataSource
  modal.value = 'confirmDisable'
}

// 确认切换禁用状态
const confirmToggleDisable = async () => {
  if (currentItem.value) {
    try {
      const newStatus = currentItem.value.status === 'disabled' ? 'disconnected' : 'disabled'
      const operationType =
        currentItem.value.status === 'disabled' ? LogOperationType.ENABLE : LogOperationType.DISABLE

      await dbConnectionApi.update({
        id: Number(currentItem.value.id),
        status: newStatus,
      })

      await logOperation(
        LogModule.DATA_SOURCE,
        operationType,
        `${operationType}数据源：${currentItem.value.name}`,
        LogStatus.SUCCESS,
      )
      alert('操作成功')
      await loadDataSources()
    } catch (error) {
      console.error('切换数据源状态失败:', error)
      const operationType =
        currentItem.value.status === 'disabled' ? LogOperationType.ENABLE : LogOperationType.DISABLE

      await logOperation(
        LogModule.DATA_SOURCE,
        operationType,
        `${operationType}数据源失败：${error instanceof Error ? error.message : '未知错误'}`,
        LogStatus.FAILURE,
      )
      alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
  modal.value = null
}

// 获取状态徽章类名
const getStatusChipClass = (status: DataSource['status']) => {
  const styles = {
    connected: 'bg-success/10 text-success',
    disconnected: 'bg-gray-100 text-gray-600',
    error: 'bg-danger/10 text-danger',
    testing: 'bg-blue-100 text-blue-600 animate-pulse',
    disabled: 'bg-gray-200 text-gray-700',
  }
  return `px-2 py-1 ${styles[status]} text-xs rounded-full`
}

// 获取状态文本
const getStatusText = (status: DataSource['status']) => {
  const text = {
    connected: '已连接',
    disconnected: '未连接',
    error: '连接错误',
    testing: '测试中...',
    disabled: '已禁用',
  }
  return text[status]
}
</script>
