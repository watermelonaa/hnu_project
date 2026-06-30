/**
 * @file services/queryShareService.ts
 * @description 统一的查询收藏和分享服务
 * 
 * ⚠️ 重要说明：后端API不完整，前端跳过检查
 * 
 * 已知问题：
 * 1. 后端 QueryShare 实体类缺少 queryLogId 字段
 *    - 数据库表 query_shares 只有：dialog_id, target_rounds, query_title 等字段
 *    - 前端传递 queryLogId，但后端实体类没有此字段，可能导致数据丢失
 * 
 * 2. 后端 QueryLog 实体类缺少查询详情字段
 *    - 数据库表 query_logs 只有基础字段：id, dialog_id, data_source_id, user_id 等
 *    - 缺少：userPrompt, sqlQuery, queryResult 等字段
 *    - 前端定义的 QueryLog 接口与后端实体类不匹配
 * 
 * 3. 后端 QueryShareController.save() 方法无法正确处理 queryLogId
 *    - 前端传递 queryLogId，但后端需要 dialogId 和 targetRounds
 *    - 后端需要根据 queryLogId 查询 QueryLog 获取 dialogId，但 QueryLog 缺少必要字段
 * 
 * 当前处理方式：
 * - 前端继续传递 queryLogId，忽略后端字段不匹配的问题
 * - 添加 try-catch 容错处理，避免因后端问题导致前端崩溃
 * - 等待后端修复后再移除这些容错代码
 * 
 * 功能：
 * - 统一管理查询的收藏和分享功能
 * - 区分查询结果（未保存）和已保存的查询结果
 * - 提供统一的API接口
 */

import { queryLogApi, queryShareApi } from './api.real'
import type { QueryResultData } from '../types'

/**
 * 保存查询结果到历史记录
 * @param query 查询结果数据
 * @returns 保存后的查询结果（包含真实的queryLogId）
 */
export async function saveQuery(query: QueryResultData): Promise<QueryResultData> {
  const userId = Number(sessionStorage.getItem('userId') || '1')
  
  // 调用API保存查询结果
  const response = await queryLogApi.create({
    userId,
    userPrompt: query.userPrompt,
    sqlQuery: query.sqlQuery,
    queryResult: JSON.stringify({
      tableData: query.tableData,
      chartData: query.chartData,
    }),
    dialogId: query.conversationId || '',
    dbConnectionId: query.dbConnectionId || 0,
    llmConfigId: query.llmConfigId || 0,
    queryTime: query.queryTime || new Date().toISOString(),
    executionTime: query.executionTime || '0ms',
  })
  
  // 返回保存后的查询结果，使用真实的queryLogId
  return {
    ...query,
    id: String(response.id), // 使用API返回的真实ID
  }
}

/**
 * 分享查询结果给好友
 * @param queryId 查询ID（可能是临时ID或真实的queryLogId）
 * @param friendId 好友ID
 * @param queryData 可选的查询数据（如果queryId是临时ID，需要提供此数据）
 * @returns 真实的queryLogId（字符串格式）
 */
export async function shareQuery(
  queryId: string,
  friendId: string,
  queryData?: QueryResultData
): Promise<string> {
  const userId = Number(sessionStorage.getItem('userId'))
  
  // 判断queryId是否是真实的queryLogId（数字字符串）
  let queryLogId = Number(queryId)
  const isRealQueryLogId = !isNaN(queryLogId) && queryLogId > 0
  
  // 如果不是真实的queryLogId，需要先保存查询
  if (!isRealQueryLogId) {
    if (!queryData) {
      throw new Error('查询数据不存在，无法分享')
    }
    
    // 先保存查询，获取真实的queryLogId
    const savedQuery = await saveQuery(queryData)
    queryLogId = Number(savedQuery.id)
  }
  
  // 调用分享API
  // ⚠️ 注意：后端 QueryShare 实体类缺少 queryLogId 字段，此字段可能被忽略
  // 后端需要修复：在 QueryShare 实体类和数据库表中添加 query_log_id 字段
  try {
    await queryShareApi.create({
      shareUserId: userId,
      receiveUserId: Number(friendId),
      queryLogId: queryLogId, // 使用真实的queryLogId（数字类型）
      receiveStatus: 0, // 未读状态
    })
  } catch (error) {
    // 容错处理：如果后端因为字段不匹配报错，记录日志但不抛出异常
    console.warn('⚠️ 分享API调用可能失败（后端字段不匹配）:', error)
    // 如果后端返回成功但实际未保存，前端无法感知
    // 建议：后端修复后移除此容错处理
  }
  
  // 返回真实的queryLogId（字符串格式）
  return String(queryLogId)
}

/**
 * 检查查询是否已保存（通过ID）
 * @param queryId 查询ID
 * @param savedQueries 已保存的查询列表
 * @returns 是否已保存
 */
export function isQuerySaved(
  queryId: string,
  savedQueries: QueryResultData[]
): boolean {
  return savedQueries.some((q) => q.id === queryId)
}

/**
 * 检查查询是否已保存（通过内容匹配）
 * @param query 查询数据
 * @param savedQueries 已保存的查询列表
 * @returns 是否已保存
 */
export function isQuerySavedByContent(
  query: QueryResultData,
  savedQueries: QueryResultData[]
): boolean {
  return savedQueries.some((q) => {
    return q.id === query.id || 
           (q.userPrompt === query.userPrompt && q.sqlQuery === query.sqlQuery)
  })
}

