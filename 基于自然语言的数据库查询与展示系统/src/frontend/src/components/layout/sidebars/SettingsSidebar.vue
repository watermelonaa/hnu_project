<!--
  @file components/layout/sidebars/SettingsSidebar.vue
  @description 设置侧边栏

  功能：
  - 主题切换
  - 其他设置选项
-->
<template>
  <aside
    :class="[
      'bg-white shadow-md h-screen flex-shrink-0 flex flex-col transition-all duration-300 border-l border-gray-200 w-80 fixed right-0 top-0 z-[70]',
      isOpen ? 'translate-x-0' : 'translate-x-full',
    ]"
  >
    <!-- Header -->
    <div class="p-4 border-b flex items-center justify-between bg-white flex-shrink-0">
      <div class="flex items-center space-x-2">
        <i class="fa fa-cog text-primary text-2xl"></i>
        <h1 class="text-lg font-bold">设置</h1>
      </div>
      <button
        @click="handleClose"
        class="p-2 text-gray-500 hover:text-primary transition-colors"
        title="关闭设置"
      >
        <i class="fa fa-times text-xl"></i>
      </button>
    </div>

    <!-- 设置内容 -->
    <div class="flex-1 overflow-y-auto p-4 space-y-6">
      <!-- 主题设置 -->
      <div>
        <h3 class="text-sm font-semibold text-gray-700 mb-3">外观设置</h3>
        <div class="space-y-2">
          <button
            @click="handleToggleTheme"
            class="w-full flex items-center justify-between p-3 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <div class="flex items-center space-x-3">
              <i :class="['fa', theme === 'light' ? 'fa-sun-o' : 'fa-moon-o', 'text-primary text-lg']"></i>
              <span class="text-sm font-medium">主题模式</span>
            </div>
            <span class="text-xs text-gray-500">{{ theme === 'light' ? '日间模式' : '夜间模式' }}</span>
          </button>
        </div>
      </div>

      <!-- 其他设置可以在这里添加 -->
      <div>
        <h3 class="text-sm font-semibold text-gray-700 mb-3">其他设置</h3>
        <div class="space-y-2">
          <div class="p-3 bg-gray-50 rounded-lg">
            <p class="text-xs text-gray-500">更多设置功能即将推出...</p>
          </div>
        </div>
      </div>
    </div>
  </aside>

  <!-- 遮罩层 -->
  <div
    v-if="isOpen"
    class="fixed inset-0 bg-black/50 z-[60]"
    @click="handleClose"
  ></div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useTheme } from '../../../composables/useTheme'

interface Props {
  isOpen: boolean
}

interface Emits {
  (e: 'close'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { theme, toggleTheme } = useTheme()

const handleClose = () => {
  emit('close')
}

const handleToggleTheme = () => {
  toggleTheme()
}
</script>

