/**
 * @file request.ts
 * @description HTTP 请求基础模块
 *
 * 职责：
 * - 提供统一的 HTTP 请求封装
 * - 自动处理认证 Token
 * - 统一处理响应格式和错误
 * - 自动记录网络异常到后端日志
 *
 * @author Frontend Team
 * @since 1.0.0
 */

import { logOperation, LogModule, LogOperationType, LogStatus } from '../../utils/logger'

// ==================== 全屏错误弹窗 ====================

/**
 * 显示全屏错误弹窗
 * 
 * @param message 错误信息
 * @param onConfirm 点击确定后的回调函数
 */
function showErrorModal(message: string, onConfirm?: () => void) {
  // 创建遮罩层
  const overlay = document.createElement('div')
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
    animation: fadeIn 0.3s ease-out;
  `

  // 创建弹窗内容
  const modal = document.createElement('div')
  modal.style.cssText = `
    background: white;
    border-radius: 12px;
    padding: 32px;
    max-width: 400px;
    width: 90%;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    animation: slideIn 0.3s ease-out;
  `

  // 创建标题
  const title = document.createElement('h3')
  title.textContent = '提示'
  title.style.cssText = `
    font-size: 20px;
    font-weight: bold;
    margin: 0 0 16px 0;
    color: #1f2937;
  `

  // 创建消息内容
  const content = document.createElement('p')
  content.textContent = message
  content.style.cssText = `
    font-size: 16px;
    line-height: 1.5;
    margin: 0 0 24px 0;
    color: #4b5563;
  `

  // 创建确定按钮
  const button = document.createElement('button')
  button.textContent = '确定'
  button.style.cssText = `
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 8px;
    padding: 12px 32px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    width: 100%;
    transition: background 0.2s;
  `
  
  button.onmouseover = () => {
    button.style.background = '#2563eb'
  }
  button.onmouseout = () => {
    button.style.background = '#3b82f6'
  }

  button.onclick = () => {
    overlay.remove()
    if (onConfirm) {
      onConfirm()
    }
  }

  // 添加动画样式
  const style = document.createElement('style')
  style.textContent = `
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `
  document.head.appendChild(style)

  // 组装弹窗
  modal.appendChild(title)
  modal.appendChild(content)
  modal.appendChild(button)
  overlay.appendChild(modal)
  document.body.appendChild(overlay)
}

// ==================== 配置 ====================

/**
 * API 基础 URL
 * 优先从环境变量 VITE_API_BASE_URL 读取，否则使用默认值 '/api'
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? '/api' : '/api')

// ==================== Token 管理 ====================

/**
 * 获取认证 Token
 * 使用 sessionStorage 存储，每个标签页独立
 *
 * @returns {string | null} Token 字符串或 null
 */
export const getToken = (): string | null => {
  return sessionStorage.getItem('token')
}

/**
 * 获取当前用户 ID
 * 使用 sessionStorage 存储，每个标签页独立
 *
 * @returns {string | null} 用户 ID 或 null
 */
export const getUserId = (): string | null => {
  return sessionStorage.getItem('userId')
}

// ==================== 请求函数 ====================

/**
 * 通用 HTTP 请求函数
 *
 * 功能：
 * - 自动添加 Content-Type 和认证头
 * - 统一处理后端 Result 响应格式
 * - 自动解析 JSON 响应
 * - 统一错误处理
 *
 * @template T - 响应数据类型
 * @param {string} endpoint - API 端点路径（如 '/user/list'）
 * @param {RequestInit} options - fetch 请求选项
 * @returns {Promise<T>} 响应数据
 * @throws {Error} 请求失败或业务错误时抛出
 *
 * @example
 * // GET 请求
 * const users = await request<User[]>('/user/list')
 *
 * @example
 * // POST 请求
 * const result = await request<LoginResponse>('/auth/login', {
 *   method: 'POST',
 *   body: JSON.stringify({ username, password })
 * })
 */
export async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const userId = getUserId()

  // 构建请求头
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  // 添加认证 Token（Bearer 格式）
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  // 添加用户 ID（部分接口需要）
  if (userId) {
    headers['userId'] = userId
  }

  try {
    // 发送请求
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    })

    // 处理 HTTP 错误
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: '请求失败' }))
      console.error('HTTP错误:', response.status, errorData)
      
      // 处理401未授权错误（Token过期或无效）
      if (response.status === 401) {
        const errorMessage = errorData.message || '登录已过期，请重新登录'
        // 清除本地存储的认证信息
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userId')
        sessionStorage.removeItem('username')
        sessionStorage.removeItem('roleId')
        sessionStorage.removeItem('roleName')
        sessionStorage.removeItem('userRole')
        // 显示全屏弹窗并在点击确定后刷新页面
        showErrorModal(errorMessage, () => {
          window.location.reload()
        })
        throw new Error(errorMessage)
      }
      
      // 处理403禁止访问错误（账号被禁用）
      if (response.status === 403) {
        const errorMessage = errorData.message || '账号已被禁用，请联系管理员'
        // 清除本地存储的认证信息
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userId')
        sessionStorage.removeItem('username')
        sessionStorage.removeItem('roleId')
        sessionStorage.removeItem('roleName')
        sessionStorage.removeItem('userRole')
        // 显示全屏弹窗并在点击确定后刷新页面回到登录界面
        showErrorModal(errorMessage, () => {
          window.location.reload()
        })
        throw new Error(errorMessage)
      }
      
      // 注意：HTTP错误（如500系统异常）不应该在这里记录日志
      // 因为后端GlobalExceptionHandler已经记录了异常日志
      // 前端只记录网络连接错误（无法连接到服务器的情况）
      
      const errorMessage = errorData.message || `HTTP ${response.status}: ${response.statusText}`
      throw new Error(errorMessage)
    }

    const data = await response.json()
    console.log('收到响应数据:', data)

    // 处理后端统一 Result 格式：{ code, message, data }
    if (data.code !== undefined) {
      if (data.code === 200 || data.code === 0) {
        console.log('解析Result格式响应，返回data:', data.data)
        return data.data as T
      } else {
        console.error('Result格式错误:', data.code, data.message)
        throw new Error(data.message || '请求失败')
      }
    }

    // 直接返回数据（非 Result 格式）
    console.log('非Result格式响应，直接返回:', data)
    return data as T
  } catch (error) {
    // 捕获网络异常（如连接失败、超时等）
    if (error instanceof TypeError && error.message.includes('fetch')) {
      // 网络连接异常（如后端服务未启动、网络断开等）
      const networkError = error.message.includes('Failed to fetch') 
        ? '无法连接到服务器，请检查网络或后端服务是否启动'
        : `网络异常: ${error.message}`
      
      console.error('网络异常:', networkError, error)
      
      // 记录网络异常到日志
      await logOperation(
        '网络异常', // 模块名称：网络异常
        '网络连接失败', // 操作类型
        `网络异常: ${endpoint} - ${networkError}`,
        LogStatus.FAILURE
      ).catch(() => {
        // 日志记录失败不影响错误抛出（如果后端服务未启动，这里会失败）
      })
      
      throw new Error(networkError)
    }
    
    // 其他异常直接抛出（已在上面记录过日志的HTTP错误等）
    throw error
  }
}
