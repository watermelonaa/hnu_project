/**
 * @file services/api/db.ts
 * @description 数据库连接相关 API
 *
 * 接口：
 * - dbConnectionApi: 数据库连接 CRUD、测试连接
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 数据库连接接口 ====================

export interface DbConnection {
  id: number
  name: string
  dbTypeId: number
  url: string
  username: string
  password?: string
  status: string
  createUserId: number
}

export const dbConnectionApi = {
  getList: async (): Promise<DbConnection[]> => {
    return await request<DbConnection[]>('/db-connection/list')
  },

  getById: async (id: number): Promise<DbConnection> => {
    return await request<DbConnection>(`/db-connection/${id}`)
  },

  getListByUser: async (createUserId: number): Promise<DbConnection[]> => {
    return await request<DbConnection[]>(`/db-connection/list/${createUserId}`)
  },

  create: async (dbConnection: Partial<DbConnection>): Promise<DbConnection> => {
    return await request<DbConnection>('/db-connection', {
      method: 'POST',
      body: JSON.stringify(dbConnection),
    })
  },

  update: async (dbConnection: Partial<DbConnection>): Promise<DbConnection> => {
    return await request<DbConnection>('/db-connection', {
      method: 'PUT',
      body: JSON.stringify(dbConnection),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/db-connection/${id}`, {
      method: 'DELETE',
    })
  },

  test: async (id: number): Promise<boolean> => {
    return await request<boolean>(`/db-connection/test/${id}`)
  },

  getTables: async (id: number): Promise<string[]> => {
    return await request<string[]>(`/db-connection/${id}/tables`)
  },
}
