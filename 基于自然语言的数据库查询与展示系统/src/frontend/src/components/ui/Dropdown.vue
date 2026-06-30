<!--
  @file components/ui/Dropdown.vue
  @description 下拉选择组件

  功能：
  - 模型/数据库选择器
  - 支持禁用选项
  - 点击外部自动关闭
  - 自定义选项渲染

  @author Frontend Team
-->
<template>
  <div class="relative" ref="dropdownRef">
    <button
      @click="toggleDropdown"
      :class="[
        'inline-flex items-center justify-center bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg text-sm transition-colors',
        mobileIconOnly ? 'w-10 h-10 md:w-auto md:h-auto md:px-3 md:py-2 md:bg-primary md:text-white md:hover:bg-primary/90' : 'h-10 px-3 py-2',
      ]"
      :title="mobileIconOnly && isMobile ? selected : ''"
    >
      <i :class="['fa', icon, mobileIconOnly ? 'md:mr-2' : 'mr-2', 'text-sm']"></i>
      <span v-if="!mobileIconOnly || !isMobile" class="truncate text-xs md:text-sm max-w-[120px]">{{ selected }}</span>
    </button>

    <div
      v-if="isOpen"
      class="absolute left-0 bottom-full mb-1 w-48 bg-white border border-gray-300 rounded-lg shadow-lg z-10"
    >
      <button
        v-for="option in options"
        :key="option.name"
        @click="handleSelect(option)"
        :disabled="option.disabled"
        :title="option.description"
        :class="[
          'w-full text-left px-4 py-2 text-sm transition-colors',
          // 样式绑定
          selected === option.name ? 'bg-primary/10 text-primary' : '',
          option.disabled ? 'text-gray-400 cursor-not-allowed bg-gray-100' : 'hover:bg-gray-100',
        ]"
      >
        {{ option.name }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
// 确保 ModelOption 类型在您设置的统一类型文件中可用
import type { ModelOption } from '../../types'

// --- 1. Props 和 Emits ---

interface DropdownProps {
  // 显式定义 v-model 对应的 prop
  selected: string
  options: ModelOption[]
  icon: string
  mobileIconOnly?: boolean // 移动端只显示图标
}

const props = withDefaults(defineProps<DropdownProps>(), {
  mobileIconOnly: false,
})

const emit = defineEmits<{
  // 使用 update:selected 来支持 v-model:selected 语法
  (e: 'update:selected', optionName: string): void
  (e: 'select', optionName: string): void // 也可以保留一个独立的 select 事件
}>()

// --- 2. 内部状态 ---
const isOpen = ref(false)
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768 // md breakpoint
}

// --- 3. DOM 引用 ---
const dropdownRef = ref<HTMLDivElement | null>(null)

// --- 4. 外部点击逻辑 ---

// 定义处理函数
const handleClickOutside = (event: MouseEvent) => {
  // 检查点击事件是否发生在下拉框元素外部
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

// 在组件挂载时添加监听器
onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  document.addEventListener('mousedown', handleClickOutside)
})

// 在组件卸载前移除监听器
onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
  document.removeEventListener('mousedown', handleClickOutside)
})

// --- 5. Methods (事件处理) ---

const handleSelect = (option: ModelOption) => {
  if (option.disabled) return

  // 触发更新，支持 v-model
  emit('update:selected', option.name)
  // 也触发 select 事件
  emit('select', option.name)

  isOpen.value = false
}

// 切换下拉框状态
const toggleDropdown = () => {
  isOpen.value = !isOpen.value
}
</script>
