/**
 * @file utils/logger.ts
 * @description 操作日志记录工具
 *
 * 功能：
 * - 记录用户操作日志到后端
 * - 提供预定义的模块名称和操作类型常量
 * - 静默失败，不影响业务逻辑
 *
 * @example
 * import { logOperation, LogModule, LogOperationType, LogStatus } from '@/utils/logger'
 *
 * await logOperation(
 *   LogModule.USER_MANAGEMENT,
 *   LogOperationType.CREATE,
 *   '创建用户: admin',
 *   LogStatus.SUCCESS
 * )
 *
 * @author Frontend Team
 * @since 1.0.0
 */

import { operationLogApi } from '../services/api.real'

/**
 * 记录操作日志
 * @param module 模块名称（如：用户管理、数据源管理等）
 * @param operateType 操作类型（如：创建、更新、删除等）
 * @param operateDesc 操作描述
 * @param status 操作状态（1: 成功, 0: 失败）
 */
export const logOperation = async (
  module: string,
  operateType: string,
  operateDesc: string,
  status: number = 1,
): Promise<void> => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '0')
    if (!userId) {
      console.warn('未找到用户ID，无法记录日志')
      return
    }

    await operationLogApi.create({
      userId,
      module,
      // 后端字段为 operation/result，这里同时写入兼容字段避免错位
      operation: operateType,
      operateType,
      operateDesc,
      result: status,
      status,
      ip: 'unknown', // 前端无法直接获取IP，由后端补充
    })
  } catch (error) {
    console.error('记录操作日志失败:', error)
    // 日志记录失败不应该影响业务操作，所以只打印错误
  }
}

/**
 * 常用模块名称
 */
export const LogModule = {
  USER_MANAGEMENT: '用户管理',
  DATA_SOURCE: '数据源管理',
  LLM_CONFIG: '大模型配置',
  PERMISSION: '权限管理',
  NOTIFICATION: '通知管理',
  QUERY: '查询操作',
  SYSTEM: '系统操作',
} as const

/**
 * 常用操作类型
 */
export const LogOperationType = {
  CREATE: '创建',
  UPDATE: '更新',
  DELETE: '删除',
  ENABLE: '启用',
  DISABLE: '禁用',
  TEST: '测试',
  ASSIGN: '分配',
  PUBLISH: '发布',
  LOGIN: '登录',
  LOGOUT: '登出',
  QUERY: '查询',
} as const

/**
 * 操作状态
 */
export const LogStatus = {
  SUCCESS: 1,
  FAILURE: 0,
} as const
