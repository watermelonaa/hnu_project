/**
 * @file services/api/dashboard.ts
 * @description 仪表盘统计 API
 */
import { request } from './request'

export interface DashboardStats {
  // 系统管理员统计
  totalUsers?: number
  totalDataSources?: number
  todayQueries?: number
  todayTokenUsage?: number
  errorLogs?: number
  
  // 数据管理员统计
  datasourceCount?: number
  connectedCount?: number
  errorCount?: number
  pendingPermissions?: number
  
  // 图表数据
  queryVolumeData?: Array<{ label: string; value: number }>
  responseTimeData?: Array<{ label: string; value: number }>
  errorChartData?: Array<{ label: string; value: number }>
  costChartData?: Array<{ label: string; value: number }>
  healthStatusData?: Array<{ label: string; value: number }>
  queryLoadData?: Array<{ label: string; value: number }>
  
  // 列表数据
  recentFailures?: Array<{
    id: number
    datasource: string
    time: string
    status: string
    remark?: string
  }>
  recentPermissionLogs?: Array<any>
}

// 系统健康状态
export interface SystemHealth {
  id?: number
  dbDelay?: number
  cacheDelay?: number
  llmDelay?: number
  storageUsage?: number
  collectTime?: string
}

export const dashboardApi = {
  getSysAdminStats: async (): Promise<DashboardStats> => {
    return await request<DashboardStats>('/dashboard/sysadmin')
  },
  
  getDataAdminStats: async (): Promise<DashboardStats> => {
    return await request<DashboardStats>('/dashboard/dataadmin')
  },
}

export const systemHealthApi = {
  // 获取最新一条健康记录
  getLatest: async (): Promise<SystemHealth> => {
    return await request<SystemHealth>('/system-health/latest')
  },
  
  // 获取最近N条健康记录
  listRecent: async (limit: number): Promise<SystemHealth[]> => {
    return await request<SystemHealth[]>(`/system-health/list-recent/${limit}`)
  },
}

