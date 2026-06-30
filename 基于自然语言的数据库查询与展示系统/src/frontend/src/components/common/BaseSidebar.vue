<!--
  @file components/common/BaseSidebar.vue
  @description 基础侧边栏组件

  功能：
  - 提供侧边栏基础布局
  - 插槽支持自定义内容
  - 登出按钮
  - 其他侧边栏的基础组件

  @author Frontend Team
-->
<template>
  <!-- 移动端遮罩层 -->
  <div
    v-if="isMobile && isOpen"
  ></div>

  <!-- 移动端侧边栏（覆盖模式） -->
  <aside
    v-if="isMobile"
    v-show="isOpen"
    :class="[
      'bg-white shadow-md h-screen flex flex-col border-r border-gray-200 fixed left-0 top-0 z-[60] w-64',
    ]"
  >
    <!-- Header区域，与TopHeader对齐的灰色分隔线，但不与TopHeader连在一起 -->
    <div v-if="$slots.header" class="p-4 border-b flex items-center justify-between bg-white flex-shrink-0">
      <div class="flex items-center space-x-2">
        <slot name="header"></slot>
      </div>
      <!-- 关闭按钮（汉堡图标，移动端显示） -->
      <button
        @click="handleClose"
        class="p-2 text-gray-500 hover:text-primary transition-colors"
        title="关闭侧边栏"
      >
        <i class="fa fa-bars text-xl"></i>
      </button>
    </div>

    <nav class="py-2 flex-grow overflow-y-auto">
      <ul>
        <slot></slot>
      </ul>
    </nav>

    <div class="border-t border-gray-200 p-4">
      <button
        @click="handleLogout"
        class="flex items-center justify-center text-gray-600 hover:text-danger transition-colors w-full text-base font-medium py-3"
      >
        <i class="fa fa-sign-out w-6 text-lg"></i>
        <span>退出登录</span>
      </button>
    </div>
  </aside>

  <!-- 桌面端侧边栏（正常布局） -->
  <aside
    v-if="!isMobile"
    :class="[
      'bg-white shadow-md h-screen flex-shrink-0 flex flex-col transition-all duration-300 border-r border-gray-200',
      isCollapsed ? 'w-0 overflow-hidden hidden lg:block' : 'w-64 hidden lg:flex',
    ]"
  >
    <!-- Header区域，与TopHeader对齐的灰色分隔线，但不与TopHeader连在一起 -->
    <div v-if="$slots.header" class="p-4 border-b flex items-center justify-between bg-white flex-shrink-0">
      <div class="flex items-center space-x-2">
      <slot name="header"></slot>
      </div>
      <!-- 收起按钮（汉堡图标，仅桌面端显示） -->
      <button
        @click="handleToggle"
        class="p-2 text-gray-500 hover:text-primary transition-colors"
        :title="isCollapsed ? '展开侧边栏' : '收起侧边栏'"
      >
        <i class="fa fa-bars text-xl"></i>
      </button>
    </div>

    <nav class="py-2 flex-grow overflow-y-auto">
      <ul>
        <slot></slot>
      </ul>
    </nav>

    <div class="border-t border-gray-200 p-4">
      <button
        @click="handleLogout"
        class="flex items-center justify-center text-gray-600 hover:text-danger transition-colors w-full text-base font-medium py-3"
      >
        <i class="fa fa-sign-out w-6 text-lg"></i>
        <span>退出登录</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface BaseSidebarProps {
  isOpen?: boolean
  isCollapsed?: boolean
}

const props = withDefaults(defineProps<BaseSidebarProps>(), {
  isOpen: false,
  isCollapsed: false,
})

// Emits
const emit = defineEmits<{
  (e: 'logout'): void
  (e: 'close'): void
  (e: 'toggle'): void
}>()

const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 1024 // lg breakpoint
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const handleLogout = () => {
  emit('logout')
}

const handleClose = () => {
  emit('close')
}

const handleToggle = () => {
  if (isMobile.value) {
    emit('close')
  } else {
    emit('toggle')
  }
}
</script>
