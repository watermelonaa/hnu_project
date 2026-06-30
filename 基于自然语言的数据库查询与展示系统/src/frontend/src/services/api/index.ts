/**
 * @file services/api/index.ts
 * @description API 模块统一导出入口
 *
 * 模块划分：
 * - auth: 认证相关（登录/登出）
 * - user: 用户管理
 * - query: 查询相关（执行/日志/分享/收藏）
 * - dialog: 对话管理
 * - db: 数据库连接
 * - llm: 大模型配置
 * - permission: 用户权限
 * - notification: 通知管理
 * - friend: 好友关系
 * - log: 操作日志
 *
 * @example
 * // 推荐：从统一入口导入
 * import { authApi, userApi, queryApi } from '@/services/api'
 *
 * // 或从具体模块导入
 * import { authApi } from '@/services/api/auth'
 *
 * @author Frontend Team
 * @since 1.0.0
 */

// ==================== API 模块统一导出 ====================

// 通用请求
export { request, getToken, getUserId } from './request'

// 认证模块
export { authApi } from './auth'
export type { LoginRequest, LoginResponse } from './auth'

// 用户模块
export { userApi, userSearchApi } from './user'
export type { User, ChangePasswordRequest, UserSearchRecord } from './user'

// 查询模块
export {
  queryApi,
  queryLogApi,
  queryShareApi,
  queryCollectionApi,
  collectionRecordApi,
} from './query'
export type {
  QueryRequest,
  QueryResponse,
  QueryLog,
  QueryShare,
  QueryCollection,
  CollectionRecord,
} from './query'

// 对话模块
export { dialogApi, dialogDetailApi } from './dialog'
export type { DialogRecord, DialogDetail, DialogDetailRound } from './dialog'

// 数据库连接模块
export { dbConnectionApi } from './db'
export type { DbConnection } from './db'

// 大模型配置模块
export { llmConfigApi } from './llm'
export type { LlmConfig } from './llm'

// 权限模块
export { userDbPermissionApi } from './permission'
export type { UserDbPermission } from './permission'

// 通知模块
export { notificationApi } from './notification'
export type { Notification } from './notification'

// 好友模块
export { friendRelationApi, friendRequestApi, friendChatApi, queryShareChatApi } from './friend'
export type {
  FriendRelation,
  FriendRelationWithUser,
  FriendRequest,
  FriendRequestWithUser,
  FriendChat,
  ShareQueryRequest,
  QueryShareVO,
  ChatMessageVO,
} from './friend'

// 日志模块
export { operationLogApi, errorLogApi, dbConnectionLogApi } from './log'
export type { OperationLog, ErrorLog, DbConnectionLog } from './log'

// 仪表盘模块
export { dashboardApi, systemHealthApi } from './dashboard'
export type { DashboardStats, SystemHealth } from './dashboard'
