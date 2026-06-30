/**
 * @file services/api/log.ts
 * @description 日志相关 API
 *
 * 接口：
 * - operationLogApi: 操作日志
 * - errorLogApi: 错误日志
 * - dbConnectionLogApi: 数据库连接日志
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 操作日志接口 ====================

export interface OperationLog {
  id: number
  userId: number
  username?: string
  module: string
  relatedLlm?: string // 涉及的大模型名称
  // 后端字段名为 operation，兼容旧的 operateType/operateDesc
  operation?: string
  operateType?: string
  operateDesc?: string
  operateTime: string
  ip?: string
  ipAddress?: string
  // 后端字段名为 result，兼容旧的 status
  result?: number
  status?: number
  errorMsg?: string
}

export const operationLogApi = {
  getList: async (): Promise<OperationLog[]> => {
    return await request<OperationLog[]>('/operation-log/list')
  },

  getByTimeRange: async (startTime?: string, endTime?: string): Promise<OperationLog[]> => {
    const params = new URLSearchParams()
    if (startTime) params.append('startTime', startTime)
    if (endTime) params.append('endTime', endTime)
    const queryString = params.toString()
    return await request<OperationLog[]>(`/operation-log/list/time-range${queryString ? '?' + queryString : ''}`)
  },

  getByUser: async (userId: number): Promise<OperationLog[]> => {
    return await request<OperationLog[]>(`/operation-log/list/user/${userId}`)
  },

  getByModule: async (module: string): Promise<OperationLog[]> => {
    return await request<OperationLog[]>(`/operation-log/list/module/${encodeURIComponent(module)}`)
  },

  getFailed: async (): Promise<OperationLog[]> => {
    return await request<OperationLog[]>('/operation-log/list/failed')
  },

  getById: async (id: number): Promise<OperationLog> => {
    return await request<OperationLog>(`/operation-log/${id}`)
  },

  create: async (log: Partial<OperationLog>): Promise<OperationLog> => {
    return await request<OperationLog>('/operation-log', {
      method: 'POST',
      body: JSON.stringify(log),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/operation-log/${id}`, {
      method: 'DELETE',
    })
  },
}

// ==================== 错误日志接口 ====================
export interface ErrorLog {
  id: number
  errorTypeId: number
  errorCount: number
  statPeriod: string
  statTime: string
}

export const errorLogApi = {
  getList: async (): Promise<ErrorLog[]> => {
    return await request<ErrorLog[]>('/error-log/list')
  },

  getByErrorType: async (errorTypeId: number): Promise<ErrorLog[]> => {
    return await request<ErrorLog[]>(`/error-log/list/type/${errorTypeId}`)
  },

  getByPeriod: async (period: string): Promise<ErrorLog[]> => {
    return await request<ErrorLog[]>(`/error-log/list/period/${encodeURIComponent(period)}`)
  },

  getById: async (id: number): Promise<ErrorLog> => {
    return await request<ErrorLog>(`/error-log/${id}`)
  },

  create: async (log: Partial<ErrorLog>): Promise<ErrorLog> => {
    return await request<ErrorLog>('/error-log', {
      method: 'POST',
      body: JSON.stringify(log),
    })
  },

  update: async (log: Partial<ErrorLog>): Promise<ErrorLog> => {
    return await request<ErrorLog>('/error-log', {
      method: 'PUT',
      body: JSON.stringify(log),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/error-log/${id}`, {
      method: 'DELETE',
    })
  },
}

// ==================== 数据库连接日志接口 ====================
export interface DbConnectionLog {
  id: number
  dbConnectionId: number
  dbName?: string
  connectTime: string
  disconnectTime?: string
  status: string // 后端为字符串，如 connected / error / disconnected
  remark?: string
  errorMessage?: string // 兼容旧字段
}

export const dbConnectionLogApi = {
  getList: async (): Promise<DbConnectionLog[]> => {
    return await request<DbConnectionLog[]>('/db-connection-log/list')
  },

  getByConnection: async (dbConnectionId: number): Promise<DbConnectionLog[]> => {
    return await request<DbConnectionLog[]>(`/db-connection-log/list/connection/${dbConnectionId}`)
  },

  getById: async (id: number): Promise<DbConnectionLog> => {
    return await request<DbConnectionLog>(`/db-connection-log/${id}`)
  },

  create: async (log: Partial<DbConnectionLog>): Promise<DbConnectionLog> => {
    return await request<DbConnectionLog>('/db-connection-log', {
      method: 'POST',
      body: JSON.stringify(log),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/db-connection-log/${id}`, {
      method: 'DELETE',
    })
  },
}
