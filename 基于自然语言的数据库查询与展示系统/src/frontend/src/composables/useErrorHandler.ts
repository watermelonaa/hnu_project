/**
 * @file composables/useErrorHandler.ts
 * @description 统一错误处理组合式函数
 *
 * 功能：
 * - 统一提取错误消息
 * - 自动显示 Toast 通知
 * - 自动记录操作日志
 * - 支持包装异步函数
 *
 * @example
 * import { handleError } from '@/composables/useErrorHandler'
 *
 * try {
 *   await someApi()
 * } catch (error) {
 *   await handleError(error, {
 *     module: '用户管理',
 *     descPrefix: '删除用户失败'
 *   })
 * }
 *
 * @author Frontend Team
 * @since 1.0.0
 */
import { toast } from './useToast'
import { logOperation, LogModule, LogOperationType, LogStatus } from '../utils/logger'

export interface ErrorHandlerOptions {
  /** 是否显示 Toast 通知 */
  showToast?: boolean
  /** 是否记录操作日志 */
  logError?: boolean
  /** 日志模块名称 */
  module?: string
  /** 操作类型 */
  operationType?: string
  /** 操作描述前缀 */
  descPrefix?: string
  /** 自定义错误消息 */
  customMessage?: string
  /** 是否在控制台打印 */
  consoleLog?: boolean
}

const defaultOptions: ErrorHandlerOptions = {
  showToast: true,
  logError: true,
  consoleLog: true,
  module: LogModule.SYSTEM,
  operationType: LogOperationType.QUERY,
}

/**
 * 提取错误消息
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '未知错误'
}

/**
 * 处理错误
 */
export async function handleError(
  error: unknown,
  options: ErrorHandlerOptions = {},
): Promise<void> {
  const opts = { ...defaultOptions, ...options }
  const errorMessage = opts.customMessage || getErrorMessage(error)

  // 控制台打印
  if (opts.consoleLog) {
    console.error(opts.descPrefix ? `${opts.descPrefix}:` : '错误:', error)
  }

  // 显示 Toast
  if (opts.showToast) {
    toast.error(errorMessage)
  }

  // 记录日志
  if (opts.logError && opts.module && opts.operationType) {
    const desc = opts.descPrefix ? `${opts.descPrefix}：${errorMessage}` : errorMessage
    await logOperation(opts.module, opts.operationType, desc, LogStatus.FAILURE)
  }
}

/**
 * 包装异步函数，自动处理错误
 */
export function withErrorHandler<T extends unknown[], R>(
  fn: (...args: T) => Promise<R>,
  options: ErrorHandlerOptions = {},
): (...args: T) => Promise<R | undefined> {
  return async (...args: T): Promise<R | undefined> => {
    try {
      return await fn(...args)
    } catch (error) {
      await handleError(error, options)
      return undefined
    }
  }
}

/**
 * useErrorHandler composable
 */
export function useErrorHandler() {
  return {
    handleError,
    getErrorMessage,
    withErrorHandler,
  }
}
