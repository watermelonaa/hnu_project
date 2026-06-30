<!--
  @file components/ui/Modal.vue
  @description 通用模态框组件

  功能：
  - 可配置标题和关闭按钮
  - 支持遮罩层点击关闭
  - 过渡动画效果
  - 插槽内容自定义

  @author Frontend Team
-->
<template>
  <div v-if="isRendered" :class="modalContainerClass" @click="handleClose">
    <div :class="modalContentClass" @click.stop>
      <div v-if="!hideTitle" class="flex justify-between items-center mb-4">
        <h3 class="font-bold text-lg">{{ title }}</h3>
        <button @click="handleClose" class="text-gray-400 hover:text-gray-600">&times;</button>
      </div>

      <slot></slot>
      
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'

interface ModalProps {
  isOpen: boolean
  title?: string
  hideTitle?: boolean
  contentClassName?: string // 外部自定义样式
}

const props = withDefaults(defineProps<ModalProps>(), {
  hideTitle: false,
  contentClassName: '',
})

const emit = defineEmits<{
  (e: 'close'): void
}>()

// --- 状态管理 ---
const isRendered = ref(props.isOpen)

// 监听 isOpen 变化，控制 isRendered 状态，实现过渡效果
watch(
  () => props.isOpen,
  (newVal) => {
    if (newVal) {
      // 打开时立即渲染
      isRendered.value = true
    } else {
      // 关闭时等待 300ms 过渡时间后，再移除 DOM
      setTimeout(() => {
        isRendered.value = false
      }, 300)
    }
  },
  { immediate: true },
)

// --- 样式计算属性 ---

// 容器样式（背景和居中）
const modalContainerClass = computed(() =>
  [
    'fixed inset-0 bg-black/50 flex flex-col items-center justify-center z-50 transition-opacity duration-300',
    props.isOpen ? 'opacity-100' : 'opacity-0', //
  ].join(' '),
)

// 内容样式（主体，包含过渡效果和默认/自定义宽度）
const modalContentClass = computed(() =>
  [
    'bg-white rounded-xl p-6 w-full mx-4 my-auto transition-all duration-300 max-w-md max-h-[90vh]',
    props.isOpen ? 'scale-100 opacity-100' : 'scale-95 opacity-0', //
    props.contentClassName, //
  ].join(' '),
)

const handleClose = () => {
  emit('close')
}
</script>
