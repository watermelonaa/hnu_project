/**
 * @file services/api/permission.ts
 * @description 用户权限相关 API
 *
 * 接口：
 * - userDbPermissionApi: 用户数据库权限管理
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 用户数据权限接口 ====================

export interface UserDbPermission {
  id: number
  userId: number
  permissionDetails: string // JSON字符串
  isAssigned: number
  lastGrantUserId: number
  lastGrantTime?: string
}

export const userDbPermissionApi = {
  getList: async (): Promise<UserDbPermission[]> => {
    return await request<UserDbPermission[]>('/user-db-permission/list')
  },

  getAssigned: async (): Promise<UserDbPermission[]> => {
    return await request<UserDbPermission[]>('/user-db-permission/list/assigned')
  },

  getUnassigned: async (): Promise<UserDbPermission[]> => {
    return await request<UserDbPermission[]>('/user-db-permission/list/unassigned')
  },

  getByUserId: async (userId: number): Promise<UserDbPermission> => {
    return await request<UserDbPermission>(`/user-db-permission/user/${userId}`)
  },

  create: async (permission: Partial<UserDbPermission>): Promise<UserDbPermission> => {
    return await request<UserDbPermission>('/user-db-permission', {
      method: 'POST',
      body: JSON.stringify(permission),
    })
  },

  update: async (permission: Partial<UserDbPermission>): Promise<UserDbPermission> => {
    return await request<UserDbPermission>('/user-db-permission', {
      method: 'PUT',
      body: JSON.stringify(permission),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/user-db-permission/${id}`, {
      method: 'DELETE',
    })
  },
}
