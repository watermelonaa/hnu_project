<!--
  @file views/admin/LLMConfigPage.vue
  @description 大模型配置管理页面

  功能：
  - 大模型列表展示
  - 添加/编辑/删除模型配置
  - 模型状态管理（启用/禁用/测试）
  - API Key 和端点配置

  @author Frontend Team
-->
<template>
  <div class="flex-1 overflow-y-auto p-6 space-y-6">
    <!-- 顶部操作按钮 -->
    <div class="flex justify-between items-center">
      <button
        @click="openModal('add')"
        class="bg-primary text-white rounded-lg px-4 py-2 text-sm btn-effect"
      >
        <i class="fa fa-plus mr-1"></i> 添加新模型
      </button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="text-center">
        <i class="fa fa-spinner fa-spin text-3xl text-primary mb-4"></i>
        <p class="text-gray-500">加载中...</p>
      </div>
    </div>

    <!-- 模型配置列表 -->
    <div v-else class="bg-white rounded-xl shadow-sm overflow-hidden">
      <div class="divide-y divide-gray-200">
        <div
          v-for="[name, versions] in Object.entries(groupedLlms)"
          :key="name"
          class="model-group"
        >
          <!-- 分组标题 -->
          <div
            class="p-4 grid grid-cols-12 gap-4 items-center cursor-pointer hover:bg-gray-50"
            @click="handleToggleGroup(name)"
          >
            <div class="font-bold text-lg col-span-8 flex items-center">
              <i
                :class="`fa fa-chevron-right transition-transform duration-200 mr-3 ${
                  expandedGroups.has(name) ? 'rotate-90' : ''
                }`"
              ></i>
              {{ name }}
            </div>
            <div class="text-sm text-gray-500 col-span-2 text-right">
              {{ versions.length }} 个版本
            </div>
            <div class="col-span-2 flex justify-end">
              <button
                @click.stop="openModal('add', { name })"
                class="px-3 py-1 bg-primary/10 text-primary rounded-md text-xs hover:bg-primary/20 transition-colors"
                :title="`为 ${name} 添加新版本`"
              >
                <i class="fa fa-plus mr-1"></i> 添加版本
              </button>
            </div>
          </div>

          <!-- 展开的内容 -->
          <div v-if="expandedGroups.has(name)" class="pl-10 pr-4 pb-2 bg-gray-50/50">
            <div
              v-for="llm in versions"
              :key="llm.id"
              class="py-4 grid grid-cols-1 md:grid-cols-6 gap-4 items-center border-t border-gray-200"
            >
              <div class="font-medium text-gray-800">{{ llm.version }}</div>
              <div class="md:col-span-3">
                <div class="relative">
                  <input
                    :type="showKeys[llm.id] ? 'text' : 'password'"
                    :value="llm.apiKey"
                    class="w-full px-4 py-2 border rounded-lg bg-white"
                    readonly
                  />
                  <button
                    @click.stop="toggleKeyVisibility(llm.id)"
                    class="absolute right-3 top-2.5 text-gray-400"
                  >
                    <i :class="`fa ${showKeys[llm.id] ? 'fa-eye-slash' : 'fa-eye'}`"></i>
                  </button>
                </div>
              </div>
              <div>
                <span class="text-xs text-gray-500"
                  >状态:
                  <span :class="`${getStatusClass(llm.status)} font-medium`">
                    {{ getStatusText(llm.status) }}
                  </span>
                </span>
              </div>
              <div class="flex justify-end space-x-3 text-base">
                <!-- 测试按钮 -->
                <button
                  @click.stop="handleTest(llm.id)"
                  :disabled="llm.status === 'testing' || llm.status === 'disabled'"
                  class="text-secondary disabled:text-gray-400"
                  title="测试连接"
                >
                  <i
                    :class="`fa ${
                      llm.status === 'testing' ? 'fa-spinner fa-spin' : 'fa-check-circle'
                    }`"
                  ></i>
                </button>

                <!-- 启用/禁用按钮 -->
                <button
                  @click.stop="requestToggleDisable(llm)"
                  :class="llm.status === 'disabled' ? 'text-success' : 'text-warning'"
                  :title="llm.status === 'disabled' ? '启用' : '禁用'"
                >
                  <i :class="`fa ${llm.status === 'disabled' ? 'fa-check-circle' : 'fa-ban'}`"></i>
                </button>

                <!-- 编辑按钮 -->
                <button @click.stop="openModal('edit', llm)" class="text-primary" title="编辑">
                  <i class="fa fa-edit"></i>
                </button>

                <!-- 删除按钮 -->
                <button
                  @click.stop="openModal('confirmDelete', llm)"
                  class="text-danger"
                  title="删除"
                >
                  <i class="fa fa-trash"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 模态框 -->
    <!-- 添加/编辑模态框 -->
    <AdminModal
      :is-open="modal === 'add' || modal === 'edit'"
      :title="modalTitle"
      @close="closeModal"
    >
      <form @submit.prevent="handleSave" class="space-y-4">
        <div>
          <label class="block text-sm mb-1">模型提供商</label>
          <input
            v-model="editForm.name"
            name="name"
            required
            :readonly="isAddingVersion"
            :class="`w-full px-3 py-2 border rounded-lg ${isAddingVersion ? 'bg-gray-100' : ''}`"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">模型版本</label>
          <input
            v-model="editForm.version"
            name="version"
            required
            placeholder="例如: gemini-2.5-pro、gpt-4"
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">API Key</label>
          <input
            v-model="editForm.apiKey"
            name="apiKey"
            required
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">API 端点 (可选)</label>
          <input
            v-model="editForm.endpoint"
            name="endpoint"
            placeholder="如无特殊需求可留空"
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div class="flex justify-end space-x-2 pt-4">
          <button type="button" @click="closeModal" class="px-4 py-2 border rounded-lg">
            取消
          </button>
          <button type="submit" class="px-4 py-2 bg-primary text-white rounded-lg">保存</button>
        </div>
      </form>
    </AdminModal>

    <!-- 删除确认模态框 -->
    <AdminModal :is-open="modal === 'confirmDelete'" title="确认删除模型" @close="closeModal">
      <p>您确定要删除模型 "{{ currentItem?.name }} ({{ currentItem?.version }})" 的配置吗？</p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="closeModal" class="px-4 py-2 border rounded-lg">取消</button>
        <button @click="confirmDelete" class="px-4 py-2 bg-danger text-white rounded-lg">
          确认删除
        </button>
      </div>
    </AdminModal>

    <!-- 启用/禁用确认模态框 -->
    <AdminModal
      :is-open="modal === 'confirmToggleDisable'"
      :title="`确认${currentItem?.status === 'disabled' ? '启用' : '禁用'}模型`"
      @close="closeModal"
    >
      <p>
        您确定要{{ currentItem?.status === 'disabled' ? '启用' : '禁用' }}模型 "{{
          currentItem?.name
        }}
        ({{ currentItem?.version }})" 吗？
      </p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="closeModal" class="px-4 py-2 border rounded-lg">取消</button>
        <button
          @click="confirmToggleDisable"
          :class="`px-4 py-2 text-white rounded-lg ${
            currentItem?.status === 'disabled' ? 'bg-success' : 'bg-danger'
          }`"
        >
          确认
        </button>
      </div>
    </AdminModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import type { LLMConfig } from '../../types'
import { llmConfigApi, type LlmConfig as BackendLlmConfig } from '../../services/api.real'

// 响应式数据
const loading = ref(true)
const llms = ref<LLMConfig[]>([])
const modal = ref<'add' | 'edit' | 'confirmDelete' | 'confirmToggleDisable' | null>(null)
const currentItem = ref<Partial<LLMConfig> | null>(null)
const showKeys = ref<Record<string, boolean>>({})
const expandedGroups = ref<Set<string>>(new Set(['Gemini'])) // 默认展开Gemini

// 编辑表单
const editForm = ref({
  name: '',
  version: '',
  apiKey: '',
  endpoint: '',
})

// 计算属性
const groupedLlms = computed(() => {
  const groups: Record<string, LLMConfig[]> = {}
  llms.value.forEach((llm) => {
    if (!groups[llm.name]) {
      groups[llm.name] = []
    }
    groups[llm.name].push(llm)
  })
  return groups
})

const isAddingVersion = computed(() => modal.value === 'add' && !!currentItem.value?.name)

const modalTitle = computed(() => {
  if (modal.value === 'add') {
    return isAddingVersion.value ? `为 ${currentItem.value?.name} 添加新版本` : '添加新模型'
  }
  return '编辑模型配置'
})

// 生命周期钩子
onMounted(() => {
  loadLlmConfigs()
})

// 方法
const loadLlmConfigs = async () => {
  try {
    loading.value = true
    const configs = await llmConfigApi.getList()
    const frontendConfigs: LLMConfig[] = configs.map((config) => ({
      id: String(config.id),
      name: config.name,
      version: config.version,
      apiKey: config.apiKey || '',
      endpoint: config.apiUrl,
      status: mapBackendStatusToFrontend(config.isDisabled, config.statusId),
    }))
    llms.value = frontendConfigs
  } catch (error) {
    console.error('加载大模型配置失败:', error)
    alert('加载大模型配置失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.value = false
  }
}

const mapBackendStatusToFrontend = (isDisabled: number, _statusId: number): LLMConfig['status'] => {
  if (isDisabled === 1) return 'disabled'
  return 'available'
}

const handleToggleGroup = (name: string) => {
  const newSet = new Set(expandedGroups.value)
  newSet.has(name) ? newSet.delete(name) : newSet.add(name)
  expandedGroups.value = newSet
}

const handleTest = (id: string) => {
  llms.value = llms.value.map((llm) => (llm.id === id ? { ...llm, status: 'testing' } : llm))

  setTimeout(() => {
    llms.value = llms.value.map((llm) => {
      if (llm.id === id) {
        const statuses: LLMConfig['status'][] = ['available', 'unstable', 'unavailable']
        return { ...llm, status: statuses[Math.floor(Math.random() * 3)] }
      }
      return llm
    })
  }, 2000)
}

const openModal = (type: 'add' | 'edit' | 'confirmDelete', item?: Partial<LLMConfig>) => {
  currentItem.value = item || null

  // 初始化编辑表单
  if (type === 'add' || type === 'edit') {
    editForm.value = {
      name: item?.name || '',
      version: item?.version || '',
      apiKey: item?.apiKey || '',
      endpoint: item?.endpoint || '',
    }
  }

  modal.value = type
}

const closeModal = () => {
  modal.value = null
  currentItem.value = null
  editForm.value = {
    name: '',
    version: '',
    apiKey: '',
    endpoint: '',
  }
}

const handleSave = async () => {
  // 验证必填字段
  if (!editForm.value.name.trim()) {
    alert('模型名称不能为空')
    return
  }
  if (!editForm.value.version.trim()) {
    alert('模型版本不能为空')
    return
  }
  if (!editForm.value.apiKey.trim()) {
    alert('API密钥不能为空')
    return
  }
  if (!editForm.value.endpoint.trim()) {
    alert('API地址不能为空')
    return
  }

  try {
    if (modal.value === 'add') {
      const backendConfig: Partial<BackendLlmConfig> = {
        name: editForm.value.name.trim(),
        version: editForm.value.version.trim(),
        apiKey: editForm.value.apiKey.trim(),
        apiUrl: editForm.value.endpoint.trim(),
        isDisabled: 0,
        statusId: 1,
        timeout: 60000,
      }
      await llmConfigApi.create(backendConfig)
      alert('添加成功')
      await loadLlmConfigs()
    } else if (modal.value === 'edit' && currentItem.value) {
      const backendConfig: Partial<BackendLlmConfig> = {
        id: Number(currentItem.value.id),
        name: editForm.value.name.trim(),
        version: editForm.value.version.trim(),
        apiKey: editForm.value.apiKey.trim(),
        apiUrl: editForm.value.endpoint.trim(),
      }
      await llmConfigApi.update(backendConfig)
      alert('更新成功')
      await loadLlmConfigs()
    }
  } catch (error) {
    console.error('保存大模型配置失败:', error)
    alert('保存失败: ' + (error instanceof Error ? error.message : '未知错误'))
    return
  }

  closeModal()
}

const confirmDelete = async () => {
  if (currentItem.value) {
    try {
      await llmConfigApi.delete(Number(currentItem.value.id))
      await loadLlmConfigs()
    } catch (error) {
      console.error('删除大模型配置失败:', error)
    }
  }
  closeModal()
}

const toggleKeyVisibility = (id: string) => {
  showKeys.value = {
    ...showKeys.value,
    [id]: !showKeys.value[id],
  }
}

const requestToggleDisable = (llm: LLMConfig) => {
  currentItem.value = llm
  modal.value = 'confirmToggleDisable'
}

const confirmToggleDisable = async () => {
  if (currentItem.value) {
    try {
      await llmConfigApi.toggle(Number(currentItem.value.id))
      alert('操作成功')
      await loadLlmConfigs()
    } catch (error) {
      console.error('切换大模型状态失败:', error)
      alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
  closeModal()
}

// 状态工具函数
const getStatusText = (status: LLMConfig['status']) => {
  const mapping = {
    available: '可用',
    unstable: '不稳定',
    unavailable: '不可用',
    testing: '测试中...',
    disabled: '已禁用',
  }
  return mapping[status]
}

const getStatusClass = (status: LLMConfig['status']) => {
  const mapping = {
    available: 'text-success',
    unstable: 'text-warning',
    unavailable: 'text-danger',
    testing: 'text-blue-500',
    disabled: 'text-gray-500',
  }
  return mapping[status]
}
</script>

<style scoped>
.btn-effect:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.model-group:hover {
  background-color: #f9fafb;
}
</style>

