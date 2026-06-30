/**
 * @file services/api/auth.ts
 * @description 认证相关 API
 *
 * 接口：
 * - login: 用户登录
 * - logout: 用户登出
 * - isAuthenticated: 检查登录状态
 *
 * @author Frontend Team
 */
import { request, getToken } from './request'

// ==================== 认证接口 ====================

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  email: string
  roleId: number
  roleName: string
  avatarUrl: string
}

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    })

    // 保存token和用户信息到sessionStorage（每个标签页独立）
    if (response.token) {
      sessionStorage.setItem('token', response.token)
      sessionStorage.setItem('userId', String(response.userId))
      sessionStorage.setItem('username', response.username)
      sessionStorage.setItem('roleId', String(response.roleId))
      sessionStorage.setItem('roleName', response.roleName || '')

      // 根据 roleId 映射前端角色类型并保存
      let role: string = 'normal-user'
      if (response.roleId === 1) {
        role = 'sys-admin'
      } else if (response.roleId === 2) {
        role = 'data-admin'
      } else if (response.roleId === 3) {
        role = 'normal-user'
      }
      sessionStorage.setItem('userRole', role)
    }

    return response
  },

  logout: async (): Promise<void> => {
    try {
      // 调用后端登出接口，将token加入黑名单
      await request('/auth/logout', {
        method: 'POST',
      })
    } catch (error) {
      // 即使后端登出失败，也清除本地存储
      console.error('登出失败:', error)
    } finally {
      // 清除本地存储的用户信息
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('userId')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('roleId')
      sessionStorage.removeItem('roleName')
      sessionStorage.removeItem('userRole')
    }
  },

  isAuthenticated: (): boolean => {
    return !!getToken()
  },

  /**
   * 忘记密码 - 发送重置密码邮件
   * @param email 用户邮箱地址
   */
  forgotPassword: async (email: string): Promise<void> => {
    await request<void>('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    })
  },
}
