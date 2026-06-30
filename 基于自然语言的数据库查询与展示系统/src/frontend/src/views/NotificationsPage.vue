<!--
  @file views/NotificationsPage.vue
  @description 通知中心页面

  功能：
  - 显示系统通知列表
  - 通知详情弹窗
  - 标记已读/未读
  - 置顶通知展示

  @author Frontend Team
-->
<template>
  <section
    v-if="loading"
    class="p-6 space-y-6 overflow-y-auto flex items-center justify-center h-full"
  >
    <div class="text-center">
      <div class="dot-flashing"></div>
      <p class="mt-4 text-gray-500">加载通知中...</p>
    </div>
  </section>

  <section v-else class="p-6 space-y-6 overflow-y-auto">
    <div class="flex justify-between items-center">
      <div class="space-x-2">
        <button
          @click="handleMarkAllRead"
          class="px-3 py-1 border border-gray-300 rounded-lg text-sm hover:bg-gray-50 transition-colors"
        >
          全部标记为已读
        </button>
        <button
          @click="handleClearAll"
          class="px-3 py-1 border border-danger/50 text-danger rounded-lg text-sm hover:bg-danger/10 transition-colors"
        >
          清空通知
        </button>
      </div>
    </div>

    <!-- 置顶通知 -->
    <div v-if="pinnedNotifications.length > 0">
      <h3 class="text-sm font-semibold text-gray-500 mb-2 px-2">置顶通知</h3>
      <div class="bg-white rounded-xl shadow-sm">
        <ul class="divide-y divide-gray-200">
          <!-- 直接把通知项的代码写在这里 -->
          <li
            v-for="notification in pinnedNotifications"
            :key="notification.id"
            :class="`p-4 flex items-start space-x-4 transition-colors ${!notification.isRead ? 'bg-primary/5' : 'hover:bg-gray-50'}`"
          >
            <div
              class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center flex-shrink-0 mt-1 relative"
            >
              <!-- 通知图标 -->
              <i v-if="notification.type === 'share'" class="fa fa-share-alt text-primary"></i>
              <i v-if="notification.type === 'system'" class="fa fa-cogs text-secondary"></i>
              <i
                v-if="notification.isPinned"
                class="fa fa-thumb-tack text-xs text-gray-500 absolute -top-1 -right-1 bg-white p-1 rounded-full shadow"
              ></i>
            </div>
            <div class="flex-1">
              <p class="font-semibold text-dark">{{ notification.title }}</p>
              <span class="text-xs text-gray-400 mt-1 block">{{
                formatDate(notification.timestamp)
              }}</span>
            </div>
            <div class="flex items-center space-x-3">
              <button
                @click="handleViewDetail(notification)"
                class="text-indigo-500 hover:text-indigo-600 transition-colors"
                title="查看详情"
              >
                <i class="fa fa-eye"></i>
              </button>
              <button
                @click="handleToggleRead(notification.id)"
                class="text-gray-400 hover:text-primary transition-colors"
                :title="notification.isRead ? '标记为未读' : '标记为已读'"
              >
                <i :class="`fa ${notification.isRead ? 'fa-envelope-open' : 'fa-envelope'}`"></i>
              </button>
              <button
                v-if="!notification.isPinned"
                @click="handleDelete(notification.id)"
                class="text-gray-400 hover:text-danger transition-colors"
                title="删除通知"
              >
                <i class="fa fa-trash"></i>
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <!-- 普通通知 -->
    <div>
      <h3 class="text-sm font-semibold text-gray-500 mb-2 px-2">
        {{ pinnedNotifications.length > 0 ? '普通通知' : '' }}
      </h3>
      <div class="bg-white rounded-xl shadow-sm">
        <ul class="divide-y divide-gray-200">
          <li
            v-for="notification in regularNotifications"
            :key="notification.id"
            v-if="regularNotifications.length > 0"
            :class="`p-4 flex items-start space-x-4 transition-colors ${!notification.isRead ? 'bg-primary/5' : 'hover:bg-gray-50'}`"
          >
            <div
              class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center flex-shrink-0 mt-1 relative"
            >
              <i v-if="notification.type === 'share'" class="fa fa-share-alt text-primary"></i>
              <i v-if="notification.type === 'system'" class="fa fa-cogs text-secondary"></i>
              <i
                v-if="notification.isPinned"
                class="fa fa-thumb-tack text-xs text-gray-500 absolute -top-1 -right-1 bg-white p-1 rounded-full shadow"
              ></i>
            </div>
            <div class="flex-1">
              <p class="font-semibold text-dark">{{ notification.title }}</p>
              <span class="text-xs text-gray-400 mt-1 block">{{
                formatDate(notification.timestamp)
              }}</span>
            </div>
            <div class="flex items-center space-x-3">
              <button
                @click="handleViewDetail(notification)"
                class="text-indigo-500 hover:text-indigo-600 transition-colors"
                title="查看详情"
              >
                <i class="fa fa-eye"></i>
              </button>
              <button
                @click="handleToggleRead(notification.id)"
                class="text-gray-400 hover:text-primary transition-colors"
                :title="notification.isRead ? '标记为未读' : '标记为已读'"
              >
                <i :class="`fa ${notification.isRead ? 'fa-envelope-open' : 'fa-envelope'}`"></i>
              </button>
              <button
                v-if="!notification.isPinned"
                @click="handleDelete(notification.id)"
                class="text-gray-400 hover:text-danger transition-colors"
                title="删除通知"
              >
                <i class="fa fa-trash"></i>
              </button>
            </div>
          </li>
          <div v-else class="text-center text-gray-500 py-16">
            <i class="fa fa-bell-slash-o text-4xl mb-3"></i>
            <p>没有新的通知</p>
          </div>
        </ul>
      </div>
    </div>

    <!-- 通知详情弹窗 -->
    <Modal :is-open="showDetailModal" @close="closeDetailModal" title="通知详情">
      <div v-if="viewingNotification" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">标题</label>
          <p class="text-base text-gray-900">{{ viewingNotification.title }}</p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">内容</label>
          <div class="text-base text-gray-900 whitespace-pre-wrap bg-gray-50 p-3 rounded-lg border border-gray-200 min-h-[100px]">
            {{ viewingNotification.content }}
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">发布时间</label>
          <p class="text-base text-gray-900">{{ formatDate(viewingNotification.timestamp) }}</p>
        </div>
        <div class="flex justify-end space-x-2 pt-4 border-t">
          <button
            @click="closeDetailModal"
            class="px-4 py-2 bg-primary text-white rounded-lg"
          >
            关闭
          </button>
        </div>
      </div>
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal :is-open="showDeleteConfirm" @close="closeDeleteConfirm" title="确认删除">
      <div class="text-center py-4">
        <p class="text-gray-700 mb-6">确定要删除这条通知吗？删除后无法恢复。</p>
        <div class="flex justify-center gap-3">
          <button
            @click="closeDeleteConfirm"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50"
          >
            取消
          </button>
          <button
            @click="handleConfirmDelete"
            class="px-4 py-2 bg-red-500 text-white rounded-lg text-sm hover:bg-red-600"
          >
            确认删除
          </button>
        </div>
      </div>
    </Modal>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import Modal from '../components/ui/Modal.vue'
import { notificationApi } from '../services/api.real'

// 类型定义
interface DisplayNotification {
  id: string
  type: 'share' | 'system'
  title: string
  content: string
  timestamp: string
  isRead: boolean
  isPinned: boolean
}

// 响应式数据
const notifications = ref<DisplayNotification[]>([])
const readStatus = ref<Record<string, boolean>>({})
const loading = ref(true)
const showDeleteConfirm = ref(false)
const deleteTargetId = ref<string | null>(null)
const showDetailModal = ref(false)
const viewingNotification = ref<DisplayNotification | null>(null)

// 计算属性
const pinnedNotifications = computed(() => {
  return notifications.value.filter((n) => n.isPinned)
})

const regularNotifications = computed(() => {
  return notifications.value.filter((n) => !n.isPinned)
})

// 生命周期
onMounted(() => {
  loadNotifications()
})

// 方法
const loadNotifications = async () => {
  try {
    loading.value = true
    // 使用新的API获取用户可见的通知（带已读状态）
    const userNotifications = await notificationApi.getUserNotifications()

    // 转换为显示格式
    const displayNotifications: DisplayNotification[] = userNotifications.map((n) => ({
      id: String(n.id),
      type: n.priorityId === 1 ? 'system' : 'share',
      title: n.title,
      content: n.content,
      timestamp: n.publishTime || n.createTime,
      isRead: n.isRead === 1,
      isPinned: n.isTop === 1,
    }))

    notifications.value = displayNotifications
  } catch (error) {
    console.error('加载通知失败:', error)
    alert('加载通知失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleToggleRead = async (id: string) => {
  try {
    const notification = notifications.value.find((n) => n.id === id)
    if (!notification) return

    const notificationId = Number(id)
    if (notification.isRead) {
      await notificationApi.markAsUnread(notificationId)
    } else {
      await notificationApi.markAsRead(notificationId)
    }

    // 更新本地状态
    notification.isRead = !notification.isRead
  } catch (error) {
    console.error('切换已读状态失败:', error)
    alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

const handleMarkAllRead = async () => {
  try {
    // 标记所有未读通知为已读
    const unreadNotifications = notifications.value.filter((n) => !n.isRead)
    await Promise.all(
      unreadNotifications.map((n) => notificationApi.markAsRead(Number(n.id))),
    )

    // 更新本地状态
    notifications.value = notifications.value.map((n) => ({ ...n, isRead: true }))
  } catch (error) {
    console.error('标记全部已读失败:', error)
    alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

const handleDelete = (id: string) => {
  const notification = notifications.value.find((n) => n.id === id)
  if (!notification) return

  // 检查是否为置顶通知
  if (notification.isPinned) {
    alert('不能删除置顶通知')
    return
  }

  deleteTargetId.value = id
  showDeleteConfirm.value = true
}

const handleConfirmDelete = async () => {
  if (deleteTargetId.value) {
    try {
      await notificationApi.deleteByUser(Number(deleteTargetId.value))
      // 从列表中移除
      notifications.value = notifications.value.filter((n) => n.id !== deleteTargetId.value)
    } catch (error) {
      console.error('删除通知失败:', error)
      alert('删除失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
  closeDeleteConfirm()
}

const handleClearAll = async () => {
  try {
    // 删除所有非置顶通知
    const nonPinnedNotifications = notifications.value.filter((n) => !n.isPinned)
    await Promise.all(
      nonPinnedNotifications.map((n) => notificationApi.deleteByUser(Number(n.id))),
    )

    // 只保留置顶通知
    notifications.value = notifications.value.filter((n) => n.isPinned)
  } catch (error) {
    console.error('清空通知失败:', error)
    alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

const closeDeleteConfirm = () => {
  showDeleteConfirm.value = false
  deleteTargetId.value = null
}

const handleViewDetail = async (notification: DisplayNotification) => {
  viewingNotification.value = notification
  showDetailModal.value = true
  
  // 如果通知未读，查看时自动标记为已读
  if (!notification.isRead) {
    try {
      await notificationApi.markAsRead(Number(notification.id))
      notification.isRead = true
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  viewingNotification.value = null
}

// 格式化日期
const formatDate = (timestamp: string) => {
  return new Date(timestamp).toLocaleString()
}
</script>

<!-- 使用全局 index.css 中的 .dot-flashing 样式 -->
