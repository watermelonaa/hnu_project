/**
 * 好友页面类型 - 向后兼容导出
 * 新代码请从 './index' 导入
 */
export type { ActiveTab, FriendsPageProps } from './index'

// 为了兼容旧的导入方式，重新导出 Message 别名
export type { ChatMessage as Message } from './index'
