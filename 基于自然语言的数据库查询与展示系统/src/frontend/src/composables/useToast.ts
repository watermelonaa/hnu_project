/**
 * @file composables/useToast.ts
 * @description Toast 通知组合式函数
 *
 * 功能：
 * - 显示成功/错误/警告/信息通知
 * - 支持自动消失（可配置时长）
 * - 支持手动关闭
 * - 全局单例状态管理
 *
 * @example
 * // 方式一：使用 composable
 * const { success, error } = useToast()
 * success('保存成功')
 * error('操作失败')
 *
 * // 方式二：直接使用 toast 单例
 * import { toast } from '@/composables/useToast'
 * toast.success('保存成功')
 *
 * @author Frontend Team
 * @since 1.0.0
 */
import { ref, readonly } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastMessage {
  id: number
  type: ToastType
  message: string
  duration: number
}

// 全局状态
const toasts = ref<ToastMessage[]>([])
let toastId = 0

/**
 * 显示 Toast 消息
 */
const show = (message: string, type: ToastType = 'info', duration: number = 3000) => {
  const id = ++toastId
  const toast: ToastMessage = { id, type, message, duration }
  toasts.value.push(toast)

  if (duration > 0) {
    setTimeout(() => {
      remove(id)
    }, duration)
  }

  return id
}

/**
 * 移除 Toast 消息
 */
const remove = (id: number) => {
  const index = toasts.value.findIndex((t) => t.id === id)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

/**
 * 清除所有 Toast
 */
const clear = () => {
  toasts.value = []
}

// 便捷方法
const success = (message: string, duration?: number) => show(message, 'success', duration)
const error = (message: string, duration?: number) => show(message, 'error', duration ?? 5000)
const warning = (message: string, duration?: number) => show(message, 'warning', duration)
const info = (message: string, duration?: number) => show(message, 'info', duration)

export function useToast() {
  return {
    toasts: readonly(toasts),
    show,
    remove,
    clear,
    success,
    error,
    warning,
    info,
  }
}

// 导出单例供全局使用
export const toast = {
  show,
  remove,
  clear,
  success,
  error,
  warning,
  info,
}
