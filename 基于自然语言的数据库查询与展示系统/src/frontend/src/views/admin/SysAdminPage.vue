<!--
  @file views/admin/SysAdminPage.vue
  @description 系统管理员主页面容器

  功能：
  - 根据 activePage 路由到对应子页面
  - 管理系统管理员的所有功能模块

  子页面：
  - dashboard: 系统概览
  - user-management: 用户管理
  - system-log: 系统日志
  - llm-config: 大模型配置
  - notification-management: 通知管理
  - account: 我的账户

  @author Frontend Team
-->
<template>
  <div>
    <!-- 根据当前页面渲染不同的组件 -->
    <DashboardPage 
      v-if="activePage === 'dashboard'" 
      :set-active-page="handleSetActivePage"
      @view-abnormal-logs="handleViewAbnormalLogs" 
    />
    <UserManagementPage v-else-if="activePage === 'user-management'" />
    <NotificationManagementPage v-else-if="activePage === 'notification-management'" />
    <SystemLogPage
      v-else-if="activePage === 'system-log'"
      :initial-status-filter="initialLogStatusFilter"
      :clear-initial-filter="clearInitialFilter"
    />
    <LLMConfigPage v-else-if="activePage === 'llm-config'" />
    <AdminAccountPage v-else-if="activePage === 'account'" />
    <SettingsPage v-else-if="activePage === 'settings'" />
    <div v-else class="p-6 text-center text-gray-500">未知页面: {{ activePage }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import DashboardPage from './DashboardPage.vue'
import UserManagementPage from './UserManagementPage.vue'
import NotificationManagementPage from './NotificationManagementPage.vue'
import SystemLogPage from './SystemLogPage.vue'
import LLMConfigPage from './LLMConfigPage.vue'
import AdminAccountPage from './AdminAccountPage.vue'
import SettingsPage from '../SettingsPage.vue'
import type { SysAdminPageType } from '../../types'

// Props
interface SysAdminPageProps {
  activePage: SysAdminPageType
}

const props = defineProps<SysAdminPageProps>()

// Emits
const emit = defineEmits<{
  (e: 'update:active-page', page: SysAdminPageType): void
  (e: 'view-abnormal-logs'): void
}>()

// 响应式数据
const initialLogStatusFilter = ref('')

// 方法
const handleSetActivePage = (page: SysAdminPageType) => {
  emit('update:active-page', page)
}

const handleViewAbnormalLogs = () => {
  initialLogStatusFilter.value = 'failure'
  emit('update:active-page', 'system-log')
}

const clearInitialFilter = () => {
  initialLogStatusFilter.value = ''
}

// 监听页面变化，清除筛选器
watch(
  () => props.activePage,
  (newPage) => {
    if (newPage !== 'system-log') {
      initialLogStatusFilter.value = ''
    }
  },
)
</script>
