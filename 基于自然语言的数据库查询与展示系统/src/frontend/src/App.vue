<!--
  @file App.vue
  @description 应用程序根组件

  ===== 核心职责 =====
  1. 根据用户角色渲染对应的 MainLayout 和页面组件
  2. 管理全局状态（用户信息、角色、页面路由）
  3. 处理页面导航和事件转发（不处理具体业务逻辑）

  ===== 架构设计原则 =====
  - App.vue 只负责路由和状态管理，不包含具体页面逻辑
  - 具体页面逻辑在各自的 Page 组件中（UserPage、DataAdminPage、SysAdminPage）
  - QueryPage 的状态管理已移至 QueryPage.vue 内部
  - MainLayout 负责布局（侧边栏、TopHeader、内容区域）

  ===== 用户角色与页面映射 =====
  - sys-admin: 系统管理员
    * 页面：dashboard, user-management, system-log, llm-config, notification-management, account, settings
    * 无查询功能，无历史面板
  
  - data-admin: 数据管理员
    * 页面：dashboard, query, history, datasource, user-permission, notification-management, 
            connection-log, notifications, account, friends, settings
    * 有查询功能，有历史面板
  
  - normal-user: 普通用户
    * 页面：query, history, notifications, friends, account, settings
    * 有查询功能，有历史面板

  ===== 状态管理说明 =====
  - userRole: 当前用户角色（null 表示未登录）
  - currentUser: 当前用户基本信息（name, avatarUrl）
  - activePage: 普通用户的当前页面（Page 类型）
  - activeSysAdminPage: 系统管理员的当前页面（SysAdminPageType 类型）
  - activeDataAdminPage: 数据管理员的当前页面（DataAdminPageType 类型）
  - queryPageTitle: 查询页面的动态标题（由 QueryPage 通过事件更新）

  ===== 事件流转说明 =====
  1. 登录：LoginPage @login -> handleLogin -> loadUserInfo
  2. 页面切换：Sidebar @update:active-page -> setActivePage/setActiveSysAdminPage/setActiveDataAdminPage
  3. 查询标题更新：QueryPage @update:title -> handleUpdateQueryTitle -> queryPageTitle
  4. 通知点击：TopHeader @notification-click -> handleNotificationClick -> 切换到 notifications 页面
  5. 头像点击：TopHeader @avatar-click -> handleAvatarClick -> 切换到 account 页面
  6. 新对话：TopHeader @new-conversation -> handleNewConversation -> 切换到 query 页面
  7. 历史面板：TopHeader @toggle-history -> handleToggleHistory -> 转发到 QueryPage（空实现，逻辑在子组件）

  @author Frontend Team
  @since 1.0.0
-->
<template>
  <!-- 未登录状态 - 登录页面 -->
  <LoginPage v-if="!userRole" @login="handleLogin" />

  <!-- 退出登录确认弹窗 -->
  <div
    v-if="showLogoutConfirm"
    class="fixed inset-0 bg-black/50 flex items-center justify-center z-[9999]"
    @click="showLogoutConfirm = false"
  >
    <div
      class="bg-white rounded-xl shadow-xl w-full max-w-md p-6 mx-4"
      @click.stop
    >
      <h3 class="text-xl font-bold mb-4">确认退出</h3>
      <p class="text-gray-600 mb-6">确定要退出登录吗？</p>
      <div class="flex justify-end space-x-3">
        <button
          @click="showLogoutConfirm = false"
          class="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
        >
          取消
        </button>
        <button
          @click="confirmLogout"
          class="px-6 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
        >
          退出登录
        </button>
      </div>
    </div>
  </div>

  <!-- 系统管理员页面 -->
  <MainLayout
    v-else-if="userRole === 'sys-admin'"
    :user-role="userRole"
    :current-user="currentUser"
    :active-page="activeSysAdminPage"
    :header-title="headerTitle"
    :is-query-page="false"
    @update:active-page="setActiveSysAdminPage"
    @logout="handleLogout"
    @notification-click="handleNotificationClick"
    @avatar-click="handleAvatarClick"
  >
    <SysAdminPage :active-page="activeSysAdminPage" @update:active-page="setActiveSysAdminPage" />
  </MainLayout>

  <!-- 数据管理员页面 -->
  <MainLayout
    v-else-if="userRole === 'data-admin'"
    :user-role="userRole"
    :current-user="currentUser"
    :active-page="activeDataAdminPage"
    :header-title="headerTitle"
    :is-query-page="activeDataAdminPage === 'query'"
    @update:active-page="setActiveDataAdminPage"
    @logout="handleLogout"
    @notification-click="handleNotificationClick"
    @avatar-click="handleAvatarClick"
    @toggle-history="handleToggleHistory"
    @new-conversation="handleNewConversation"
    @update:query-title="handleUpdateQueryTitle"
  >
    <DataAdminPage
      ref="dataAdminPageRef"
      :active-page="activeDataAdminPage"
      :set-active-page="setActiveDataAdminPage"
      @update:query-title="handleUpdateQueryTitle"
      @switch-to-query="activeDataAdminPage = 'query'"
    />
  </MainLayout>

  <!-- 普通用户页面 -->
  <MainLayout
    v-else-if="userRole === 'normal-user'"
    :user-role="userRole"
    :current-user="currentUser"
    :active-page="activePage"
    :header-title="headerTitle"
    :is-query-page="activePage === 'query'"
    @update:active-page="setActivePage"
    @logout="handleLogout"
    @notification-click="handleNotificationClick"
    @avatar-click="handleAvatarClick"
    @toggle-history="handleToggleHistory"
    @new-conversation="handleNewConversation"
    @update:query-title="handleUpdateQueryTitle"
  >
    <UserPage
      ref="userPageRef"
      :active-page="activePage"
      @update:query-title="handleUpdateQueryTitle"
      @switch-to-query="activePage = 'query'"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  type UserRole,
  type Page,
  type SysAdminPageType,
  type DataAdminPageType,
} from './types'

// 导入页面组件
import LoginPage from './views/LoginPage.vue'
import UserPage from './views/UserPage.vue'
import SysAdminPage from './views/admin/SysAdminPage.vue'
import DataAdminPage from './views/DataAdminPage.vue'
import MainLayout from './components/layout/MainLayout.vue'

// 导入 API
import { userApi } from './services/api.real'
import { authApi } from './services/api/auth'

/**
 * 侧边栏页面名称映射（用于显示 TopHeader 标题）
 * 
 * 逻辑说明：
 * - 查询页面（query）不使用此函数，使用 queryPageTitle
 * - 其他页面根据角色和页面 key 返回对应的中文名称
 * - 如果找不到对应页面，返回空字符串
 */
const getSidebarPageName = (role: UserRole | null, page: string) => {
  const normalUserPages: Record<string, string> = {
    history: '收藏夹',
    notifications: '通知中心',
    friends: '好友管理',
    account: '账户管理',
    settings: '设置',
  }
  
  const dataAdminPages: Record<string, string> = {
    history: '收藏夹',
    datasource: '数据源管理',
    dashboard: '数据源概览',
    'user-permission': '用户权限管理',
    'notification-management': '通知管理（数据员）',
    'connection-log': '数据源连接日志',
    notifications: '通知中心',
    account: '我的账户',
    friends: '好友管理',
    settings: '设置',
  }
  
  const sysAdminPages: Record<string, string> = {
    dashboard: '系统概览',
    'user-management': '用户管理',
    account: '我的账户',
    'system-log': '系统日志',
    'llm-config': '大模型配置',
    'notification-management': '通知管理',
    settings: '设置',
  }
  
  if (role === 'normal-user') {
    return normalUserPages[page] || ''
  } else if (role === 'data-admin') {
    return dataAdminPages[page] || ''
  } else if (role === 'sys-admin') {
    return sysAdminPages[page] || ''
  }
  return ''
}

// ===== 全局核心状态 =====
/** 当前用户角色，null 表示未登录 */
const userRole = ref<UserRole | null>(null)

/** 退出登录确认弹窗显示状态 */
const showLogoutConfirm = ref(false)

/** 当前用户基本信息 */
const currentUser = ref({ name: '', avatarUrl: '' })

// 页面容器 ref（用于调用子组件方法）
const userPageRef = ref<InstanceType<typeof UserPage> | null>(null)
const dataAdminPageRef = ref<InstanceType<typeof DataAdminPage> | null>(null)

// ===== 页面状态管理 =====
/** 普通用户的当前页面，默认为查询页面 */
const activePage = ref<Page>('query')

/** 系统管理员的当前页面，默认为仪表盘 */
const activeSysAdminPage = ref<SysAdminPageType>('dashboard')

/** 数据管理员的当前页面，默认为仪表盘 */
const activeDataAdminPage = ref<DataAdminPageType>('dashboard')

/** 查询页面的动态标题，由 QueryPage 通过 @update:title 事件更新 */
const queryPageTitle = ref<string>('新对话')

// ===== 计算属性 =====
/**
 * 顶部导航栏标题
 * 
 * 逻辑说明：
 * 1. 查询页面（query）：使用 queryPageTitle（由 QueryPage 动态更新）
 * 2. 其他页面：使用 getSidebarPageName 根据角色和页面 key 获取中文名称
 * 3. 未登录或未知页面：返回空字符串
 */
const headerTitle = computed(() => {
  // 判断是否为查询页面
  const isQueryPage =
    (userRole.value === 'normal-user' && activePage.value === 'query') ||
    (userRole.value === 'data-admin' && activeDataAdminPage.value === 'query')

  // 查询页面使用动态标题
  if (isQueryPage) {
    return queryPageTitle.value
  }

  // 其他页面使用侧边栏页面名称映射
  if (userRole.value === 'normal-user') {
    return getSidebarPageName(userRole.value, activePage.value)
  } else if (userRole.value === 'data-admin') {
    return getSidebarPageName(userRole.value, activeDataAdminPage.value)
  } else if (userRole.value === 'sys-admin') {
    return getSidebarPageName(userRole.value, activeSysAdminPage.value)
  }
  
  return ''
})

// ===== 事件处理函数 =====

/**
 * 登录处理
 * 
 * 逻辑说明：
 * 1. 优先使用传入的 role，否则从 sessionStorage 读取
 * 2. 设置 userRole 后加载用户信息
 * 3. 登录成功后，页面状态保持默认值（query 或 dashboard）
 */
const handleLogin = async (role: UserRole) => {
  const savedRole = sessionStorage.getItem('userRole') as UserRole
  const roleToSet = role || savedRole
  if (roleToSet) {
    userRole.value = roleToSet
    await loadUserInfo()
  }
}

/**
 * 加载用户信息
 * 
 * 逻辑说明：
 * 1. 从 sessionStorage 获取 userId
 * 2. 调用 API 获取用户数据
 * 3. 更新 currentUser（name, avatarUrl）
 * 4. 失败时使用默认值
 */
const loadUserInfo = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const userData = await userApi.getById(userId)
    currentUser.value = {
      name: userData.username,
      avatarUrl: userData.avatarUrl || '/default-avatar.png',
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    currentUser.value = { name: '用户', avatarUrl: '/default-avatar.png' }
  }
}

/**
 * 退出登录
 * 
 * 逻辑说明：
 * 1. 显示确认对话框
 * 2. 用户确认后调用后端登出接口，将token加入黑名单
 * 3. 清除 sessionStorage 中的用户信息
 * 4. 重置 userRole 为 null（触发显示 LoginPage）
 * 5. 重置所有页面状态为默认值
 */
const handleLogout = () => {
  // 显示确认弹窗
  showLogoutConfirm.value = true
}

const confirmLogout = async () => {
  // 隐藏确认弹窗
  showLogoutConfirm.value = false

  try {
    // 调用后端登出接口
    await authApi.logout()
  } catch (error) {
    console.error('登出失败:', error)
  } finally {
    // 无论后端登出是否成功，都清除本地状态
    userRole.value = null
    activePage.value = 'query'
    activeSysAdminPage.value = 'dashboard'
    activeDataAdminPage.value = 'dashboard'
  }
}

/**
 * 更新 QueryPage 标题
 * 
 * 逻辑说明：
 * - 由 QueryPage 通过 @update:title 事件调用
 * - 更新 queryPageTitle，用于 headerTitle 计算属性
 */
const handleUpdateQueryTitle = (title: string) => {
  queryPageTitle.value = title
}

/**
 * 创建新对话
 * 
 * 逻辑说明：
 * - 由 TopHeader 的"新对话"按钮触发
 * - 切换到查询页面（query）
 * - 实际的新对话逻辑在 QueryPage 内部处理
 */
const handleNewConversation = () => {
  if (userRole.value === 'normal-user') {
    activePage.value = 'query'
    // 等待页面切换后调用新对话方法
    setTimeout(() => {
      if (userPageRef.value) {
        userPageRef.value.handleNewConversation()
      }
    }, 100)
  } else if (userRole.value === 'data-admin') {
    activeDataAdminPage.value = 'query'
    // 等待页面切换后调用新对话方法
    setTimeout(() => {
      if (dataAdminPageRef.value) {
        dataAdminPageRef.value.handleNewConversation()
      }
    }, 100)
  }
  // sys-admin 没有查询功能，不需要处理
}

/**
 * 通知图标点击
 * 
 * 逻辑说明：
 * - 由 TopHeader 的通知图标触发
 * - 切换到对应角色的通知页面（notifications）
 */
const handleNotificationClick = () => {
  if (userRole.value === 'normal-user') {
    activePage.value = 'notifications'
  } else if (userRole.value === 'data-admin') {
    activeDataAdminPage.value = 'notifications'
  }
  // sys-admin 没有通知中心页面，不需要处理
}

/**
 * 头像点击
 * 
 * 逻辑说明：
 * - 由 TopHeader 的头像触发
 * - 切换到对应角色的账户页面（account）
 */
const handleAvatarClick = () => {
  if (userRole.value === 'normal-user') {
    activePage.value = 'account'
  } else if (userRole.value === 'data-admin') {
    activeDataAdminPage.value = 'account'
  } else if (userRole.value === 'sys-admin') {
    activeSysAdminPage.value = 'account'
  }
}

/**
 * 切换历史面板
 * 
 * 逻辑说明：
 * - 由 TopHeader 的历史按钮触发（仅在查询页面显示）
 * - 转发到 UserPage/DataAdminPage，然后转发到 QueryPage
 */
const handleToggleHistory = () => {
  if (userRole.value === 'normal-user') {
    // 如果不在查询页面，先切换到查询页面
    if (activePage.value !== 'query') {
      activePage.value = 'query'
      setTimeout(() => {
        if (userPageRef.value) {
          userPageRef.value.handleToggleHistory()
        }
      }, 100)
    } else if (userPageRef.value) {
      userPageRef.value.handleToggleHistory()
    }
  } else if (userRole.value === 'data-admin') {
    // 如果不在查询页面，先切换到查询页面
    if (activeDataAdminPage.value !== 'query') {
      activeDataAdminPage.value = 'query'
      setTimeout(() => {
        if (dataAdminPageRef.value) {
          dataAdminPageRef.value.handleToggleHistory()
        }
      }, 100)
    } else if (dataAdminPageRef.value) {
      dataAdminPageRef.value.handleToggleHistory()
    }
  }
}


// ===== 页面设置函数 =====
/**
 * 设置普通用户页面
 * - 由 Sidebar 的 @update:active-page 事件触发
 */
const setActivePage = (page: Page) => {
  activePage.value = page
}

/**
 * 设置系统管理员页面
 * - 由 Sidebar 的 @update:active-page 事件触发
 */
const setActiveSysAdminPage = (page: SysAdminPageType) => {
  activeSysAdminPage.value = page
}

/**
 * 设置数据管理员页面
 * - 由 Sidebar 的 @update:active-page 事件触发
 */
const setActiveDataAdminPage = (page: DataAdminPageType) => {
  activeDataAdminPage.value = page
}

/**
 * 处理头像更新事件
 * 
 * 逻辑说明：
 * - 由 AccountPage/AdminAccountPage 通过自定义事件触发
 * - 更新 currentUser 的 avatarUrl
 */
const handleAvatarUpdate = (event: CustomEvent) => {
  currentUser.value = {
    ...currentUser.value,
    avatarUrl: event.detail.avatarUrl,
  }
}

// ===== 生命周期 =====
onMounted(() => {
  // 初始化登录：从 sessionStorage 恢复登录状态
  const savedRole = sessionStorage.getItem('userRole') as UserRole
  if (savedRole) {
    handleLogin(savedRole)
  }

  // 监听头像更新事件（由 AccountPage/AdminAccountPage 触发）
  window.addEventListener('userAvatarUpdated', handleAvatarUpdate as EventListener)
})

onUnmounted(() => {
  // 清理事件监听器
  window.removeEventListener('userAvatarUpdated', handleAvatarUpdate as EventListener)
})
</script>

<style scoped>
/* 确保组件正确布局 */
.h-screen {
  height: 100vh;
}

.bg-neutral {
  background-color: #f5f5f5;
}

.flex {
  display: flex;
}

.flex-1 {
  flex: 1 1 0%;
}

.flex-col {
  flex-direction: column;
}

.overflow-hidden {
  overflow: hidden;
}
</style>
