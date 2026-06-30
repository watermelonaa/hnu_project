<!--
  @file views/data-admin/DataAdminNotificationPage.vue
  @description 通知管理页面（数据管理员）

  功能：
  - 通知列表展示
  - 创建/编辑/删除通知
  - 发布/置顶通知
  - 目标用户群体选择

  @author Frontend Team
-->
<template>
  <div class="flex-1 overflow-y-auto p-6 space-y-6">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="text-center">
        <i class="fa fa-spinner fa-spin text-3xl text-primary mb-4"></i>
        <p class="text-gray-500">加载中...</p>
      </div>
    </div>

    <!-- 主内容 -->
    <div v-else class="space-y-6">
      <!-- 顶部操作按钮 -->
    <div class="flex justify-between items-center">
      <button
        @click="openModal('add')"
        class="bg-primary text-white rounded-lg px-4 py-2 text-sm btn-effect"
      >
        <i class="fa fa-plus mr-1"></i> 发布新通知
      </button>
    </div>

      <!-- 筛选栏 -->
      <div class="bg-white rounded-xl p-6 shadow-sm">
        <div class="flex flex-col md:flex-row gap-4">
          <div class="flex-1">
            <input
              v-model="filters.search"
              name="search"
              placeholder="搜索标题/内容"
              class="w-full px-4 py-2 border rounded-lg"
            />
          </div>
          <div class="w-full md:w-48">
            <select v-model="filters.role" name="role" class="w-full px-4 py-2 border rounded-lg">
              <option value="">所有角色</option>
              <option value="all">全体用户</option>
              <option value="sys-admin">系统管理员</option>
              <option value="data-admin">数据管理员</option>
              <option value="normal-user">普通用户</option>
            </select>
          </div>
          <div class="w-full md:w-40">
            <select
              v-model="filters.priority"
              name="priority"
              class="w-full px-4 py-2 border rounded-lg"
            >
              <option value="">所有优先级</option>
              <option value="urgent">紧急</option>
              <option value="normal">普通</option>
              <option value="low">低</option>
            </select>
          </div>
        </div>
      </div>

    <!-- 通知表格 -->
    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-gray-50">
              <th class="px-6 py-3 text-left">标题</th>
                <th class="px-6 py-3 text-left">目标角色</th>
                <th class="px-6 py-3 text-left">优先级</th>
              <th class="px-6 py-3 text-left">发布者</th>
              <th class="px-6 py-3 text-left">发布时间</th>
                <th class="px-6 py-3 text-left">状态</th>
              <th class="px-6 py-3 text-left">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
                v-for="notification in paginatedNotifications"
              :key="notification.id"
              :class="`border-b hover:bg-gray-50 ${notification.pinned ? 'bg-yellow-50' : ''}`"
            >
              <td class="px-6 py-4 font-medium">
                {{ notification.title }}
                <i v-if="notification.pinned" class="fa fa-thumb-tack text-yellow-500 ml-1"></i>
              </td>
                <td class="px-6 py-4">{{ getRoleDisplayName(notification.role) }}</td>
                <td class="px-6 py-4">{{ getPriorityDisplayName(notification.priority) }}</td>
              <td class="px-6 py-4">{{ notification.publisher }}</td>
              <td class="px-6 py-4">{{ notification.publishTime }}</td>
                <td class="px-6 py-4">{{ getStatusDisplayName(notification.status) }}</td>
              <td class="px-6 py-4">
                <button
                  @click="openModal('view', notification)"
                  class="text-indigo-500 hover:text-indigo-600 hover:underline mr-3"
                  title="查看详情"
                >
                  <i class="fa fa-eye"></i>
                </button>
                <button
                  @click="handleTogglePin(notification)"
                    :class="`hover:underline mr-3 ${
                      notification.pinned ? 'text-yellow-600' : 'text-gray-500'
                    }`"
                  :title="notification.pinned ? '取消置顶' : '置顶'"
                >
                  <i class="fa fa-thumb-tack"></i>
                </button>
                <button
                  @click="openModal('edit', notification)"
                  class="text-primary hover:underline mr-3"
                >
                  编辑
                </button>
                <button
                  @click="openModal('delete', notification)"
                  class="text-danger hover:underline"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        </div>
        <div class="px-6 py-4 border-t flex justify-between items-center">
          <p class="text-sm text-gray-500">
            显示 {{ paginatedNotifications.length }} 条，共 {{ filteredNotifications.length }} 条
          </p>
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
    </div>

    <!-- 模态框 -->
    <!-- 添加/编辑模态框 -->
    <AdminModal
      :isOpen="modal === 'add' || modal === 'edit'"
      :title="modal === 'add' ? '发布新通知' : '编辑通知'"
      @close="closeModal"
    >
      <form @submit.prevent="handlePublish" class="space-y-4">
        <div>
          <label class="block text-sm mb-1">标题</label>
          <input
            v-model="editForm.title"
            name="title"
            required
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">内容</label>
          <textarea
            v-model="editForm.content"
            name="content"
            required
            class="w-full px-3 py-2 border rounded-lg"
            rows="4"
          ></textarea>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm mb-1">目标角色</label>
            <select v-model="editForm.role" name="role" class="w-full px-3 py-2 border rounded-lg">
              <option value="all">全体用户</option>
              <option value="sys-admin">系统管理员</option>
              <option value="data-admin">数据管理员</option>
              <option value="normal-user">普通用户</option>
            </select>
          </div>
          <div>
            <label class="block text-sm mb-1">优先级</label>
            <select
              v-model="editForm.priority"
              name="priority"
              class="w-full px-3 py-2 border rounded-lg"
            >
              <option value="urgent">紧急</option>
              <option value="normal">普通</option>
              <option value="low">低</option>
            </select>
          </div>
        </div>
        <div>
          <label class="flex items-center">
            <input v-model="editForm.pinned" type="checkbox" name="pinned" class="mr-2" />
            置顶通知
          </label>
        </div>
        <div class="flex justify-end space-x-2 pt-4">
          <button type="button" @click="closeModal" class="px-4 py-2 border border-gray-300 rounded-lg text-sm">
            取消
          </button>
          <button 
            type="button" 
            @click="handleSave" 
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm"
          >
            保存草稿
          </button>
          <button 
            type="button" 
            @click="handlePublish" 
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm"
          >
            发布
          </button>
        </div>
      </form>
    </AdminModal>

    <!-- 查看详情模态框 -->
    <AdminModal :isOpen="modal === 'view'" title="通知详情" @close="closeModal">
      <div v-if="currentItem" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">标题</label>
          <p class="text-base text-gray-900">{{ currentItem.title }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">内容</label>
          <div class="text-base text-gray-900 whitespace-pre-wrap bg-gray-50 p-3 rounded-lg border border-gray-200 min-h-[100px]">
            {{ currentItem.content }}
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">目标角色</label>
            <p class="text-base text-gray-900">{{ getRoleDisplayName(currentItem.role) }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">优先级</label>
            <p class="text-base text-gray-900">{{ getPriorityDisplayName(currentItem.priority) }}</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">发布者</label>
            <p class="text-base text-gray-900">{{ currentItem.publisher }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">发布时间</label>
            <p class="text-base text-gray-900">{{ currentItem.publishTime || '未发布' }}</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">状态</label>
            <p class="text-base text-gray-900">{{ getStatusDisplayName(currentItem.status) }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">置顶</label>
            <p class="text-base text-gray-900">
              <span v-if="currentItem.pinned" class="text-yellow-600">
                <i class="fa fa-thumb-tack mr-1"></i>已置顶
              </span>
              <span v-else class="text-gray-500">未置顶</span>
            </p>
          </div>
        </div>
        <div class="flex justify-end space-x-2 pt-4 border-t">
          <button @click="closeModal" class="px-4 py-2 bg-primary text-white rounded-lg">
            关闭
          </button>
        </div>
      </div>
    </AdminModal>

    <!-- 删除确认模态框 -->
    <AdminModal :isOpen="modal === 'delete'" title="确认删除通知" @close="closeModal">
      <p>您确定要删除通知 "{{ currentItem?.title }}" 吗？</p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="closeModal" class="px-4 py-2 border rounded-lg">取消</button>
        <button @click="confirmDelete" class="px-4 py-2 bg-danger text-white rounded-lg">
          确认删除
        </button>
      </div>
    </AdminModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import type { AdminNotification, UserRole } from '../../types'
import { notificationApi } from '../../services/api.real'

// 响应式数据
const notifications = ref<AdminNotification[]>([])
const loading = ref(true)
const filters = ref({
  search: '',
  role: '',
  priority: '',
})
const modal = ref<'add' | 'edit' | 'delete' | 'view' | null>(null)
const currentItem = ref<AdminNotification | null>(null)

// 分页状态
const currentPage = ref(1)
const pageSize = ref(10)

// 编辑表单
const editForm = ref({
  title: '',
  content: '',
  role: 'all' as 'all' | UserRole,
  priority: 'normal' as 'urgent' | 'normal' | 'low',
  pinned: false,
})

// 计算属性：筛选和排序通知
const filteredNotifications = computed(() => {
  const filtered = notifications.value.filter((n) => {
    const matchesSearch = filters.value.search
      ? n.title.toLowerCase().includes(filters.value.search.toLowerCase()) ||
        n.content.toLowerCase().includes(filters.value.search.toLowerCase())
      : true

    const matchesRole = filters.value.role ? n.role === filters.value.role : true
    const matchesPriority = filters.value.priority ? n.priority === filters.value.priority : true

    return matchesSearch && matchesRole && matchesPriority
  })

  // 排序：置顶的在前，然后按发布时间倒序
  return filtered.sort((a, b) => {
    // 置顶优先
    if (a.pinned && !b.pinned) return -1
    if (!a.pinned && b.pinned) return 1

    // 按发布时间倒序
    return new Date(b.publishTime).getTime() - new Date(a.publishTime).getTime()
  })
})

// 计算属性 - 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredNotifications.value.length / pageSize.value) || 1
})

// 计算属性 - 当前页的通知
const paginatedNotifications = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredNotifications.value.slice(start, end)
})

// 监听筛选变化，重置到第一页
watch(filteredNotifications, () => {
  currentPage.value = 1
})

// 生命周期
onMounted(() => {
  loadNotifications()
})

// 方法
const loadNotifications = async () => {
  try {
    loading.value = true
    const notificationList = await notificationApi.getList()
    
    // 转换API格式到前端格式
    const adminNotifications = notificationList.map((n) => ({
      id: n.id,
      title: n.title,
      content: n.content,
      role: mapTargetIdToRole(n.targetId) as 'all' | UserRole,
      priority: mapPriorityIdToPriority(n.priorityId) as 'urgent' | 'normal' | 'low',
      pinned: n.isTop === 1,
      publisher: '数据管理员', // 需要从API获取或从用户信息获取
      publishTime: n.publishTime || n.createTime,
      status: n.publishTime ? 'published' : 'draft',
    })) as AdminNotification[]
    
    notifications.value = adminNotifications
  } catch (error) {
    console.error('加载通知列表失败:', error)
    alert('加载通知列表失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.value = false
  }
}

const openModal = (type: 'add' | 'edit' | 'delete' | 'view', item?: AdminNotification) => {
  currentItem.value = item || null

  if (type === 'add' || type === 'edit') {
    // 初始化编辑表单
    editForm.value = {
      title: item?.title || '',
      content: item?.content || '',
      role: (item?.role || 'all') as 'all' | UserRole,
      priority: (item?.priority || 'normal') as 'urgent' | 'normal' | 'low',
      pinned: item?.pinned || false,
    }
  }

  modal.value = type
}

const closeModal = () => {
  modal.value = null
  currentItem.value = null
  // 重置编辑表单
  editForm.value = {
    title: '',
    content: '',
    role: 'all' as 'all' | UserRole,
    priority: 'normal' as 'urgent' | 'normal' | 'low',
    pinned: false,
  }
}

const handleTogglePin = async (item: AdminNotification) => {
  try {
    await notificationApi.toggleTop(item.id)
    await loadNotifications()
  } catch (error) {
    console.error('切换置顶状态失败:', error)
    alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

const handleSave = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const notificationData = {
      title: editForm.value.title,
      content: editForm.value.content,
      targetId: mapRoleToTargetId(editForm.value.role),
      priorityId: mapPriorityToPriorityId(editForm.value.priority),
      isTop: editForm.value.pinned ? 1 : 0,
      publisherId: userId,
    }

  if (modal.value === 'add') {
      await notificationApi.create(notificationData)
      alert('草稿保存成功')
    } else if (modal.value === 'edit' && currentItem.value) {
      await notificationApi.update({
        id: currentItem.value.id,
        ...notificationData,
      })
      alert('通知更新成功')
    }

    await loadNotifications()
    closeModal()
  } catch (error) {
    console.error('保存通知失败:', error)
    alert('保存失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

const handlePublish = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const notificationData = {
      title: editForm.value.title,
      content: editForm.value.content,
      targetId: mapRoleToTargetId(editForm.value.role),
      priorityId: mapPriorityToPriorityId(editForm.value.priority),
      isTop: editForm.value.pinned ? 1 : 0,
      publisherId: userId,
    }

    if (modal.value === 'add') {
      // 先创建通知
      const newNotification = await notificationApi.create(notificationData)
      // 然后发布
      await notificationApi.publish(newNotification.id)
      alert('通知发布成功')
  } else if (modal.value === 'edit' && currentItem.value) {
      // 先更新通知
      await notificationApi.update({
        id: currentItem.value.id,
        ...notificationData,
      })
      // 如果还没有发布，则发布
      if (currentItem.value.status === 'draft') {
        await notificationApi.publish(currentItem.value.id)
      }
      alert('通知发布成功')
    }

    await loadNotifications()
    closeModal()
  } catch (error) {
    console.error('发布通知失败:', error)
    alert('发布失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

const confirmDelete = async () => {
  if (currentItem.value) {
    try {
      await notificationApi.delete(currentItem.value.id)
      alert('删除成功')
      await loadNotifications()
    } catch (error) {
      console.error('删除通知失败:', error)
      alert('删除失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
  closeModal()
}

// 分页方法
const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

// 映射函数
const mapRoleToTargetId = (role: string): number => {
  const roleMap: Record<string, number> = {
    'all': 1,
    'sys-admin': 2,
    'data-admin': 3,
    'normal-user': 4,
  }
  return roleMap[role] || 1
}

const mapTargetIdToRole = (targetId: number): string => {
  if (targetId === 1) return 'all'
  if (targetId === 2) return 'sys-admin'
  if (targetId === 3) return 'data-admin'
  return 'normal-user'
}

const mapPriorityToPriorityId = (priority: string): number => {
  const priorityMap: Record<string, number> = {
    'urgent': 1, // 紧急
    'normal': 2, // 普通
    'low': 3, // 低
  }
  return priorityMap[priority] || 2
}

const mapPriorityIdToPriority = (priorityId: number): string => {
  if (priorityId === 1) return 'urgent'
  if (priorityId === 2) return 'normal'
  if (priorityId === 3) return 'low'
  return 'normal' // 默认返回普通
}

// 获取角色显示名称
const getRoleDisplayName = (role: string): string => {
  const roleMap: Record<string, string> = {
    'all': '全部',
    'sys-admin': '系统管理员',
    'data-admin': '数据管理员',
    'normal-user': '普通用户',
  }
  return roleMap[role] || role
}

// 获取优先级显示名称
const getPriorityDisplayName = (priority: string): string => {
  const priorityMap: Record<string, string> = {
    'urgent': '紧急',
    'normal': '普通',
    'low': '低',
  }
  return priorityMap[priority] || priority
}

// 获取状态显示名称
const getStatusDisplayName = (status: string): string => {
  const statusMap: Record<string, string> = {
    'published': '已发布',
    'draft': '草稿',
  }
  return statusMap[status] || status
}
</script>

<style scoped>
.btn-effect:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
