/**
 * @file services/api/friend.ts
 * @description 好友相关 API
 *
 * 接口：
 * - friendRelationApi: 好友关系管理
 * - friendRequestApi: 好友请求处理
 * - friendChatApi: 好友聊天消息
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 好友关系接口 ====================
import type { User } from './user'

export interface FriendRelation {
  id: number
  userId: number
  friendId: number
  remark?: string
  remarkName?: string  // 后端实际字段名
  onlineStatus: number
  createTime: string
}

export interface FriendRelationWithUser extends FriendRelation {
  friendUser?: User
}

export const friendRelationApi = {
  getByUser: async (userId: number): Promise<FriendRelation[]> => {
    return await request<FriendRelation[]>(`/friend-relation/list/${userId}`)
  },

  getByUserAndFriend: async (userId: number, friendId: number): Promise<FriendRelation> => {
    return await request<FriendRelation>(`/friend-relation/${userId}/${friendId}`)
  },

  create: async (friendRelation: Partial<FriendRelation>): Promise<FriendRelation> => {
    return await request<FriendRelation>('/friend-relation', {
      method: 'POST',
      body: JSON.stringify(friendRelation),
    })
  },

  update: async (friendRelation: Partial<FriendRelation>): Promise<FriendRelation> => {
    return await request<FriendRelation>('/friend-relation', {
      method: 'PUT',
      body: JSON.stringify(friendRelation),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/friend-relation/${id}`, {
      method: 'DELETE',
    })
  },

  deleteByUserAndFriend: async (userId: number, friendId: number): Promise<void> => {
    return await request<void>(`/friend-relation/${userId}/${friendId}`, {
      method: 'DELETE',
    })
  },
}

// ==================== 好友请求接口 ====================
export interface FriendRequest {
  id: number
  applicantId: number
  recipientId: number
  applyMsg?: string
  status: number
  createTime: string
  handleTime?: string
}

export interface FriendRequestWithUser extends FriendRequest {
  applicantUser?: User
}

export const friendRequestApi = {
  getByRecipient: async (recipientId: number): Promise<FriendRequest[]> => {
    return await request<FriendRequest[]>(`/friend-request/list/${recipientId}`)
  },

  getByRecipientAndStatus: async (
    recipientId: number,
    status: number,
  ): Promise<FriendRequest[]> => {
    return await request<FriendRequest[]>(`/friend-request/list/${recipientId}/${status}`)
  },

  getById: async (id: number): Promise<FriendRequest> => {
    return await request<FriendRequest>(`/friend-request/${id}`)
  },

  create: async (friendRequest: Partial<FriendRequest>): Promise<FriendRequest> => {
    return await request<FriendRequest>('/friend-request', {
      method: 'POST',
      body: JSON.stringify(friendRequest),
    })
  },

  update: async (friendRequest: Partial<FriendRequest>): Promise<FriendRequest> => {
    return await request<FriendRequest>('/friend-request', {
      method: 'PUT',
      body: JSON.stringify(friendRequest),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/friend-request/${id}`, {
      method: 'DELETE',
    })
  },

  accept: async (id: number): Promise<string> => {
    return await request<string>(`/friend-request/${id}/accept`, {
      method: 'POST',
    })
  },

  reject: async (id: number): Promise<string> => {
    return await request<string>(`/friend-request/${id}/reject`, {
      method: 'POST',
    })
  },
}

// ==================== 好友聊天接口 ====================
export interface FriendChat {
  id: string
  userId: number  // 发送人ID（后端实际字段）
  friendId: number  // 接收人ID（后端实际字段）
  sendUserId?: number  // 兼容字段
  receiveUserId?: number  // 兼容字段
  contentType?: string  // 消息类型：text, query_share, image
  content: string | Record<string, any>  // 消息内容（可能是字符串或对象）
  sendTime: string
  isRead: boolean
}

// 聊天消息VO（后端返回的完整消息对象）
export interface ChatMessageVO {
  id: string
  senderId: number
  senderName: string
  senderAvatar?: string
  receiverId: number
  receiverName: string
  receiverAvatar?: string
  contentType: string
  content: any
  sendTime: string
  isRead: boolean
  extra?: any
}

// 分页查询参数
export interface ChatMessagePageDTO {
  userId: number
  friendId: number
  page: number
  pageSize: number
  lastMessageTime?: string
}

// 分页响应
export interface ChatMessagePage {
  content: ChatMessageVO[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// 发送消息DTO
export interface SendChatMessageDTO {
  senderId: number
  receiverId: number
  contentType: string
  content: Record<string, any>
  extra?: Record<string, any>
}

// 未读消息统计VO
export interface UnreadCountVO {
  friendId: number
  friendName: string
  friendAvatar: string
  unreadCount: number
  lastMessagePreview: string
  lastMessageTime: string
}

// 最近聊天列表项
export interface RecentChat {
  friendId: number
  friendName: string
  friendAvatar: string
  onlineStatus: number
  lastMessagePreview: string
  lastMessageTime: string
  unreadCount: number
}

export const friendChatApi = {
  getByUserAndFriend: async (userId: number, friendId: number): Promise<FriendChat[]> => {
    return await request<FriendChat[]>(`/friend-chat/list/${userId}/${friendId}`)
  },

  getUnreadByFriend: async (friendId: number): Promise<FriendChat[]> => {
    return await request<FriendChat[]>(`/friend-chat/unread/${friendId}`)
  },

  create: async (friendChat: Partial<FriendChat>): Promise<FriendChat> => {
    return await request<FriendChat>('/friend-chat', {
      method: 'POST',
      body: JSON.stringify(friendChat),
    })
  },

  update: async (friendChat: Partial<FriendChat>): Promise<FriendChat> => {
    return await request<FriendChat>('/friend-chat', {
      method: 'PUT',
      body: JSON.stringify(friendChat),
    })
  },

  delete: async (id: string): Promise<void> => {
    return await request<void>(`/friend-chat/${id}`, {
      method: 'DELETE',
    })
  },

  // 新版接口
  /** 发送聊天消息 */
  sendMessage: async (dto: SendChatMessageDTO): Promise<ChatMessageVO> => {
    return await request<ChatMessageVO>('/friend-chat/send', {
      method: 'POST',
      body: JSON.stringify(dto),
    })
  },

  /** 分页查询聊天记录 */
  getChatHistoryPage: async (dto: ChatMessagePageDTO): Promise<ChatMessagePage> => {
    return await request<ChatMessagePage>('/friend-chat/history/page', {
      method: 'POST',
      body: JSON.stringify(dto),
    })
  },

  /** 获取未读消息统计（按好友分组） */
  getUnreadCountByFriends: async (userId: number): Promise<UnreadCountVO[]> => {
    return await request<UnreadCountVO[]>(`/friend-chat/unread/count/${userId}`)
  },

  /** 获取来自指定好友的未读消息数量 */
  getUnreadCountFromFriend: async (userId: number, friendId: number): Promise<{ userId: number; friendId: number; unreadCount: number }> => {
    return await request<{ userId: number; friendId: number; unreadCount: number }>(`/friend-chat/unread/count/${userId}/${friendId}`)
  },

  /** 获取最近聊天列表 */
  getRecentChats: async (userId: number): Promise<RecentChat[]> => {
    return await request<RecentChat[]>(`/friend-chat/recent/${userId}`)
  },

  /** 批量标记消息为已读 */
  markMessagesAsRead: async (userId: number, friendId: number): Promise<{ markedCount: number; message: string }> => {
    return await request<{ markedCount: number; message: string }>(`/friend-chat/mark-read?userId=${userId}&friendId=${friendId}`, {
      method: 'PUT',
    })
  },

  /** 标记单条消息为已读 */
  markMessageAsRead: async (messageId: string): Promise<{ success: boolean; message: string }> => {
    return await request<{ success: boolean; message: string }>(`/friend-chat/mark-read/${messageId}`, {
      method: 'PUT',
    })
  },
}

// ==================== 查询分享聊天接口 ====================
export interface ShareQueryRequest {
  shareUserId: number
  receiveUserId: number
  queryTitle: string
  sqlQuery: string
  databaseName: string
  dbConnectionId?: number | null
  llmName?: string
  llmConfigId?: number | null
  executionTime: number
  queryTime: string
  dialogId?: string
  shareMessage?: string
  tableData?: {
    headers: string[]
    rows: string[][]
  }
  chartData?: {
    type: string
    labels?: string[]
    xAxis?: string[]
    yAxis?: any[]
    datasets?: Array<{
      label: string
      data: number[]
      backgroundColor?: string | string[]
    }>
  }
}

export interface QueryShareVO {
  shareId: string
  shareUserId: number
  shareUserName?: string
  shareUserAvatar?: string
  receiveUserId: number
  receiveUserName?: string
  queryTitle: string
  sqlQuery: string
  databaseName: string
  dbConnectionId?: number
  llmName?: string
  llmConfigId?: number
  executionTime?: number
  executionTimeText?: string
  queryTime: string
  dialogId?: string
  shareMessage?: string
  shareTime: string
  receiveStatus: number  // 0-未处理, 1-已保存, 2-已删除
  tableData?: {
    headers: string[]
    rows: any[][]
    totalRows?: number
  }
  chartData?: {
    type: string
    xAxis: string[]
    yAxis: any[]
    datasets: any[]
  }
}

export const queryShareChatApi = {
  /**
   * 分享查询记录给好友
   */
  share: async (shareData: ShareQueryRequest): Promise<ChatMessageVO> => {
    return await request<ChatMessageVO>('/query-share-chat/share', {
      method: 'POST',
      body: JSON.stringify(shareData),
    })
  },

  /**
   * 获取收到的查询分享列表
   */
  getReceived: async (userId: number): Promise<QueryShareVO[]> => {
    return await request<QueryShareVO[]>(`/query-share-chat/received/${userId}`)
  },

  /**
   * 获取分享出去的查询列表
   */
  getShared: async (userId: number): Promise<QueryShareVO[]> => {
    return await request<QueryShareVO[]>(`/query-share-chat/shared/${userId}`)
  },

  /**
   * 获取查询分享详情
   */
  getDetail: async (shareId: string): Promise<QueryShareVO> => {
    return await request<QueryShareVO>(`/query-share-chat/detail/${shareId}`)
  },

  /**
   * 保存收到的查询分享
   */
  save: async (shareId: string, userId: number): Promise<{ success: boolean; message: string }> => {
    return await request<{ success: boolean; message: string }>(
      `/query-share-chat/save/${shareId}/${userId}`,
      {
        method: 'PUT',
      }
    )
  },

  /**
   * 删除收到的查询分享
   */
  delete: async (shareId: string, userId: number): Promise<{ success: boolean; message: string }> => {
    return await request<{ success: boolean; message: string }>(
      `/query-share-chat/delete/${shareId}/${userId}`,
      {
        method: 'DELETE',
      }
    )
  },
}
