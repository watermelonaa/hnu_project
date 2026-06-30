/**
 * @file services/api/user.ts
 * @description 用户相关 API
 *
 * 接口：
 * - userApi: 用户 CRUD 操作
 * - userSearchApi: 用户搜索
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 用户接口 ====================

export interface User {
  id: number
  username: string
  email: string
  phonenumber: string
  roleId: number
  avatarUrl: string
  status: number
}

export interface ChangePasswordRequest {
  userId: number
  oldPassword: string
  newPassword: string
}

export const userApi = {
  getById: async (id: number): Promise<User> => {
    return await request<User>(`/user/${id}`)
  },

  getList: async (): Promise<User[]> => {
    return await request<User[]>('/user/list')
  },

  getByUsername: async (username: string): Promise<User> => {
    return await request<User>(`/user/username/${username}`)
  },

  create: async (user: Partial<User>): Promise<string> => {
    return await request<string>('/user', {
      method: 'POST',
      body: JSON.stringify(user),
    })
  },

  update: async (user: Partial<User>): Promise<string> => {
    return await request<string>('/user', {
      method: 'PUT',
      body: JSON.stringify(user),
    })
  },

  delete: async (id: number): Promise<string> => {
    return await request<string>(`/user/${id}`, {
      method: 'DELETE',
    })
  },

  changePassword: async (changePasswordRequest: ChangePasswordRequest): Promise<string> => {
    return await request<string>('/user/change-password', {
      method: 'POST',
      body: JSON.stringify(changePasswordRequest),
    })
  },

  resetPassword: async (id: number): Promise<string> => {
    return await request<string>(`/user/reset-password/${id}`, {
      method: 'POST',
    })
  },
}

// ==================== 用户搜索接口 ====================
export interface UserSearchRecord {
  id: number
  userId: number
  searchKeyword: string
  searchTime: string
}

export const userSearchApi = {
  searchByEmail: async (email: string): Promise<User[]> => {
    return await request<User[]>(`/user/search/email?email=${encodeURIComponent(email)}`)
  },

  searchByPhoneNumber: async (phoneNumber: string): Promise<User> => {
    return await request<User>(`/user/search/phone?phoneNumber=${encodeURIComponent(phoneNumber)}`)
  },

  getSearchHistory: async (userId: number): Promise<UserSearchRecord[]> => {
    return await request<UserSearchRecord[]>(`/user-search/list/${userId}`)
  },

  getTopSearches: async (userId: number, limit: number): Promise<UserSearchRecord[]> => {
    return await request<UserSearchRecord[]>(`/user-search/list/top/${userId}/${limit}`)
  },

  saveSearch: async (userSearch: Partial<UserSearchRecord>): Promise<UserSearchRecord> => {
    return await request<UserSearchRecord>('/user-search', {
      method: 'POST',
      body: JSON.stringify(userSearch),
    })
  },
}
