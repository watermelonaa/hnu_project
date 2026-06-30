/**
 * 查询分享消息处理工具函数
 * 统一处理查询分享消息的解析逻辑，避免重复代码
 */

import type { QueryResultData } from '../types'
import { queryLogApi } from '../services/api.real'

/**
 * 解析查询分享消息内容
 * @param messageContent 消息内容（可能是对象或字符串）
 * @param messageId 消息ID
 * @param savedQueries 已保存的查询列表（可选，用于查找）
 * @returns 解析后的消息内容和类型
 */
export async function parseQueryShareMessage(
  messageContent: any,
  messageId: string,
  savedQueries: QueryResultData[] = []
): Promise<{ content: string | QueryResultData; contentType: 'text' | 'query_share' }> {
  const shareContent = typeof messageContent === 'object' ? messageContent : {}
  
  // 如果后端直接返回了 tableData 和 chartData，直接使用
  if (shareContent.tableData || shareContent.chartData) {
    try {
      const queryData: QueryResultData = {
        id: String(shareContent.queryId || shareContent.queryLogId || messageId || Date.now()),
        userPrompt: shareContent.queryTitle || shareContent.title || '查询结果',
        sqlQuery: shareContent.sqlQuery || '',
        conversationId: shareContent.dialogId || '',
        queryTime: shareContent.queryTime || new Date().toISOString(),
        executionTime: shareContent.executionTimeText || String(shareContent.executionTime || '0ms'),
        database: shareContent.databaseName || shareContent.database || '',
        model: shareContent.llmName || shareContent.model || '',
        tableData: shareContent.tableData || { headers: [], rows: [] },
        chartData: shareContent.chartData,
        dbConnectionId: shareContent.dbConnectionId,
        llmConfigId: shareContent.llmConfigId,
      }
      return { content: queryData, contentType: 'query_share' }
    } catch (error) {
      console.error('解析分享内容失败:', error)
      return { content: `分享了查询：${shareContent.queryTitle || shareContent.title || '查询结果'}`, contentType: 'text' }
    }
  }
  
  // 如果没有直接的数据，尝试通过 queryId 获取
  const queryId = shareContent.queryId || shareContent.queryLogId
  
  if (queryId) {
    // 从 savedQueries 中查找
    let queryData = savedQueries.find(q => q.id === String(queryId))
    
    if (!queryData) {
      // 验证 queryId 是否为有效的数字
      const numericQueryId = Number(queryId)
      if (!isNaN(numericQueryId) && numericQueryId > 0) {
        // 从 API 获取查询详情
        try {
          const queryLog = await queryLogApi.getById(numericQueryId)
          const queryResult = queryLog.queryResult ? JSON.parse(queryLog.queryResult) : {}
          
          queryData = {
            id: String(queryLog.id),
            userPrompt: queryLog.userPrompt || '',
            sqlQuery: queryLog.sqlQuery || '',
            conversationId: queryLog.dialogId || '',
            queryTime: queryLog.queryTime || new Date().toISOString(),
            executionTime: queryLog.executionTime || '0ms',
            database: queryLog.dbConnectionId?.toString() || '',
            model: queryLog.llmConfigId?.toString() || '',
            tableData: queryResult.tableData || { headers: [], rows: [] },
            chartData: queryResult.chartData,
            dbConnectionId: queryLog.dbConnectionId,
            llmConfigId: queryLog.llmConfigId,
          }
        } catch (error) {
          console.error('获取查询详情失败:', error)
          // 如果获取失败，仍然返回查询分享类型，但使用基本信息
          return {
            content: {
              id: String(queryId),
              userPrompt: shareContent.queryTitle || shareContent.title || '查询结果',
              sqlQuery: shareContent.sqlQuery || '',
              conversationId: shareContent.dialogId || '',
              queryTime: shareContent.queryTime || new Date().toISOString(),
              executionTime: shareContent.executionTimeText || String(shareContent.executionTime || '0ms'),
              database: shareContent.databaseName || shareContent.database || '',
              model: shareContent.llmName || shareContent.model || '',
              tableData: { headers: [], rows: [] },
              dbConnectionId: shareContent.dbConnectionId,
              llmConfigId: shareContent.llmConfigId,
            } as QueryResultData,
            contentType: 'query_share'
          }
        }
      } else {
        // queryId 无效，使用基本信息构建
        console.warn('queryId 无效，使用基本信息构建查询数据:', queryId)
        return {
          content: {
            id: String(queryId),
            userPrompt: shareContent.queryTitle || shareContent.title || '查询结果',
            sqlQuery: shareContent.sqlQuery || '',
            conversationId: shareContent.dialogId || '',
            queryTime: shareContent.queryTime || new Date().toISOString(),
            executionTime: shareContent.executionTimeText || String(shareContent.executionTime || '0ms'),
            database: shareContent.databaseName || shareContent.database || '',
            model: shareContent.llmName || shareContent.model || '',
            tableData: { headers: [], rows: [] },
            dbConnectionId: shareContent.dbConnectionId,
            llmConfigId: shareContent.llmConfigId,
          } as QueryResultData,
          contentType: 'query_share'
        }
      }
    }
    
    if (queryData) {
      return { content: queryData, contentType: 'query_share' }
    }
  }
  
  // 如果没有queryId，使用基本信息构建查询数据
  return {
    content: {
      id: String(messageId || Date.now()),
      userPrompt: shareContent.queryTitle || shareContent.title || '查询结果',
      sqlQuery: shareContent.sqlQuery || '',
      conversationId: shareContent.dialogId || '',
      queryTime: shareContent.queryTime || new Date().toISOString(),
      executionTime: shareContent.executionTimeText || String(shareContent.executionTime || '0ms'),
      database: shareContent.databaseName || shareContent.database || '',
      model: shareContent.llmName || shareContent.model || '',
      tableData: { headers: [], rows: [] },
      dbConnectionId: shareContent.dbConnectionId,
      llmConfigId: shareContent.llmConfigId,
    } as QueryResultData,
    contentType: 'query_share'
  }
}

