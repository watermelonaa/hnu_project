/**
 * @file services/api/llm.ts
 * @description 大模型配置相关 API
 *
 * 接口：
 * - llmConfigApi: 大模型 CRUD、启用/禁用
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 大模型配置接口 ====================

export interface LlmConfig {
  id: number
  name: string
  version: string
  apiKey?: string
  apiUrl: string
  statusId: number
  isDisabled: number
  timeout: number
}

export const llmConfigApi = {
  getList: async (): Promise<LlmConfig[]> => {
    return await request<LlmConfig[]>('/llm-config/list')
  },

  getAvailable: async (): Promise<LlmConfig[]> => {
    return await request<LlmConfig[]>('/llm-config/list/available')
  },

  getById: async (id: number): Promise<LlmConfig> => {
    return await request<LlmConfig>(`/llm-config/${id}`)
  },

  create: async (llmConfig: Partial<LlmConfig>): Promise<LlmConfig> => {
    return await request<LlmConfig>('/llm-config', {
      method: 'POST',
      body: JSON.stringify(llmConfig),
    })
  },

  update: async (llmConfig: Partial<LlmConfig>): Promise<LlmConfig> => {
    return await request<LlmConfig>('/llm-config', {
      method: 'PUT',
      body: JSON.stringify(llmConfig),
    })
  },

  delete: async (id: number): Promise<void> => {
    return await request<void>(`/llm-config/${id}`, {
      method: 'DELETE',
    })
  },

  toggle: async (id: number): Promise<void> => {
    return await request<void>(`/llm-config/${id}/toggle`, {
      method: 'PUT',
    })
  },
}

