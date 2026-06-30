/**
 * @file services/websocket/chatWebSocket.ts
 * @description WebSocket聊天服务
 *
 * 功能：
 * - 建立WebSocket连接
 * - 发送和接收实时消息
 * - 处理在线状态变化
 * - 处理打字状态
 *
 * @author Frontend Team
 */

import SockJS from 'sockjs-client'
import { Client, IMessage, type IStompSocket } from '@stomp/stompjs'
import type { ChatMessageVO, SendChatMessageDTO } from '../api/friend'

// WebSocket连接配置
const WS_BASE_URL = (import.meta as any).env?.VITE_WS_BASE_URL || 'http://localhost:8173'
const WS_ENDPOINT = '/ws-chat'

// WebSocket客户端实例
let stompClient: Client | null = null
let isConnected = false
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 5

// 消息回调函数类型
export type MessageCallback = (message: ChatMessageVO) => void
export type TypingCallback = (senderId: number) => void
export type OnlineStatusCallback = (userId: number, isOnline: boolean) => void
export type ErrorCallback = (error: string) => void

// 回调函数存储
const messageCallbacks = new Set<MessageCallback>()
const typingCallbacks = new Set<TypingCallback>()
const onlineStatusCallbacks = new Set<OnlineStatusCallback>()
const errorCallbacks = new Set<ErrorCallback>()

/**
 * 连接WebSocket
 * @param userId 当前用户ID
 * @returns Promise<void>
 */
export const connectWebSocket = (userId: number): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (isConnected && stompClient) {
      resolve()
      return
    }

    try {
      // 创建SockJS连接（将userId作为URL参数传递）
      const socketUrl = `${WS_BASE_URL}${WS_ENDPOINT}?userId=${userId}`
      const socket = new SockJS(socketUrl)
      
      // 创建STOMP客户端
      stompClient = new Client({
        webSocketFactory: () => socket as IStompSocket,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
          if ((import.meta as any).env?.DEV) {
            console.log('[STOMP]', str)
          }
        },
        onConnect: (frame) => {
          console.log('WebSocket连接成功', frame)
          console.log('WebSocket连接成功 - userId:', userId, 'frame:', frame)
          isConnected = true
          reconnectAttempts = 0

          // 订阅接收消息队列
          const messageDestination = `/user/${userId}/queue/messages`
          console.log('[WebSocket] 订阅消息队列:', messageDestination, 'userId:', userId)
          
          const subscription = stompClient?.subscribe(messageDestination, (message: IMessage) => {
            try {
              console.log('[WebSocket] 收到消息')
              console.log('[WebSocket] 消息详情:', {
                destination: message.headers.destination,
                subscription: message.headers.subscription,
                messageId: message.headers['message-id'],
                body: message.body,
                '订阅路径': messageDestination,
                'userId': userId
              })
              console.log('[WebSocket] 消息原始内容:', message.body)
              
              const chatMessage: ChatMessageVO = JSON.parse(message.body)
              console.log('[WebSocket] 解析后的消息:', {
                id: chatMessage.id,
                senderId: chatMessage.senderId,
                receiverId: chatMessage.receiverId,
                content: chatMessage.content,
                sendTime: chatMessage.sendTime,
                isRead: chatMessage.isRead,
              })
              console.log('[WebSocket] 当前注册的回调数量:', messageCallbacks.size)
              
              if (messageCallbacks.size === 0) {
                console.warn('[WebSocket] 警告: 没有注册的消息回调！消息将被忽略！')
              }
              
              let callbackIndex = 0
              messageCallbacks.forEach((callback) => {
                callbackIndex++
                console.log('[WebSocket] 调用消息回调 #' + callbackIndex + '/' + messageCallbacks.size)
                try {
                  callback(chatMessage)
                  console.log('[WebSocket] 回调 #' + callbackIndex + ' 执行成功')
                } catch (error) {
                  console.error('[WebSocket] 回调 #' + callbackIndex + ' 执行失败:', error)
                }
              })
              
              console.log('[WebSocket] 消息处理完成')
            } catch (error) {
              console.error('[WebSocket] 解析消息失败:', error, '原始消息:', message.body)
            }
          })
          
          if (subscription) {
            console.log('[WebSocket] 消息队列订阅成功，subscription ID:', subscription.id, 'destination:', messageDestination)
            console.log('[WebSocket] 订阅详情:', {
              subscriptionId: subscription.id,
              destination: messageDestination,
              userId: userId,
              '订阅类型': '点对点消息队列'
            })
          } else {
            console.error('[WebSocket] 消息队列订阅失败！')
          }

          // 订阅打字状态队列
          stompClient?.subscribe(`/user/${userId}/queue/typing`, (message: IMessage) => {
            try {
              const senderId = Number(message.body)
              typingCallbacks.forEach((callback) => callback(senderId))
            } catch (error) {
              console.error('解析打字状态失败:', error)
            }
          })

          // 订阅在线状态广播
          stompClient?.subscribe('/topic/online', (message: IMessage) => {
            try {
              const userId = Number(message.body)
              onlineStatusCallbacks.forEach((callback) => callback(userId, true))
            } catch (error) {
              console.error('解析在线状态失败:', error)
            }
          })

          // 订阅离线状态广播
          stompClient?.subscribe('/topic/offline', (message: IMessage) => {
            try {
              const userId = Number(message.body)
              onlineStatusCallbacks.forEach((callback) => callback(userId, false))
            } catch (error) {
              console.error('解析离线状态失败:', error)
            }
          })

          // 订阅错误队列
          stompClient?.subscribe(`/user/${userId}/queue/errors`, (message: IMessage) => {
            const error = message.body
            errorCallbacks.forEach((callback) => callback(error))
          })

          // 通知用户上线
          sendOnlineStatus(userId)

          resolve()
        },
        onStompError: (frame) => {
          console.error('STOMP错误:', frame)
          isConnected = false
          reject(new Error(frame.headers['message'] || 'WebSocket连接失败'))
        },
        onWebSocketClose: () => {
          console.log('WebSocket连接关闭')
          isConnected = false
          // 尝试重连
          if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            setTimeout(() => {
              if (userId) {
                connectWebSocket(userId).catch(console.error)
              }
            }, 5000 * reconnectAttempts)
          }
        },
        onDisconnect: () => {
          console.log('WebSocket断开连接')
          isConnected = false
        },
      })

      // 设置连接头（传递用户ID）
      // 注意：connectHeaders 中的 userId 会在 CONNECT 帧中传递
      // 后端 WebSocketChannelInterceptor 会从 header 中提取 userId 并设置 Principal
      if (stompClient) {
        stompClient.connectHeaders = {
          userId: String(userId),
        }
        console.log('[WebSocket] 设置连接头，userId:', userId, 'connectHeaders:', stompClient.connectHeaders)
      }

      // 激活连接
      stompClient.activate()
    } catch (error) {
      console.error('创建WebSocket连接失败:', error)
      reject(error)
    }
  })
}

/**
 * 断开WebSocket连接
 */
export const disconnectWebSocket = () => {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    isConnected = false
  }
  // 清空所有回调
  messageCallbacks.clear()
  typingCallbacks.clear()
  onlineStatusCallbacks.clear()
  errorCallbacks.clear()
}

/**
 * 发送私聊消息
 * @param dto 消息DTO
 */
export const sendPrivateMessage = (dto: SendChatMessageDTO) => {
  if (!stompClient || !isConnected) {
    throw new Error('WebSocket未连接')
  }

  console.log('[WebSocket] 准备发送私聊消息:', {
    destination: '/app/chat/private',
    senderId: dto.senderId,
    receiverId: dto.receiverId,
    contentType: dto.contentType,
    content: dto.content,
    'WebSocket连接状态': isConnected,
    'stompClient状态': stompClient ? '已创建' : '未创建'
  })

  try {
    stompClient.publish({
      destination: '/app/chat/private',
      body: JSON.stringify(dto),
    })
    console.log('[WebSocket] 消息已发送到 /app/chat/private')
  } catch (error) {
    console.error('[WebSocket] 发送消息失败:', error)
    throw error
  }
}

/**
 * 注册消息接收回调
 */
export const onMessage = (callback: MessageCallback): (() => void) => {
  messageCallbacks.add(callback)
  return () => {
    messageCallbacks.delete(callback)
  }
}

/**
 * 发送打字状态
 * @param senderId 发送者ID
 * @param receiverId 接收者ID
 */
export const sendTypingStatus = (senderId: number, receiverId: number) => {
  if (!stompClient || !isConnected) {
    return
  }

  stompClient.publish({
    destination: '/app/chat/typing',
    body: JSON.stringify({
      senderId,
      receiverId,
    }),
  })
}

/**
 * 发送在线状态
 * @param userId 用户ID
 */
export const sendOnlineStatus = (userId: number) => {
  if (!stompClient || !isConnected) {
    return
  }

  stompClient.publish({
    destination: '/app/chat/online',
    body: String(userId),
  })
}

/**
 * 发送离线状态
 * @param userId 用户ID
 */
export const sendOfflineStatus = (userId: number) => {
  if (!stompClient || !isConnected) {
    return
  }

  stompClient.publish({
    destination: '/app/chat/offline',
    body: String(userId),
  })
}

/**
 * 注册打字状态回调
 * @param callback 回调函数
 * @returns 取消注册函数
 */
export const onTyping = (callback: TypingCallback): (() => void) => {
  typingCallbacks.add(callback)
  return () => {
    typingCallbacks.delete(callback)
  }
}

/**
 * 注册在线状态回调
 * @param callback 回调函数
 * @returns 取消注册函数
 */
export const onOnlineStatus = (callback: OnlineStatusCallback): (() => void) => {
  onlineStatusCallbacks.add(callback)
  return () => {
    onlineStatusCallbacks.delete(callback)
  }
}

/**
 * 注册错误回调
 * @param callback 回调函数
 * @returns 取消注册函数
 */
export const onError = (callback: ErrorCallback): (() => void) => {
  errorCallbacks.add(callback)
  return () => {
    errorCallbacks.delete(callback)
  }
}

/**
 * 检查WebSocket连接状态
 * @returns 是否已连接
 */
export const isWebSocketConnected = (): boolean => {
  return isConnected && stompClient !== null
}

