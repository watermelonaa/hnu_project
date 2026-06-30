/**
 * @file types/index.ts
 * @description 全局 TypeScript 类型定义
 *
 * 类型分类：
 * - 基础类型：UserRole, MessageRole
 * - 页面导航：Page, SysAdminPageType, DataAdminPageType
 * - 数据结构：QueryResultData, Conversation, Notification
 * - 用户相关：UserProfile, Friend, FriendRequest
 * - 管理员类型：AdminUser, SystemLog, LLMConfig
 * - 数据管理员：DataSource, DataSourcePermission
 *
 * 使用方式：
 * ```typescript
 * import type { UserRole, QueryResultData } from '@/types'
 * ```
 *
 * @author Frontend Team
 * @since 1.0.0
 */

// ==================== 基础类型 ====================
export type UserRole = 'sys-admin' | 'data-admin' | 'normal-user'
export type MessageRole = 'user' | 'ai'

export interface DashboardStats {
  datasourceCount: number
  connectedCount: number
  errorCount: number
  pendingPermissions: number
  healthStatusData?: Array<{ label: string; value: number }>
  queryLoadData?: Array<{ label: string; value: number }>
  recentFailures?: ConnectionLog[]
  recentPermissionLogs?: PermissionLog[]
}

// ==================== 页面导航类型 ====================
export type Page = 'query' | 'history' | 'notifications' | 'account' | 'friends' | 'comparison' | 'settings'

export type SysAdminPageType =
  | 'dashboard'
  | 'user-management'
  | 'notification-management'
  | 'system-log'
  | 'llm-config'
  | 'account'
  | 'settings'

export type DataAdminPageType =
  | 'dashboard'
  | 'query'
  | 'history'
  | 'datasource'
  | 'user-permission'
  | 'notification-management'
  | 'connection-log'
  | 'notifications'
  | 'account'
  | 'friends'
  | 'comparison'
  | 'settings'

// ==================== 数据结构类型 ====================
export interface ModelOption {
  name: string
  disabled: boolean
  description: string
}

export interface ChartData {
  type: 'bar' | 'line' | 'pie'
  labels: string[]
  datasets: {
    label: string
    data: number[]
    backgroundColor: string | string[]
  }[]
}

export interface TableData {
  headers: string[]
  rows: string[][]
}

export interface QueryResultData {
  id: string
  userPrompt: string
  sqlQuery: string
  conversationId: string
  queryTime: string
  executionTime: string
  tableData: TableData
  chartData?: ChartData
  database: string
  model: string
  dbConnectionId?: number  // 数据库连接ID（用于保存查询日志）
  llmConfigId?: number  // 大模型配置ID（用于保存查询日志）
}

export interface Message {
  role: MessageRole
  content: string | QueryResultData
}

export interface Conversation {
  id: string
  title: string
  messages: Message[]
  createTime: string
  totalRounds?: number // 后端保存的轮次数，用于区分是否为后端加载的对话
}

// ==================== 通知类型 ====================
export interface Notification {
  id: string
  type: 'system' | 'share'
  title: string
  content: string
  timestamp: string
  isRead: boolean
  isPinned: boolean
  fromUser?: { id?: string; name: string; avatarUrl: string }
  relatedShareId?: string
}

// ==================== 用户相关类型 ====================
export interface UserProfile {
  id: string
  userId: string
  name: string
  email: string
  phoneNumber: string
  avatarUrl: string
  registrationDate: string
  accountStatus: 'normal' | 'disabled'
  preferences: {
    defaultModel: string
    defaultDatabase: string
  }
}

export interface Friend {
  id: string
  name: string
  avatarUrl: string
  isOnline: boolean
  email: string
  remark?: string
}

export interface FriendRequest {
  id: string
  fromUser: { id?: string; name: string; avatarUrl: string }
  timestamp: string
}

// ==================== 查询分享类型 ====================
export interface QueryShare {
  id: string
  sender: Friend
  recipientId: string
  querySnapshot: QueryResultData
  timestamp: string
  status: 'unread' | 'read'
}

// ==================== 管理员类型 ====================
export interface AdminNotification {
  id: number
  title: string
  content: string
  role: 'all' | UserRole
  priority: 'urgent' | 'normal' | 'low'
  pinned: boolean
  publisher: string
  publishTime: string
  status: 'published' | 'draft'
  dataSourceTopic?: string
}

export interface AdminUser {
  id: number
  username: string
  role: UserRole
  email: string
  regTime: string
  status: 'active' | 'disabled'
  phonenumber: string
}

export interface SystemLog {
  id: string
  time: string
  user: string
  action: string
  model: string
  llm?: string
  ip: string
  status: 'success' | 'failure'
  details?: string
}

export interface LLMConfig {
  id: string
  name: string
  version: string
  apiKey: string
  endpoint: string
  status: 'available' | 'unstable' | 'unavailable' | 'testing' | 'disabled'
}

// ==================== 数据管理员类型 ====================
export interface DataSource {
  id: string
  name: string
  type: 'MySQL' | 'PostgreSQL' | 'Oracle' | 'SQL Server'
  address: string
  status: 'connected' | 'disconnected' | 'error' | 'testing' | 'disabled'
}

export interface DataSourcePermission {
  dataSourceId: string
  dataSourceName: string
  tables: string[]
}

export interface UserPermissionAssignment {
  id: string
  userId: string
  username: string
  permissions: DataSourcePermission[]
}

export interface UnassignedUser {
  id: string
  username: string
  email: string
  regTime: string
}

export interface ConnectionLog {
  id: string | number
  time: string
  datasource: string
  status: string
  details?: string
  remark?: string
}

export interface PermissionLog {
  id: string | number
  timestamp: string
  text: string
}

// ==================== 聊天相关类型 ====================
export interface ChatMessage {
  id: string
  content: string | QueryResultData  // 支持文本或查询结果
  isSent: boolean
  timestamp: Date
  isRead: boolean
  contentType?: 'text' | 'query_share'  // 消息内容类型
}

export interface ChatModalProps {
  isOpen: boolean
  onClose: () => void
  friend: Friend
  savedQueries: QueryResultData[]
  currentUnreadCount: number
  updateUnreadCount: (friendId: string, count: number) => void
  messages: ChatMessage[]
  updateMessages: (newMessages: ChatMessage[]) => void
}

// ==================== 好友页面类型 ====================
export type ActiveTab = 'friends' | 'requests' | 'shares'

export interface FriendsPageProps {
  savedQueries: QueryResultData[]
  shares: QueryShare[]
  onMarkShareAsRead: (shareId: string) => void
  onDeleteShare: (shareId: string) => void
  onRerunQuery: (prompt: string) => void
  onSaveQuery: (query: QueryResultData) => void
}
