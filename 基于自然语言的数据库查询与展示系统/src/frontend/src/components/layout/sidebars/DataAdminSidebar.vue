<!--
  @file components/layout/sidebars/DataAdminSidebar.vue
  @description 数据管理员侧边栏

  功能：
  - 数据管理导航（数据源、权限、日志等）
  - 查询功能导航
  - 登出按钮
  - 基于 BaseSidebar 封装

  @author Frontend Team
-->
<template>
  <BaseSidebar :is-open="isOpen" :is-collapsed="isCollapsed" @logout="handleLogout" @close="handleClose" @toggle="handleToggle">
    <template #header>
      <i class="fa fa-database text-primary text-2xl"></i>
      <h1 class="text-lg font-bold">数据管理中心</h1>
    </template>

    <SidebarCategory title="查询功能">
    <SidebarItem
      v-for="item in queryItems"
      :key="item.href"
      :href="item.href"
      :icon="item.icon"
      :label="item.label"
      :is-active="activePage === item.href"
      @click="handleItemClick"
    />
    </SidebarCategory>

    <SidebarCategory title="管理功能">
    <SidebarItem
      v-for="item in managementItems"
      :key="item.href"
      :href="item.href"
      :icon="item.icon"
      :label="item.label"
      :is-active="activePage === item.href"
      @click="handleItemClick"
    />
    </SidebarCategory>

    <SidebarCategory title="个人设置">
    <SidebarItem
      v-for="item in personalItems"
      :key="item.href"
      :href="item.href"
      :icon="item.icon"
      :label="item.label"
      :is-active="activePage === item.href"
      @click="handleItemClick"
    />
    </SidebarCategory>
  </BaseSidebar>
</template>

<script setup lang="ts">
import BaseSidebar from '../../common/BaseSidebar.vue'
import SidebarItem from '../../common/SidebarItem.vue'
import SidebarCategory from '../../common/SidebarCategory.vue'
import type { DataAdminPageType } from '../../../types'

interface DataAdminSidebarProps {
  activePage: DataAdminPageType | string
  onLogout: () => void
  isOpen?: boolean
  isCollapsed?: boolean
}

const props = withDefaults(defineProps<DataAdminSidebarProps>(), {
  isOpen: false,
  isCollapsed: false,
})

const emit = defineEmits<{
  (e: 'update:activePage', page: DataAdminPageType | string): void
  (e: 'close'): void
  (e: 'toggle'): void
}>()

const queryItems = [
  { href: 'query' as DataAdminPageType, icon: 'fa-search', label: '数据查询' },
  { href: 'history' as DataAdminPageType, icon: 'fa-star', label: '收藏夹' },
]

const managementItems = [
  { href: 'dashboard' as DataAdminPageType, icon: 'fa-tachometer', label: '仪表盘' },
  { href: 'datasource' as DataAdminPageType, icon: 'fa-plug', label: '数据源管理' },
  { href: 'user-permission' as DataAdminPageType, icon: 'fa-key', label: '用户权限管理' },
  { href: 'notification-management' as DataAdminPageType, icon: 'fa-bullhorn', label: '通知管理' },
  { href: 'connection-log' as DataAdminPageType, icon: 'fa-link', label: '连接日志' },
]

const personalItems = [
  { href: 'notifications' as DataAdminPageType, icon: 'fa-bell', label: '通知中心' },
  { href: 'account' as DataAdminPageType, icon: 'fa-user', label: '账户管理' },
  { href: 'friends' as DataAdminPageType, icon: 'fa-users', label: '好友管理' },
  { href: 'settings' as DataAdminPageType, icon: 'fa-cog', label: '设置' },
]

const handleItemClick = (page: DataAdminPageType | string) => {
  emit('update:activePage', page)
  // 移动端点击后自动关闭侧边栏
  if (props.isOpen && window.innerWidth < 1024) {
    emit('close')
  }
}

const handleLogout = () => {
  props.onLogout() // 继续调用上层传入的 onLogout Prop
}

const handleClose = () => {
  emit('close')
}

const handleToggle = () => {
  emit('toggle')
}
</script>
