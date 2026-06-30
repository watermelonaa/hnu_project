<!--
  @file components/layout/sidebars/UserSidebar.vue
  @description 普通用户侧边栏

  功能：
  - 导航菜单（查询、收藏、通知、好友、账户）
  - 登出按钮
  - 基于 BaseSidebar 封装

  @author Frontend Team
-->
<template>
  <BaseSidebar :is-open="isOpen" :is-collapsed="isCollapsed" @logout="handleLogout" @close="handleClose" @toggle="handleToggle">
    <template #header>
      <i class="fa fa-user text-primary text-2xl"></i>
      <h1 class="text-lg font-bold">用户中心</h1>
    </template>

    <SidebarCategory title="查询中心">
    <SidebarItem
      v-for="item in queryItems"
      :key="item.href"
      :href="item.href"
      :icon="item.icon"
      :label="item.label"
      :is-active="activePage === item.href"
      @click="handleSetActivePage"
    />
    </SidebarCategory>

    <SidebarCategory title="个人中心">
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
import type { Page } from '../../../types'
import BaseSidebar from '../../common/BaseSidebar.vue'
import SidebarItem from '../../common/SidebarItem.vue'
import SidebarCategory from '../../common/SidebarCategory.vue'

// 1. Props/Emits 保持不变
interface SidebarProps {
  activePage: Page
  isOpen?: boolean
  isCollapsed?: boolean
}
const props = withDefaults(defineProps<SidebarProps>(), {
  isOpen: false,
  isCollapsed: false,
})

const emit = defineEmits<{
  (e: 'update:active-page', page: Page): void
  (e: 'logout'): void
  (e: 'close'): void
  (e: 'toggle'): void
}>()

// 2. 菜单数据
const queryItems = [
  { href: 'query' as Page, icon: 'fa-search', label: '数据查询' },
  { href: 'history' as Page, icon: 'fa-star', label: '收藏夹' },
]

const personalItems = [
  { href: 'notifications' as Page, icon: 'fa-bell', label: '通知中心' },
  { href: 'friends' as Page, icon: 'fa-users', label: '好友管理' },
  { href: 'account' as Page, icon: 'fa-user', label: '账户管理' },
  { href: 'settings' as Page, icon: 'fa-cog', label: '设置' },
]

// 3. Methods
const handleSetActivePage = (page: Page) => {
  emit('update:active-page', page)
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
