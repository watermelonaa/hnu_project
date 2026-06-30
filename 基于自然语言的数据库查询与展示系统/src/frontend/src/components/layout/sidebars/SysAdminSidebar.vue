<!--
  @file components/layout/sidebars/SysAdminSidebar.vue
  @description 系统管理员侧边栏

  功能：
  - 管理功能导航（概览、用户、日志、模型、通知）
  - 登出按钮
  - 基于 BaseSidebar 封装

  @author Frontend Team
-->
<template>
  <BaseSidebar :is-open="isOpen" :is-collapsed="isCollapsed" @logout="handleLogout" @close="handleClose" @toggle="handleToggle">
    <template #header>
      <i class="fa fa-shield text-primary text-2xl"></i>
      <h1 class="text-lg font-bold">系统管理中心</h1>
    </template>

    <SidebarCategory title="系统监控">
    <SidebarItem
      v-for="item in monitoringItems"
      :key="item.href"
      :href="item.href"
      :icon="item.icon"
      :label="item.label"
      :is-active="activePage === item.href"
      @click="handleSetActivePage"
    />
    </SidebarCategory>

    <SidebarCategory title="核心管理">
    <SidebarItem
      v-for="item in managementItems"
      :key="item.href"
      :href="item.href"
      :icon="item.icon"
      :label="item.label"
      :is-active="activePage === item.href"
      @click="handleSetActivePage"
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
      @click="handleSetActivePage"
    />
    </SidebarCategory>
  </BaseSidebar>
</template>

<script setup lang="ts">
import type { SysAdminPageType } from '../../../types'
import BaseSidebar from '../../common/BaseSidebar.vue'
import SidebarItem from '../../common/SidebarItem.vue'
import SidebarCategory from '../../common/SidebarCategory.vue'

// 1. Props/Emits 保持不变
interface SysAdminSidebarProps {
  activePage: SysAdminPageType
  isOpen?: boolean
  isCollapsed?: boolean
}
const props = withDefaults(defineProps<SysAdminSidebarProps>(), {
  isOpen: false,
  isCollapsed: false,
})

const emit = defineEmits<{
  (e: 'update:activePage', page: SysAdminPageType): void
  (e: 'logout'): void
  (e: 'close'): void
  (e: 'toggle'): void
}>()

// 2. 菜单数据
const monitoringItems = [
  { href: 'dashboard' as SysAdminPageType, icon: 'fa-tachometer', label: '仪表盘' },
  { href: 'system-log' as SysAdminPageType, icon: 'fa-history', label: '系统日志' },
]

const managementItems = [
  { href: 'user-management' as SysAdminPageType, icon: 'fa-users', label: '用户管理' },
  { href: 'llm-config' as SysAdminPageType, icon: 'fa-cogs', label: '大模型配置' },
  { href: 'notification-management' as SysAdminPageType, icon: 'fa-bullhorn', label: '通知管理' },
]

const personalItems = [
  { href: 'account' as SysAdminPageType, icon: 'fa-user-circle-o', label: '我的账户' },
  { href: 'settings' as SysAdminPageType, icon: 'fa-cog', label: '设置' },
]

// 3. Methods
const handleSetActivePage = (page: SysAdminPageType) => {
  emit('update:activePage', page)
  // 移动端点击后自动关闭侧边栏
  if (props.isOpen && window.innerWidth < 1024) {
    emit('close')
  }
}

const handleLogout = () => {
  emit('logout') // 转发 BaseSidebar 的 logout 事件
}

const handleClose = () => {
  emit('close')
}

const handleToggle = () => {
  emit('toggle')
}
</script>
