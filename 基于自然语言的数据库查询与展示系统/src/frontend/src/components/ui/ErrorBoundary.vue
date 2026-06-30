<!--
  @file components/ui/ErrorBoundary.vue
  @description 错误边界组件

  功能：
  - 捕获子组件渲染错误
  - 显示友好的错误提示
  - 提供重试按钮
  - 防止错误扩散到整个应用

  @author Frontend Team
-->
<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-content">
      <div class="error-icon">⚠️</div>
      <h2 class="error-title">出错了</h2>
      <p class="error-message">{{ errorMessage }}</p>
      <button class="error-button" @click="retry">重试</button>
    </div>
  </div>
  <slot v-else></slot>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { getErrorMessage } from '../../composables/useErrorHandler'

const hasError = ref(false)
const errorMessage = ref('')

onErrorCaptured((error) => {
  hasError.value = true
  errorMessage.value = getErrorMessage(error)
  console.error('ErrorBoundary 捕获错误:', error)
  return false // 阻止错误继续传播
})

const retry = () => {
  hasError.value = false
  errorMessage.value = ''
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 40px;
}

.error-content {
  text-align: center;
  max-width: 400px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.error-message {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 20px;
  line-height: 1.5;
}

.error-button {
  padding: 10px 24px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.error-button:hover {
  background: #2563eb;
}
</style>
