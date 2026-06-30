/**
 * @file services/api/notification.ts
 * @description 通知相关 API
 *
 * 接口：
 * - notificationApi: 通知 CRUD、发布、置顶
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 通知接口 ====================

export interface Notification {
  id: number
  title: string
  content: string
  targetId: number
  priorityId: number
  publishTime?: string
  isTop: number
  publisherId: number
  createTime: string
  latestUpdateTime: string
}

export interface NotificationWithReadStatus extends Notification {
  isRead: number
}

export const notificationApi = {
  getList: async (): Promise<Notification[]> => {
    return await request<Notification[]>('/notification/list')
  },

  getPublished: async (): Promise<Notification[]> => {
    return await request<Notification[]>('/notification/list/published')
  },

  getDrafts: async (): Promise<Notification[]> => {
    return await request<Notification[]>('/notification/list/drafts')
  },

  getByTarget: async (targetId: number): Promise<Notification[]> => {
    return await request<Notification[]>(`/notification/list/target/${targetId}`)
  },

  getById: async (id: number): Promise<Notification> => {
    return await request<Notification>(`/notification/${id}`)
  },

  create: async (notification: Partial<Notification>): Promise<Notification> => {
    return await request<Notification>('/notification', {
      method: 'POST',
      body: JSON.stringify(notification),
    })
  },

  update: async (notification: Partial<Notification>): Promise<Notification> => {
    return await request<Notification>('/notification', {
      method: 'PUT',
      body: JSON.stringify(notification),
    })
  },

  publish: async (id: number): Promise<void> => {
    return await request<void>(`/notification/${id}/publish`, {
      method: 'PUT',
    })
  },

  toggleTop: async (id: number): Promise<void> => {
    return await request<void>(`/notification/${id}/toggle-top`, {
      method: 'PUT',
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/notification/${id}`, {
      method: 'DELETE',
    })
  },

  // ==================== 用户相关API ====================
  /** 获取用户可见的通知列表（带已读状态） */
  getUserNotifications: async (): Promise<NotificationWithReadStatus[]> => {
    return await request<NotificationWithReadStatus[]>('/notification/user/list')
  },

  /** 获取用户未读通知数量 */
  getUnreadCount: async (): Promise<number> => {
    return await request<number>('/notification/user/unread-count')
  },

  /** 标记通知为已读 */
  markAsRead: async (notificationId: number): Promise<void> => {
    return await request<void>(`/notification/user/${notificationId}/read`, {
      method: 'PUT',
    })
  },

  /** 标记通知为未读 */
  markAsUnread: async (notificationId: number): Promise<void> => {
    return await request<void>(`/notification/user/${notificationId}/unread`, {
      method: 'PUT',
    })
  },

  /** 用户删除通知（只能删除非置顶通知） */
  deleteByUser: async (id: number): Promise<void> => {
    return await request<void>(`/notification/user/${id}`, {
      method: 'DELETE',
    })
  },
}
