<!--
  @file components/layout/TopHeader.vue
  @description 系统顶部导航栏

  功能：
  - 显示当前页面/对话标题
  - 用户头像和下拉菜单
  - 通知图标和未读数量
  - 新建对话按钮（查询页面）

  @author Frontend Team
-->
<template>
  <header class="p-4 border-b flex justify-between items-center bg-white flex-shrink-0">
    <div class="flex items-center space-x-2">
      <!-- 移动端和桌面端（侧边栏收起时）汉堡图标 -->
      <button
        v-if="onToggleSidebar"
        @click="onToggleSidebar"
        :class="[
          'p-2 text-gray-500 hover:text-primary transition-colors mr-2',
          isSidebarCollapsed ? '' : 'lg:hidden'
        ]"
        :title="isSidebarCollapsed ? '展开侧边栏' : '打开侧边栏'"
      >
        <i class="fa fa-bars text-xl"></i>
      </button>
      <h1 v-if="currentConversationName" class="text-xl font-bold truncate max-w-xs">
        {{ currentConversationName }}
      </h1>
    </div>

    <div class="flex items-center space-x-4">
      <template v-if="showHistoryToggle">
        <button
          v-if="onNewConversation"
          @click="onNewConversation"
          class="p-2 text-gray-500 hover:text-primary transition-colors"
          title="新对话"
        >
          <i class="fa fa-plus-circle text-xl"></i>
        </button>

        <button
          v-if="onToggleHistory"
          @click="onToggleHistory"
          class="p-2 text-gray-500 hover:text-primary transition-colors"
          title="历史对话"
        >
          <i class="fa fa-history text-xl"></i>
        </button>
      </template>

      <!-- 主题切换按钮 -->
      <button
        @click="toggleTheme"
        class="p-2 text-gray-500 hover:text-primary transition-colors"
        :title="theme === 'light' ? '切换到夜间模式' : '切换到日间模式'"
      >
        <i :class="['fa', theme === 'light' ? 'fa-sun-o' : 'fa-moon-o', 'text-xl']"></i>
      </button>

      <div class="relative" ref="notificationRef">
        <button
          @click="toggleNotification"
          class="p-2 text-gray-500 hover:text-primary transition-colors"
          title="通知中心"
        >
          <i class="fa fa-bell text-xl"></i>
          <span
            v-if="notificationCount > 0"
            class="absolute top-0.5 right-0.5 block h-4 w-4 rounded-full bg-danger text-white text-[10px] flex items-center justify-center ring-2 ring-white"
          >
            {{ notificationCount > 9 ? '9+' : notificationCount }}
          </span>
        </button>

        <div
          v-if="isNotificationOpen"
          class="absolute top-full right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border z-50"
        >
          <div class="p-4 border-b">
            <h3 class="font-bold text-lg">通知中心</h3>
          </div>

          <ul class="divide-y divide-gray-100">
            <li
              v-for="notification in notificationsToShow"
              :key="notification.id"
              class="p-4 flex items-start space-x-3 hover:bg-gray-50"
            >
              <div :class="['mt-1 w-6 text-center', getNotificationDetails(notification).color]">
                <i :class="['fa', getNotificationDetails(notification).icon]"></i>
              </div>
              <div class="flex-1">
                <p class="text-sm text-dark">
                  <template
                    v-for="(part, i) in formatNotificationTitle(notification.title)"
                    :key="i"
                  >
                    <strong v-if="part.type === 'strong'">{{ part.content }}</strong>
                    <span v-else>{{ part.content }}</span>
                  </template>
                </p>
                <p class="text-xs text-gray-400 mt-1">
                  {{ formatTimeAgo(notification.timestamp) }}
                </p>
              </div>
            </li>
          </ul>

          <div class="p-2 border-t text-center">
            <button
              @click="handleViewAll"
              class="w-full text-sm text-primary py-2 hover:bg-primary/10 rounded"
            >
              查看全部通知
            </button>
          </div>
        </div>
      </div>

      <button
        @click="onAvatarClick"
        class="flex items-center space-x-2 text-left transition-colors hover:bg-gray-100 p-1 rounded-lg"
      >
        <template v-if="avatarError || !user.avatarUrl">
          <div
            class="w-9 h-9 rounded-full bg-primary/80 flex items-center justify-center text-sm font-bold text-white"
          >
            <i v-if="!user.name" class="fa fa-user"></i>
            <span v-else>{{ getInitials(user.name) }}</span>
          </div>
        </template>
        <img
          v-else
          :src="user.avatarUrl"
          alt="User Avatar"
          class="w-9 h-9 rounded-full object-cover"
          @error="avatarError = true"
        />

        <div class="hidden md:block">
          <p class="text-sm font-medium">{{ user.name }}</p>
          <p class="text-xs text-gray-500">{{ getRoleName(user.role) }}</p>
        </div>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import type { UserRole, Notification } from '../../types'
import { useTheme } from '../../composables/useTheme'

const { theme, toggleTheme } = useTheme()

// 顶部导航栏组件属性接口
interface User {
  name: string
  role: UserRole | null
  avatarUrl: string
}

interface TopHeaderProps {
  user: User
  notificationCount: number
  notifications: Notification[]
  onNewConversation?: () => void
  onNotificationClick: () => void
  showHistoryToggle?: boolean
  onToggleHistory?: () => void
  onAvatarClick: () => void
  currentConversationName?: string
  onToggleSidebar?: () => void
  isSidebarCollapsed?: boolean
}

const props = defineProps<TopHeaderProps>()

// 通知下拉框显示状态
const isNotificationOpen = ref(false)
// 头像加载失败状态
const avatarError = ref(false)
// 通知下拉框DOM引用
const notificationRef = ref<HTMLDivElement | null>(null)

// 当用户头像URL变化时，重置加载失败状态 (替代 useEffect [user.avatarUrl])
watch(
  () => props.user.avatarUrl,
  (newUrl) => {
    if (newUrl) {
      avatarError.value = false
    }
  },
)

// 点击页面外部关闭通知下拉框 (替代 useEffect [notificationRef])
const handleClickOutside = (event: MouseEvent) => {
  if (
    notificationRef.value &&
    !notificationRef.value.contains(event.target as Node) &&
    isNotificationOpen.value // 只有在打开状态才尝试关闭
  ) {
    isNotificationOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})

// --- 辅助函数 ---

/** 根据用户角色获取角色中文名称 */
const getRoleName = (role: UserRole | null) => {
  if (!role) return ''
  switch (role) {
    case 'sys-admin':
      return '系统管理员'
    case 'data-admin':
      return '数据管理员'
    case 'normal-user':
      return '普通用户'
    default:
      return '用户'
  }
}

/** 格式化通知标题：指定关键词加粗显示 (返回 Vue 支持的 VNode/Fragment) */
const formatNotificationTitle = (title: string) => {
  const keywords = ['王小明', 'Gemini']
  const regex = new RegExp(`(${keywords.join('|')})`, 'g')
  const parts = title.split(regex)

  // 返回一个 VNode 数组，供模板渲染
  return parts.map((part, i) =>
    keywords.includes(part)
      ? { type: 'strong', content: part, key: i }
      : { type: 'text', content: part, key: i },
  )
}

/** 根据通知内容获取对应的图标和颜色 */
const getNotificationDetails = (notification: Notification) => {
  let icon = 'fa-info-circle'
  let color = 'text-primary'

  // 新用户通知：绿色用户添加图标
  if (notification.title.includes('新用户')) {
    icon = 'fa-user-plus'
    color = 'text-green-500'
  }
  // 连接失败通知：红色警告图标
  else if (notification.title.includes('连接失败')) {
    icon = 'fa-exclamation-triangle'
    color = 'text-danger'
  }

  return { icon, color }
}

/** 格式化时间戳为相对时间（如：3秒前、2小时前） */
const formatTimeAgo = (timestamp: string): string => {
  const now = new Date()
  const then = new Date(timestamp)
  const diffInSeconds = Math.round((now.getTime() - then.getTime()) / 1000)

  if (isNaN(diffInSeconds)) return '未知时间'
  if (diffInSeconds < 60) return `${diffInSeconds}秒前`
  const diffInMinutes = Math.round(diffInSeconds / 60)
  if (diffInMinutes < 60) return `${diffInMinutes}分钟前`
  const diffInHours = Math.round(diffInMinutes / 60)
  if (diffInHours < 24) return `${diffInHours}小时前`
  const diffInDays = Math.round(diffInHours / 24)
  return `${diffInDays}天前`
}

/** 获取用户姓名首字母（用于头像占位符） */
const getInitials = (name: string) => {
  if (!name) return '?'
  return name.trim().charAt(0).toUpperCase()
}

/** 查看全部通知：关闭下拉框并触发回调 */
const handleViewAll = () => {
  isNotificationOpen.value = false
  props.onNotificationClick()
}

/** 筛选通知下拉框显示内容：优先显示3条未读通知，不足时补充已读通知 */
const notificationsToShow = computed(() => {
  const notifications = props.notifications
  const list = notifications.filter((n) => !n.isRead).slice(0, 3)

  if (list.length < 3) {
    const readNotifications = notifications.filter((n) => n.isRead)
    list.push(...readNotifications.slice(0, 3 - list.length))
  }
  return list
})

/** 切换通知下拉框状态 */
const toggleNotification = () => {
  isNotificationOpen.value = !isNotificationOpen.value
}
</script>
