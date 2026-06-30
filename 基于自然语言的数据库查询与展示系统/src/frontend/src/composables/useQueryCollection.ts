/**
 * @file composables/useQueryCollection.ts
 * @description 查询收藏夹组合式函数
 *
 * 功能：
 * - 收藏夹列表管理
 * - 收藏夹 CRUD 操作
 * - 收藏记录管理
 *
 * @author Frontend Team
 */
import { ref, watch } from 'vue'
import type { Ref } from 'vue'
import { queryCollectionApi, collectionRecordApi } from '../services/api.real'

// --- 接口定义 ---

export interface QueryCollection {
  id: string | number  // MongoDB 返回的是 String，但前端可能使用 number
  userId: number
  collectionName: string
  description?: string
  createTime: string
}

export interface CollectionRecord {
  id: string
  collectionId: number
  queryLogId: number
  addTime: string
}

// --- 核心 Composition Function ---

/**
 * 管理用户的查询收藏夹及其记录。
 * * @param userId - 当前用户的 ID，可以是一个响应式的 Ref 或普通数字。
 */
export const useQueryCollection = (userId: Ref<number> | number) => {
  // 响应式状态：使用 ref 替代 useState
  const collections = ref<QueryCollection[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 辅助函数：获取 userId 的当前值，以处理 Ref 或普通数字输入
  const getUserIdValue = (): number => {
    return typeof userId === 'object' && 'value' in userId ? userId.value : (userId as number)
  }

  /** 获取或创建默认收藏夹 */
  const getOrCreateDefaultCollection = async (): Promise<QueryCollection | null> => {
    const currentUserId = getUserIdValue()
    if (!currentUserId) return null

    // 先加载所有收藏夹
    await loadCollections()
    
    // 查找默认收藏夹（名称为"默认收藏夹"）
    let defaultCollection = collections.value.find(c => c.collectionName === '默认收藏夹')
    
    // 如果没有默认收藏夹，创建一个
    if (!defaultCollection) {
      defaultCollection = await createCollection('默认收藏夹', '系统自动创建的默认收藏夹')
    }
    
    return defaultCollection
  }

  /** 列表加载逻辑 */
  const loadCollections = async () => {
    const currentUserId = getUserIdValue()
    if (!currentUserId) return

    loading.value = true
    error.value = null
    try {
      const data = await queryCollectionApi.getByUser(currentUserId)
      collections.value = data
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载收藏夹失败'
    } finally {
      loading.value = false
    }
  }

  // 监听 userId 变化并加载数据
  watch(
    () => getUserIdValue(),
    (newUserId) => {
      if (newUserId) {
        loadCollections()
      } else {
        collections.value = [] // 用户 ID 清空时，清空数据
      }
    },
    { immediate: true }, // 立即执行一次，模拟组件挂载时的初始加载
  )

  /** 创建新的收藏夹 */
  const createCollection = async (name: string, description?: string) => {
    loading.value = true
    error.value = null
    try {
      const newCollection = await queryCollectionApi.create({
        userId: getUserIdValue(),
        collectionName: name,
        description,
      })
      collections.value = [...collections.value, newCollection]
      return newCollection
    } catch (err) {
      error.value = err instanceof Error ? err.message : '创建收藏夹失败'
      return null
    } finally {
      loading.value = false
    }
  }

  /** 更新收藏夹信息 */
  const updateCollection = async (id: number | string, name: string, description?: string) => {
    loading.value = true
    error.value = null
    try {
      const currentUserId = getUserIdValue()
      const updated = await queryCollectionApi.update({
        id: typeof id === 'string' ? id : String(id),
        userId: currentUserId, // 确保传递 userId，后端需要此字段
        collectionName: name,
        description,
      })
      // ⚠️ 状态更新逻辑全部使用 .value
      collections.value = collections.value.map((c) => (String(c.id) === String(id) ? updated : c))
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新收藏夹失败'
      return null
    } finally {
      loading.value = false
    }
  }

  /** 删除收藏夹 */
  const deleteCollection = async (id: number | string) => {
    loading.value = true
    error.value = null
    try {
      const idStr = typeof id === 'string' ? id : String(id)
      await queryCollectionApi.delete(idStr)
      // ⚠️ 状态更新逻辑全部使用 .value
      collections.value = collections.value.filter((c) => String(c.id) !== idStr)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除收藏夹失败'
      return false
    } finally {
      loading.value = false
    }
  }

  /** 添加查询记录到收藏夹 */
  const addQueryToCollection = async (
    collectionId: number | string, 
    queryLogId: number,
    queryData?: {
      userId?: number
      sqlContent?: string
      userPrompt?: string
      queryResult?: any
      dbConnectionId?: number
      llmConfigId?: number
    }
  ) => {
    loading.value = true
    error.value = null
    try {
      // MongoDB 返回的 ID 是字符串（ObjectId），后端现在支持 String 和 Number
      // 直接使用原始值，后端会处理类型转换
      console.log('添加到收藏夹: collectionId=', collectionId, 'type=', typeof collectionId)
      
      // 后端支持 String 和 Number 类型的 collectionId（MongoDB ObjectId 是字符串）
      // 但前端接口定义中 collectionId 是 number，使用类型断言绕过
      await collectionRecordApi.create({
        collectionId: collectionId as any, // 后端实际支持 String 和 Number，但前端类型定义是 number
        queryLogId,
        ...(queryData || {}),
      } as any)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '添加到收藏夹失败'
      console.error('添加到收藏夹失败:', err)
      return false
    } finally {
      loading.value = false
    }
  }

  /** 从收藏夹移除查询记录 */
  const removeQueryFromCollection = async (recordId: string) => {
    loading.value = true
    error.value = null
    try {
      await collectionRecordApi.delete(recordId)
      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '从收藏夹移除失败'
      return false
    } finally {
      loading.value = false
    }
  }

  /** 获取某个收藏夹下的所有记录 */
  const getCollectionRecords = async (collectionId: string) => {
    loading.value = true
    error.value = null
    try {
      const records = await collectionRecordApi.getByCollection(collectionId)
      return records
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取收藏记录失败'
      return []
    } finally {
      loading.value = false
    }
  }

  // 返回响应式状态和方法。
  // 在 Vue 组件的 <template> 中，这些 ref 会被自动解包。
  return {
    collections,
    loading,
    error,
    loadCollections,
    createCollection,
    updateCollection,
    deleteCollection,
    addQueryToCollection,
    removeQueryFromCollection,
    getCollectionRecords,
    getOrCreateDefaultCollection,
  }
}
