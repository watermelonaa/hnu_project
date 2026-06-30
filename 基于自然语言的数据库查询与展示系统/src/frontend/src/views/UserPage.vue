<!--
  @file views/UserPage.vue
  @description 普通用户主页面容器

  功能：
  - 根据 activePage 路由到对应子页面
  - 管理普通用户的所有功能模块

  子页面：
  - query: 数据查询
  - history: 收藏夹
  - notifications: 通知中心
  - friends: 好友管理
  - account: 账户管理

  @author Frontend Team
-->
<template>
  <!-- 移除多余的div包装，避免与MainLayout和QueryPage的div重叠 -->
  <QueryPage
      v-if="activePage === 'query'"
      ref="queryPageRef"
      @update:title="handleUpdateQueryTitle"
      @toggle-history="handleToggleHistory"
      @new-conversation="handleNewConversation"
    />
    <FavoritesPage
      v-else-if="activePage === 'history'"
      @view-in-chat="handleViewInChat"
      @rerun="handleRerunQuery"
    />
    <NotificationsPage v-else-if="activePage === 'notifications'" />
    <FriendsPageWithRealAPI
      v-else-if="activePage === 'friends'"
      @rerun-query="handleRerunQuery"
    />
    <AccountPage v-else-if="activePage === 'account'" />
    <SettingsPage v-else-if="activePage === 'settings'" />
    <div v-else class="p-6 text-center text-gray-500">未知页面: {{ activePage }}</div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Page } from '../types'
import QueryPage from './QueryPage.vue'
import FavoritesPage from './FavoritesPage.vue'
import AccountPage from './AccountPage.vue'
import FriendsPageWithRealAPI from './FriendsPage.vue'
import NotificationsPage from './NotificationsPage.vue'
import SettingsPage from './SettingsPage.vue'

interface Props {
  activePage: Page
}

interface Emits {
  (e: 'update:query-title', title: string): void
  (e: 'switch-to-query'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// QueryPage ref（用于调用内部方法）
const queryPageRef = ref<InstanceType<typeof QueryPage> | null>(null)

// 处理 QueryPage 标题更新
const handleUpdateQueryTitle = (title: string) => {
  emit('update:query-title', title)
}

// 处理切换历史面板（由 TopHeader 调用）
const handleToggleHistory = () => {
  // 确保在查询页面
  if (props.activePage !== 'query') {
    emit('switch-to-query')
    setTimeout(() => {
      if (queryPageRef.value) {
        queryPageRef.value.toggleHistory()
      }
    }, 100)
  } else if (queryPageRef.value) {
    queryPageRef.value.toggleHistory()
  }
}

// 处理新建对话（由 TopHeader 调用）
const handleNewConversation = () => {
  // 确保在查询页面
  if (props.activePage !== 'query') {
    emit('switch-to-query')
    setTimeout(() => {
      if (queryPageRef.value) {
        queryPageRef.value.handleNewConversation()
      }
    }, 100)
  } else if (queryPageRef.value) {
    queryPageRef.value.handleNewConversation()
  }
}

// 暴露方法给父组件（App.vue）
defineExpose({
  handleToggleHistory,
  handleNewConversation,
})

// 从历史页面查看对话（切换到查询页面）
const handleViewInChat = (conversationId: string) => {
  emit('switch-to-query')
  // 等待 QueryPage 渲染后调用方法
  setTimeout(() => {
    if (queryPageRef.value) {
      queryPageRef.value.handleViewInChat(conversationId)
    }
  }, 100)
}

// 重新执行查询（从历史页面或好友页面）
const handleRerunQuery = (prompt: string, dbConnectionId?: number) => {
  emit('switch-to-query')
  // 等待 QueryPage 渲染后调用方法
  setTimeout(() => {
    if (queryPageRef.value) {
      queryPageRef.value.handleRerunQuery(prompt, dbConnectionId)
    }
  }, 100)
}

// 监听 activePage 变化，确保 QueryPage 已渲染
watch(() => props.activePage, (newPage) => {
  if (newPage === 'query') {
    // QueryPage 已渲染，ref 应该可用
  }
})
</script>
