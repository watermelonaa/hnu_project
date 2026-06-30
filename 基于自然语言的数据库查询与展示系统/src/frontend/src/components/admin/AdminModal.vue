<!--
  @file components/admin/AdminModal.vue
  @description 管理员模态框组件

  功能：
  - 通用管理操作弹窗
  - 表单内容插槽
  - 遮罩层点击关闭

  @author Frontend Team
-->
<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
    @click="handleBackdropClick"
  >
    <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 fade-in" :class="isMobile ? 'max-h-none overflow-y-visible' : 'max-h-[95vh] overflow-y-auto'" @click.stop>
      <!-- 标题栏 -->
      <div class="p-4 border-b flex justify-between items-center flex-shrink-0">
        <h3 class="font-bold text-lg">{{ title }}</h3>
        <button @click="handleClose" class="text-gray-400 hover:text-gray-600">
          <i class="fa fa-times"></i>
        </button>
      </div>

      <!-- 内容区域 -->
      <div class="p-6" :class="isMobile ? 'overflow-y-visible' : 'overflow-y-visible'">
        <slot></slot>
      </div>

      <!-- 底部操作区域 -->
      <div v-if="$slots.footer" class="p-4 border-t">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// Props
interface AdminModalProps {
  isOpen: boolean
  title: string
}

defineProps<AdminModalProps>()

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

// Emits
const emit = defineEmits<{
  (e: 'close'): void
}>()

// Methods
const handleClose = () => {
  emit('close')
}

const handleBackdropClick = () => {
  handleClose()
}
</script>

<style scoped>
.fade-in {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
