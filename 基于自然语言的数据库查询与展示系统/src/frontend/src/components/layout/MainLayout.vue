<!--
  @file components/layout/MainLayout.vue
  @description 主布局组件
  
  功能：
  - 管理侧边栏和 TopHeader 的布局
  - 处理侧边栏的打开/关闭和收起/展开
  - 根据用户角色渲染不同的侧边栏
-->
<template>
  <div :class="['flex h-screen bg-neutral', { 'sidebar-collapsed': isSidebarCollapsed }]">
    <!-- 侧边栏 -->
    <component
      :is="sidebarComponent"
      :active-page="activePage"
      :is-open="isSidebarOpen"
      :is-collapsed="isSidebarCollapsed"
      @update:active-page="handlePageChange"
      @logout="handleLogout"
      @close="isSidebarOpen = false"
      @toggle="isSidebarCollapsed = !isSidebarCollapsed"
    />

    <!-- 主内容区域 -->
    <main class="flex-1 flex flex-col overflow-hidden">
      <TopHeader
        :user="{
          name: currentUser.name,
          role: userRole,
          avatarUrl: currentUser.avatarUrl,
        }"
        :notification-count="unreadCount"
        :notifications="notifications"
        :show-history-toggle="isQueryPage"
        @notification-click="handleNotificationClick"
        @avatar-click="handleAvatarClick"
        @toggle-history="handleToggleHistory"
        @new-conversation="handleNewConversation"
        :current-conversation-name="headerTitle"
        @toggle-sidebar="handleToggleSidebar"
        :is-sidebar-collapsed="isSidebarCollapsed"
      />
      <!-- 内容区域：所有页面自己管理布局，MainLayout不做特殊处理 -->
      <slot></slot>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import type { UserRole, Notification } from '../../types'
import TopHeader from './TopHeader.vue'
import UserSidebar from './sidebars/UserSidebar.vue'
import SysAdminSidebar from './sidebars/SysAdminSidebar.vue'
import DataAdminSidebar from './sidebars/DataAdminSidebar.vue'
import { notificationApi } from '../../services/api.real'

interface Props {
  userRole: UserRole
  currentUser: { name: string; avatarUrl: string }
  activePage: string
  headerTitle: string
  isQueryPage: boolean
}

interface Emits {
  (e: 'update:active-page', page: string): void
  (e: 'logout'): void
  (e: 'notification-click'): void
  (e: 'avatar-click'): void
  (e: 'toggle-history'): void
  (e: 'new-conversation'): void
  (e: 'update:query-title', title: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 侧边栏状态
const isSidebarOpen = ref(false)
const isSidebarCollapsed = ref(false)
const isMobile = ref(false)

// 通知相关状态
const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const isLoadingNotifications = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 1024 // lg breakpoint
}

// 加载通知数据
const loadNotifications = async () => {
  try {
    isLoadingNotifications.value = true
    // 获取用户通知列表
    const userNotifications = await notificationApi.getUserNotifications()
    
    // 转换为前端Notification格式
    const formattedNotifications: Notification[] = userNotifications.map((n) => ({
      id: String(n.id),
      type: n.priorityId === 1 ? 'system' : 'share',
      title: n.title,
      content: n.content,
      timestamp: n.publishTime || n.createTime,
      isRead: n.isRead === 1,
      isPinned: n.isTop === 1,
    }))
    
    notifications.value = formattedNotifications
    
    // 获取未读数量
    unreadCount.value = await notificationApi.getUnreadCount()
  } catch (error) {
    console.error('加载通知失败:', error)
  } finally {
    isLoadingNotifications.value = false
  }
}

// 定期刷新通知（每30秒）
let notificationInterval: number | null = null

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  
  // 初始加载通知
  loadNotifications()
  
  // 设置定时刷新
  notificationInterval = window.setInterval(() => {
    loadNotifications()
  }, 30000) // 30秒刷新一次
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  
  // 清除定时器
  if (notificationInterval !== null) {
    clearInterval(notificationInterval)
  }
})

// 监听通知变化，当用户切换到通知页面时刷新
watch(() => props.activePage, (newPage) => {
  if (newPage === 'notifications') {
    loadNotifications()
  }
})

// 根据角色选择侧边栏组件
const sidebarComponent = computed(() => {
  if (props.userRole === 'sys-admin') return SysAdminSidebar
  if (props.userRole === 'data-admin') return DataAdminSidebar
  return UserSidebar
})

// 事件处理
const handlePageChange = (page: string) => {
  emit('update:active-page', page)
}

const handleLogout = () => {
  emit('logout')
}

const handleToggleSidebar = () => {
  if (isMobile.value) {
    isSidebarOpen.value = !isSidebarOpen.value
  } else {
    isSidebarCollapsed.value = !isSidebarCollapsed.value
  }
}

const handleNotificationClick = () => {
  // 刷新通知数据
  loadNotifications()
  emit('notification-click')
}

const handleAvatarClick = () => {
  emit('avatar-click')
}

const handleToggleHistory = () => {
  emit('toggle-history')
}

const handleNewConversation = () => {
  emit('new-conversation')
}
</script>

