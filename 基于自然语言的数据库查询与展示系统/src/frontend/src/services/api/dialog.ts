/**
 * @file services/api/dialog.ts
 * @description 对话相关 API
 *
 * 接口：
 * - dialogApi: 对话/会话 CRUD
 * - dialogDetailApi: 对话详情（消息）CRUD
 *
 * @author Frontend Team
 */
import { request } from './request'

// ==================== 对话接口 ====================

export interface DialogRecord {
  dialogId: string
  userId: number
  topic: string
  totalRounds: number
  startTime: string
  lastTime: string
}

export interface DialogDetailRound {
  roundNum: number
  userInput: string
  aiResponse: string
  generatedSql: string
  roundTime: string
}

export interface DialogDetail {
  id: string
  dialogId: string
  rounds: DialogDetailRound[]
}

export const dialogApi = {
  getList: async (): Promise<DialogRecord[]> => {
    return await request<DialogRecord[]>('/dialog/list')
  },

  getById: async (dialogId: string): Promise<DialogRecord> => {
    return await request<DialogRecord>(`/dialog/${dialogId}`)
  },

  create: async (dialog: Partial<DialogRecord>): Promise<DialogRecord> => {
    return await request<DialogRecord>('/dialog', {
      method: 'POST',
      body: JSON.stringify(dialog),
    })
  },

  update: async (dialogId: string, dialog: Partial<DialogRecord>): Promise<DialogRecord> => {
    return await request<DialogRecord>(`/dialog/${dialogId}`, {
      method: 'PUT',
      body: JSON.stringify(dialog),
    })
  },

  delete: async (dialogId: string): Promise<void> => {
    return await request<void>(`/dialog/${dialogId}`, {
      method: 'DELETE',
    })
  },
}

// ==================== 对话详情接口 ====================

export const dialogDetailApi = {
  getByDialogId: async (dialogId: string): Promise<DialogDetail> => {
    return await request<DialogDetail>(`/dialog-detail/${dialogId}`)
  },

  save: async (detail: Partial<DialogDetail>): Promise<DialogDetail> => {
    return await request<DialogDetail>('/dialog-detail', {
      method: 'POST',
      body: JSON.stringify(detail),
    })
  },

  update: async (detail: Partial<DialogDetail>): Promise<DialogDetail> => {
    return await request<DialogDetail>('/dialog-detail', {
      method: 'PUT',
      body: JSON.stringify(detail),
    })
  },

  delete: async (id: string): Promise<void> => {
    return await request<void>(`/dialog-detail/${id}`, {
      method: 'DELETE',
    })
  },
}
