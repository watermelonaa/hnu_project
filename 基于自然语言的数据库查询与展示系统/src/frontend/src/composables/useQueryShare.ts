/**
 * @fileoverview 查询分享操作的组合式函数（Composable）。
 * 用于管理查询分享、标记已读和删除分享等操作的状态和逻辑。
 */

// 从 Vue 导入响应式 API
import { ref } from 'vue'
import type { Ref } from 'vue'
import { queryShareApi } from '../services/api.real'

// 定义返回类型接口，用于 JSDoc 提示和类型安全
export interface QueryShareResult {
  shareQuery: (queryLogId: number, receiveUserId: number) => Promise<boolean>
  markAsRead: (shareId: number) => Promise<boolean>
  deleteShare: (shareId: number) => Promise<boolean>
  loading: Ref<boolean>
  error: Ref<string | null>
}

/**
 * 查询分享操作的组合式函数。
 * * @returns {QueryShareResult} 包含操作函数、加载状态和错误信息的对象。
 */
export const useQueryShare = (): QueryShareResult => {
  /**
   * 当前操作的加载状态。
   * @type {Ref<boolean>}
   */
  const loading = ref(false)

  /**
   * 当前操作的错误信息。
   * @type {Ref<string | null>}
   */
  const error = ref<string | null>(null)

  /**
   * 分享查询日志。
   * * @param {number} queryLogId - 要分享的查询日志 ID。
   * @param {number} receiveUserId - 接收用户的 ID。
   * @returns {Promise<boolean>} 成功返回 true，失败返回 false。
   */
  const shareQuery = async (queryLogId: number, receiveUserId: number): Promise<boolean> => {
    loading.value = true
    error.value = null
    try {
      // 从 sessionStorage 获取用户 ID
      const shareUserId = Number(sessionStorage.getItem('userId') || '1')

      await queryShareApi.create({
        shareUserId,
        receiveUserId,
        queryLogId,
        receiveStatus: 0,
      })
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '分享失败'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 将分享标记为已读。
   * * @param {number} shareId - 分享记录的 ID。
   * @returns {Promise<boolean>} 成功返回 true，失败返回 false。
   */
  const markAsRead = async (shareId: number): Promise<boolean> => {
    // 标记操作通常较快，可以不设置 loading，但为了统一流程，这里保留。
    loading.value = true
    error.value = null
    try {
      await queryShareApi.update({
        id: shareId,
        receiveStatus: 1,
      })
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '标记失败'
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 删除分享记录。
   * * @param {number} shareId - 分享记录的 ID。
   * @returns {Promise<boolean>} 成功返回 true，失败返回 false。
   */
  const deleteShare = async (shareId: number): Promise<boolean> => {
    loading.value = true
    error.value = null
    try {
      await queryShareApi.delete(shareId)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除失败'
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    shareQuery,
    markAsRead,
    deleteShare,
    loading,
    error,
  }
}
