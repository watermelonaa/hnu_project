/**
 * @file services/api/query.ts
 * @description 查询相关 API
 *
 * 接口：
 * - queryApi: 执行查询
 * - queryLogApi: 查询日志
 * - queryShareApi: 查询分享
 * - queryCollectionApi: 查询收藏夹
 * - collectionRecordApi: 收藏记录
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 查询接口 ====================

export interface QueryRequest {
  userPrompt: string
  model: string
  database: string
  dbConnectionId?: number // 数据库连接ID（用于实际连接）
  conversationId?: string
}

export interface QueryResponse {
  id: string
  userPrompt: string
  sqlQuery: string
  conversationId: string
  queryTime: string
  executionTime: string
  database: string
  dbConnectionId?: number  // 数据库连接ID（用于保存查询日志）
  model: string
  llmConfigId?: number  // 大模型配置ID（用于保存查询日志）
  tableData: {
    headers: string[]
    rows: string[][]
  }
  chartData?: {
    type: string
    labels: string[]
    datasets: Array<{
      label: string
      data: number[]
      backgroundColor?: string | string[]
    }>
  }
  // 【新增】后续探索问题 - 可选字段，向后兼容
  followupQuestions?: string[]
}

// 【可选】新增单独的后续问题生成接口
export interface FollowupQuestionRequest {
  question: string
  schema: string
}

export const queryApi = {
  execute: async (queryRequest: QueryRequest, signal?: AbortSignal): Promise<QueryResponse> => {
    return await request<QueryResponse>('/query/execute', {
      method: 'POST',
      body: JSON.stringify(queryRequest),
      signal,
    })
  },
  // 【可选】新增单独的后续问题生成接口
  generateFollowupQuestions: async (
    payload: FollowupQuestionRequest 
  ): Promise<string[]> => {
    return await request<string[]>('/query/followup-questions', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  }
}

// ==================== 推荐查询接口 ====================
export interface RecommendationRequest {
  conversationId?: string
  conversationHistory: Array<{
    role: 'user' | 'ai'
    content: string | {
      sqlQuery?: string
      tableData?: {
        headers: string[]
        rows: string[][]
      }
      chartData?: any
      [key: string]: any
    }
  }>
  dbConnectionId: number
  llmConfigId: string
  queryFailed?: boolean
}

export const recommendationApi = {
  getRecommendations: async (payload: RecommendationRequest): Promise<string[]> => {
    return await request<string[]>('/query/recommendations', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
}

// ==================== 查询日志接口 ====================
export interface QueryLog {
  id: number
  userId: number
  dialogId: string
  userPrompt: string
  sqlQuery: string
  queryResult: string
  queryTime: string
  executionTime: string
  llmConfigId: number
  dbConnectionId: number
}

export const queryLogApi = {
  getList: async (): Promise<QueryLog[]> => {
    return await request<QueryLog[]>('/query-log/list')
  },

  getByUser: async (userId: number): Promise<QueryLog[]> => {
    return await request<QueryLog[]>(`/query-log/list/user/${userId}`)
  },

  getByDialog: async (dialogId: string): Promise<QueryLog[]> => {
    return await request<QueryLog[]>(`/query-log/list/dialog/${dialogId}`)
  },

  getById: async (id: number): Promise<QueryLog> => {
    return await request<QueryLog>(`/query-log/${id}`)
  },

  create: async (queryLog: Partial<QueryLog>): Promise<QueryLog> => {
    return await request<QueryLog>('/query-log', {
      method: 'POST',
      body: JSON.stringify(queryLog),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/query-log/${id}`, {
      method: 'DELETE',
    })
  },
}

// ==================== 查询分享接口 ====================
/**
 * ⚠️ 重要说明：此接口定义与后端实体类不匹配
 *
 * 前端定义包含 queryLogId 字段，但后端 QueryShare 实体类没有此字段
 * 后端实体类字段：
 * - id, shareUserId, receiveUserId, dialogId, targetRounds, queryTitle, shareTime, receiveStatus
 *
 * 问题：
 * - 前端传递 queryLogId，但后端无法接收此字段
 * - 后端需要 dialogId 和 targetRounds，但前端未传递
 *
 * 建议后端修复：
 * 1. 在 QueryShare 实体类中添加 queryLogId 字段（推荐）
 * 2. 或在 Controller 中根据 queryLogId 查询 QueryLog 获取 dialogId
 */
export interface QueryShare {
  id: number
  shareUserId: number
  receiveUserId: number
  queryLogId: number  // ⚠️ 后端实体类缺少此字段
  shareTime: string
  receiveStatus: number
}

export const queryShareApi = {
  getByReceiveUser: async (receiveUserId: number): Promise<QueryShare[]> => {
    return await request<QueryShare[]>(`/query-share/list/receive/${receiveUserId}`)
  },

  getByReceiveUserAndStatus: async (
    receiveUserId: number,
    receiveStatus: number,
  ): Promise<QueryShare[]> => {
    return await request<QueryShare[]>(
      `/query-share/list/receive/${receiveUserId}/${receiveStatus}`,
    )
  },

  getByShareUser: async (shareUserId: number): Promise<QueryShare[]> => {
    return await request<QueryShare[]>(`/query-share/list/share/${shareUserId}`)
  },

  getById: async (id: number): Promise<QueryShare> => {
    return await request<QueryShare>(`/query-share/${id}`)
  },

  create: async (queryShare: Partial<QueryShare>): Promise<QueryShare> => {
    return await request<QueryShare>('/query-share', {
      method: 'POST',
      body: JSON.stringify(queryShare),
    })
  },

  update: async (queryShare: Partial<QueryShare>): Promise<QueryShare> => {
    return await request<QueryShare>('/query-share', {
      method: 'PUT',
      body: JSON.stringify(queryShare),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/query-share/${id}`, {
      method: 'DELETE',
    })
  },
}

// ==================== 查询收藏接口 ====================
export interface QueryCollection {
  id: string | number  // MongoDB 返回的是 String，但前端可能使用 number
  userId: number
  collectionName: string
  description?: string
  createTime: string
}

export interface CollectionRecord {
  id: string
  collectionId: number
  queryLogId: number
  userId?: number
  sqlContent?: string
  userPrompt?: string  // 用户提问，作为标题
  queryResult?: {
    tableData?: any
    chartData?: any
    database?: string
    model?: string
    executionTime?: string
  }
  dbConnectionId?: number
  llmConfigId?: number
  addTime: string
  createTime?: string  // 执行时间
}

export const queryCollectionApi = {
  getByUser: async (userId: number): Promise<QueryCollection[]> => {
    return await request<QueryCollection[]>(`/query-collection/list/${userId}`)
  },

  create: async (collection: Partial<QueryCollection>): Promise<QueryCollection> => {
    return await request<QueryCollection>('/query-collection', {
      method: 'POST',
      body: JSON.stringify(collection),
    })
  },

  update: async (collection: Partial<QueryCollection>): Promise<QueryCollection> => {
    return await request<QueryCollection>('/query-collection', {
      method: 'PUT',
      body: JSON.stringify(collection),
    })
  },

  delete: async (id: number | string): Promise<void> => {
    const idStr = typeof id === 'string' ? id : String(id)
    return await request<void>(`/query-collection/${idStr}`, {
      method: 'DELETE',
    })
  },

  /**
   * 清理无效的收藏夹（名称为 undefined、null 或空字符串）
   */
  cleanupInvalid: async (userId: number): Promise<{ message: string }> => {
    return await request<{ message: string }>(`/query-collection/cleanup-invalid/${userId}`, {
      method: 'POST',
    })
  },
}

export const collectionRecordApi = {
  getByCollection: async (collectionId: string): Promise<CollectionRecord[]> => {
    return await request<CollectionRecord[]>(`/collection-record/list/query/${collectionId}`)
  },

  getByUser: async (userId: number): Promise<CollectionRecord[]> => {
    return await request<CollectionRecord[]>(`/collection-record/list/user/${userId}`)
  },

  create: async (record: Partial<CollectionRecord>): Promise<CollectionRecord> => {
    return await request<CollectionRecord>('/collection-record', {
      method: 'POST',
      body: JSON.stringify(record),
    })
  },

  delete: async (id: string): Promise<void> => {
    return await request<void>(`/collection-record/${id}`, {
      method: 'DELETE',
    })
  },
}
