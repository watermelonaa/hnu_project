<!--
  @file components/layout/RecommendationLayout.vue
  @description 推荐布局组件
  
  功能：
  - 管理推荐侧边栏的显示（桌面端和移动端）
  - 桌面端：固定在右侧
  - 移动端：滑动侧边栏（覆盖层）
  
  布局说明：
  - 桌面端：右侧固定宽度，无背景色（避免多余的白色div）
  - 移动端：覆盖层模式，从右侧滑出
-->
<template>
  <!-- 桌面端推荐侧边栏 - 固定在右侧，窄宽度只显示按钮 -->
  <div class="hidden lg:block w-20 flex-shrink-0 px-2 py-4 border-l border-gray-200 overflow-hidden">
    <RightSidebar
      :current-conversation="currentConversation"
      @recommendation-click="handleRecommendationClick"
      @open-common="() => $emit('open-common')"
      @open-suggestions="() => $emit('open-suggestions')"
      class="h-full"
    />
  </div>

  <!-- 移动端推荐侧边栏 - 从右侧滑出 -->
  <div
    v-if="isMobileOpen"
    class="lg:hidden fixed top-0 right-0 w-80 h-full bg-white border-l border-gray-200 shadow-lg z-[60] transform transition-transform duration-300"
  >
    <div class="h-full flex flex-col">
      <div class="flex justify-between items-center p-4 border-b">
        <h2 class="text-lg font-bold flex items-center">
          <i class="fa fa-star text-primary mr-2"></i>
          常用推荐
        </h2>
        <button
          @click="$emit('close-mobile')"
          class="text-gray-500 hover:text-gray-700 transition-colors p-2"
        >
          <i class="fa fa-times text-lg"></i>
        </button>
      </div>
      <div class="flex-1 overflow-y-auto">
        <RightSidebar
          :current-conversation="currentConversation"
          @recommendation-click="handleRecommendationClick"
          class="h-full"
        />
      </div>
    </div>
  </div>

  <!-- 移动端推荐侧边栏遮罩层 -->
  <div
    v-if="isMobileOpen"
    @click="$emit('close-mobile')"
    class="lg:hidden fixed inset-0 bg-black/30 z-[50] transition-opacity duration-300"
  ></div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import RightSidebar from './sidebars/QueryRecommendSidebar.vue'
import type { Conversation } from '../../types'

interface Props {
  currentConversation: Conversation | undefined
  isMobileOpen?: boolean
}

interface Emits {
  (e: 'recommendation-click', prompt: string): void
  (e: 'close-mobile'): void
  (e: 'open-common'): void
  (e: 'open-suggestions'): void
}

const props = withDefaults(defineProps<Props>(), {
  isMobileOpen: false,
})

const emit = defineEmits<Emits>()

const handleRecommendationClick = (recommendation: string) => {
  emit('close-mobile') // 移动端点击推荐后关闭侧边栏
  emit('recommendation-click', recommendation)
}
</script>

