<!--
  @file components/ui/ToastContainer.vue
  @description Toast 通知容器组件

  功能：
  - 显示成功/错误/警告/信息通知
  - 自动消失动画
  - 点击关闭
  - 堆叠显示多条通知

  使用方式：
  在 App.vue 中引入此组件即可全局使用 toast

  @author Frontend Team
-->
<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="item in toasts"
          :key="item.id"
          :class="['toast', `toast-${item.type}`]"
          @click="remove(item.id)"
        >
          <span class="toast-icon">
            <template v-if="item.type === 'success'">✓</template>
            <template v-else-if="item.type === 'error'">✕</template>
            <template v-else-if="item.type === 'warning'">⚠</template>
            <template v-else>ℹ</template>
          </span>
          <span class="toast-message">{{ item.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useToast } from '../../composables/useToast'

const { toasts, remove } = useToast()
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 400px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  background: white;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.15),
    0 0 1px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.toast:hover {
  transform: translateX(-4px);
}

.toast-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.toast-message {
  font-size: 14px;
  line-height: 1.4;
  color: #333;
}

/* 类型样式 */
.toast-success {
  border-left: 4px solid #10b981;
}
.toast-success .toast-icon {
  background: #d1fae5;
  color: #10b981;
}

.toast-error {
  border-left: 4px solid #ef4444;
}
.toast-error .toast-icon {
  background: #fee2e2;
  color: #ef4444;
}

.toast-warning {
  border-left: 4px solid #f59e0b;
}
.toast-warning .toast-icon {
  background: #fef3c7;
  color: #f59e0b;
}

.toast-info {
  border-left: 4px solid #3b82f6;
}
.toast-info .toast-icon {
  background: #dbeafe;
  color: #3b82f6;
}

/* 动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
