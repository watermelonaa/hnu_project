<!--
  @file components/common/SidebarCategory.vue
  @description 侧边栏分类标题组件（可折叠）

  功能：
  - 显示分类标题（如"查询功能"）
  - 支持折叠/展开
  - 带分割线
-->
<template>
  <li class="sidebar-category">
    <div
      class="category-header"
      :class="{ 'is-collapsed': isCollapsed }"
      @click="toggleCollapse"
    >
      <span class="category-title">{{ title }}</span>
      <i
        class="fa category-icon"
        :class="isCollapsed ? 'fa-chevron-down' : 'fa-chevron-up'"
      ></i>
    </div>
    <div class="category-divider"></div>
    <div v-show="!isCollapsed" class="category-content">
      <slot></slot>
    </div>
  </li>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface SidebarCategoryProps {
  title: string
  defaultCollapsed?: boolean
}

const props = withDefaults(defineProps<SidebarCategoryProps>(), {
  defaultCollapsed: false,
})

const isCollapsed = ref(props.defaultCollapsed)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped>
.sidebar-category {
  margin-top: 0.25rem;
}

.sidebar-category:first-child {
  margin-top: 0;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 1.5rem;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}

.category-header:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.category-title {
  font-size: 0.9375rem; /* 15px, 比 text-sm 稍大 */
  font-weight: 600;
  color: #4b5563; /* text-gray-600, 更深一点 */
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.category-icon {
  font-size: 0.75rem;
  color: #9ca3af; /* text-gray-400 */
  transition: transform 0.2s;
}

.category-header.is-collapsed .category-icon {
  transform: rotate(-90deg);
}

.category-divider {
  height: 1px;
  background-color: #e5e7eb; /* border-gray-200 */
  margin: 0 1.5rem;
}

/* 夜间模式样式 */
.dark .category-title {
  color: #a0a0a0 !important;
}

.dark .category-icon {
  color: #808080 !important;
}

.dark .category-header:hover {
  background-color: #3a3a3a !important;
}

.dark .category-divider {
  background-color: #404040 !important;
}

.category-content {
  padding-top: 0.25rem;
}
</style>

