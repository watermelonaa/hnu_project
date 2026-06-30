<!--
  @file views/PlaceholderPage.vue
  @description 占位页面组件

  功能：
  - 显示"功能开发中"提示
  - 用于尚未实现的页面

  @author Frontend Team
-->
<template>
  <section
    class="p-6 space-y-6 overflow-y-auto flex flex-col items-center justify-center h-full text-gray-500"
  >
    <div class="text-center">
      <i class="fa fa-cogs text-6xl mb-4"></i>
      <h2 class="text-2xl font-bold mb-2">{{ title }}</h2>
      <p>此页面正在建设中。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
// 定义 Props
interface PlaceholderPageProps {
  title: string
}

defineProps<PlaceholderPageProps>()
</script>
