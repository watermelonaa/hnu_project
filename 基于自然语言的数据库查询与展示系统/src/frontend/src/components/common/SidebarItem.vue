<!--
  @file components/common/SidebarItem.vue
  @description 侧边栏菜单项组件

  功能：
  - 导航链接渲染
  - 激活状态样式
  - 图标和文本展示

  @author Frontend Team
-->
<template>
  <li>
    <a
      :href="`#${href}`"
      :class="[
        'flex items-center px-6 py-3 text-base transition-colors duration-200',
        isActive ? activeClass : inactiveClass,
      ]"
      @click.prevent="handleClick"
    >
      <i :class="['fa', icon, 'w-6']"></i>
      <span>{{ label }}</span>
    </a>
  </li>
</template>

<script setup lang="ts">
// 使用泛型支持不同类型的页面
interface SidebarItemProps<T extends string> {
  href: T
  icon: string
  label: string
  isActive: boolean
}

// 定义泛型 props
const props = defineProps<SidebarItemProps<any>>()

// Emits
const emit = defineEmits<{
  (e: 'click', page: any): void
}>()

const handleClick = () => {
  emit('click', props.href)
}

const activeClass = 'bg-primary/10 text-primary border-l-4 border-primary'
const inactiveClass = 'text-gray-600 hover:bg-gray-50'
</script>
