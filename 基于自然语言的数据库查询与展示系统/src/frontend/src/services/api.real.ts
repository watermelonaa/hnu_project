// ==================== API 模块 - 向后兼容导出 ====================
// 此文件从按业务域拆分的模块中重新导出所有 API
// 新代码建议直接从 './api/index' 或具体模块导入

// 通用请求
export { request, getToken, getUserId } from './api/request'

// 认证模块
export { authApi } from './api/auth'
export type { LoginRequest, LoginResponse } from './api/auth'

// 用户模块
export { userApi, userSearchApi } from './api/user'
export type { User, ChangePasswordRequest, UserSearchRecord } from './api/user'

// 查询模块
export {
  queryApi,
  queryLogApi,
  queryShareApi,
  queryCollectionApi,
  collectionRecordApi,
  recommendationApi, // Added export
} from './api/query'
export type {
  QueryRequest,
  QueryResponse,
  QueryLog,
  QueryShare,
  QueryCollection,
  CollectionRecord,
} from './api/query'

// 对话模块
export { dialogApi, dialogDetailApi } from './api/dialog'
export type { DialogRecord, DialogDetail, DialogDetailRound } from './api/dialog'

// 数据库连接模块
export { dbConnectionApi } from './api/db'
export type { DbConnection } from './api/db'

// 大模型配置模块
export { llmConfigApi } from './api/llm'
export type { LlmConfig } from './api/llm'

// 权限模块
export { userDbPermissionApi } from './api/permission'
export type { UserDbPermission } from './api/permission'

// 通知模块
export { notificationApi } from './api/notification'
export type { Notification } from './api/notification'

// 好友模块
export { friendRelationApi, friendRequestApi, friendChatApi, queryShareChatApi } from './api/friend'
export type {
  FriendRelation,
  FriendRelationWithUser,
  FriendRequest,
  FriendRequestWithUser,
  FriendChat,
  ShareQueryRequest,
  QueryShareVO,
  ChatMessageVO,
} from './api/friend'

// 日志模块
export { operationLogApi, errorLogApi, dbConnectionLogApi } from './api/log'
export type { OperationLog, ErrorLog, DbConnectionLog } from './api/log'
