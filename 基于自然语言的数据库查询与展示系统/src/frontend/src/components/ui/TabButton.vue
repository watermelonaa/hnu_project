<!--
  @file components/ui/TabButton.vue
  @description 标签页按钮组件

  功能：
  - 标签页切换
  - 激活状态样式
  - 可选未读数量徽章

  @author Frontend Team
-->
<template>
  <button
    @click="$emit('tabChange', tab)"
    :class="`py-2 px-4 text-sm font-medium transition-colors duration-200 relative ${
      activeTab === tab ? 'border-b-2 border-primary text-primary' : 'text-gray-500 hover:text-dark'
    }`"
  >
    {{ label }}
    <span v-if="count !== undefined && count > 0" class="ml-1 text-xs"> ({{ count }}) </span>
  </button>
</template>

<script setup>
defineProps({
  tab: {
    type: String,
    required: true,
  },
  label: {
    type: String,
    required: true,
  },
  count: {
    type: Number,
    default: undefined,
  },
  activeTab: {
    type: String,
    required: true,
  },
})

defineEmits(['tabChange'])
</script>
