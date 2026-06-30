<!--
  @file components/feature/chat/ChatModal.vue
  @description 好友聊天模态框

  功能：
  - 实时消息发送接收
  - 查询结果分享
  - 消息已读状态
  - 模拟自动回复

  @author Frontend Team
-->
<template>
  <!-- 聊天主弹窗 -->
  <Modal
    :is-open="isOpen"
    @close="onClose"
    :title="`与 ${friend.name} 聊天中${unreadCount > 0 ? ` (${unreadCount}条未读)` : ''}`"
    content-class-name="max-w-3xl max-h-[90vh] min-h-[60vh]"
  >
    <div class="h-[80vh] min-h-[500px] flex flex-col">
      <!-- 消息展示区域 -->
      <div
        ref="messagesContainerRef"
        class="flex-1 p-4 bg-gray-100 rounded-t-lg overflow-y-auto space-y-4"
      >
        <!-- 空状态提示 -->
        <div v-if="displayedMessages.length === 0" class="flex items-center justify-center h-full text-gray-400">
          <p>暂无消息，开始聊天吧~</p>
        </div>
        
        <!-- 渲染所有聊天消息 -->
        <div
          v-for="message in displayedMessages"
          :key="`msg-${message.id}`"
          :class="`flex items-start gap-3 ${message.isSent ? 'flex-row-reverse' : ''}`"
        >
          <!-- 头像 -->
          <img
            :src="
              message.isSent
                ? currentUserAvatar
                : friend.avatarUrl || '/default-avatar.png'
            "
            :alt="message.isSent ? '自己' : friend.name || '好友'"
            class="w-8 h-8 rounded-full mt-1 flex-shrink-0"
            @error="(e) => { if (message.isSent) { (e.target as HTMLImageElement).src = '/default-avatar.png' } }"
          />

          <!-- 消息内容+时间戳容器 -->
          <div :class="`flex flex-col ${message.isSent ? 'items-end' : 'items-start'}`" style="max-width: 75%;">
            <!-- 消息气泡 -->
            <div
              :class="`
                shadow-sm text-sm rounded-xl 
                min-w-[60px] max-w-full box-border
                ${message.isSent ? 'bg-primary text-white' : 'bg-white text-gray-800'}
                ${typeof message.content === 'object' ? 'p-0' : 'p-3 whitespace-pre-wrap'}
              `"
              :style="typeof message.content === 'object' ? '' : 'white-space: pre-wrap; line-height: 1.5; word-wrap: break-word; word-break: break-word;'"
            >
              <!-- 文本消息 -->
              <template v-if="typeof message.content === 'string'">
                <p style="margin: 0; word-wrap: break-word; word-break: break-word;">{{ message.content }}</p>
              </template>
              <!-- 查询结果分享 -->
              <template v-else-if="message.contentType === 'query_share' || (typeof message.content === 'object' && message.content && 'userPrompt' in message.content)">
                <div class="p-3">
                  <div class="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg p-4 border border-blue-200">
                    <div class="flex items-center justify-between mb-3">
                      <div class="flex items-center space-x-2">
                        <i class="fa fa-share-alt text-primary text-lg"></i>
                        <span class="font-semibold text-gray-800">分享的查询结果</span>
                      </div>
                      <button
                        @click="openQueryDetailModal(message.content)"
                        class="text-primary hover:text-primary/80 text-sm font-medium flex items-center space-x-1 transition-colors"
                      >
                        <span>查看详情</span>
                        <i class="fa fa-arrow-right text-xs"></i>
                      </button>
                    </div>
                    <div class="space-y-2">
                      <div>
                        <p class="text-xs text-gray-500 mb-1">查询问题</p>
                        <p class="text-sm font-medium text-gray-800 line-clamp-2">
                          {{ (message.content as QueryResultData).userPrompt || '查询结果' }}
                        </p>
                      </div>
                      <div v-if="(message.content as QueryResultData).sqlQuery" class="mt-2">
                        <p class="text-xs text-gray-500 mb-1">SQL查询</p>
                        <code class="text-xs bg-gray-100 px-2 py-1 rounded block line-clamp-2 text-gray-700">
                          {{ (message.content as QueryResultData).sqlQuery }}
                        </code>
                      </div>
                      <div v-if="(message.content as QueryResultData).tableData && (message.content as QueryResultData).tableData.headers.length > 0" class="mt-2">
                        <p class="text-xs text-gray-500 mb-1">数据预览</p>
                        <div class="text-xs text-gray-600">
                          共 {{ (message.content as QueryResultData).tableData.rows.length }} 行数据
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
              <!-- 其他对象类型（容错处理） -->
              <template v-else>
                <div class="p-3 text-gray-500 text-sm">
                  <i class="fa fa-exclamation-triangle mr-2"></i>
                  无法显示此消息内容
                </div>
              </template>
            </div>

            <!-- 时间戳+已读状态 -->
            <div
              :class="`flex items-center mt-1 text-xs ${
                message.isSent ? 'text-primary-200' : 'text-gray-400'
              }`"
            >
              <span>{{ formatTime(message.timestamp) }}</span>
              <!-- 发送方显示已读/未读状态，接收方不显示（因为接收方就是自己） -->
              <span v-if="message.isSent" class="ml-2 text-gray-500">{{ message.isRead ? '已读' : '未读' }}</span>
            </div>
          </div>
        </div>
        
        <!-- 打字状态提示 -->
        <div v-if="isTyping" class="flex items-center gap-2 text-gray-500 text-sm italic">
          <img
            :src="friend.avatarUrl || '/default-avatar.png'"
            :alt="friend.name"
            class="w-6 h-6 rounded-full"
          />
          <span>{{ friend.name }} 正在输入...</span>
        </div>
      </div>

      <!-- 消息输入区域 -->
      <div class="p-4 border-t">
        <div class="flex items-center space-x-2">
          <!-- 分享查询按钮 -->
          <button
            @click="handleOpenShareModal"
            class="p-2 w-10 h-10 flex items-center justify-center text-gray-600 rounded-full text-xl hover:bg-gray-100 transition-colors"
            title="分享查询结果"
          >
            <i class="fa fa-share-alt"></i>
          </button>
          <!-- 消息输入框 -->
          <input
            ref="inputRef"
            type="text"
            placeholder="输入消息..."
            v-model="inputText"
            @keydown.enter.prevent="handleSendMessage"
            class="flex-1 px-4 py-3 border border-gray-300 rounded-full focus:ring-2 focus:ring-primary/30 outline-none"
          />
          <!-- 发送按钮 -->
          <button
            @click="handleSendMessage"
            :disabled="!inputText.trim()"
            class="px-4 py-3 bg-primary text-white rounded-full hover:bg-primary/90 disabled:bg-gray-300 transition-colors"
            aria-label="发送"
          >
            <i class="fa fa-paper-plane text-lg"></i>
          </button>
        </div>
      </div>
    </div>
  </Modal>

  <!-- 查询详情弹窗 -->
  <Modal 
    :is-open="isQueryDetailModalOpen" 
    @close="isQueryDetailModalOpen = false" 
    title="查询详情"
    content-class-name="max-w-4xl max-h-[90vh]"
  >
    <div v-if="selectedQueryDetail" class="overflow-y-auto max-h-[80vh]">
      <QueryResult
        :result="selectedQueryDetail"
        :saved-queries="savedQueries"
        :show-actions="{ save: true, share: false, export: true }"
        @save-query="(query) => {
          console.log('保存查询:', query)
          isQueryDetailModalOpen = false
        }"
      />
    </div>
  </Modal>

  <!-- 分享查询弹窗 -->
  <Modal :is-open="isShareModalOpen" @close="isShareModalOpen = false" title="分享查询结果">
    <div class="space-y-4">
      <p class="text-sm text-gray-600">
        从您的收藏夹中选择一个查询结果分享给 {{ friend.name }}。
      </p>
      <!-- 可分享查询列表 -->
      <div class="max-h-60 overflow-y-auto space-y-2 p-2 bg-gray-50 rounded-lg border">
        <template v-if="savedQueries.length > 0">
          <label
            v-for="query in savedQueries"
            :key="query.id"
            :class="`flex items-center p-3 rounded-lg cursor-pointer transition-colors ${
              selectedQueryId === query.id ? 'bg-primary/20' : 'hover:bg-gray-200'
            }`"
          >
            <input
              type="radio"
              name="query-share-selection"
              :checked="selectedQueryId === query.id"
              @change="selectedQueryId = query.id"
              class="mr-3"
            />
            <span class="text-sm font-medium text-dark truncate" :title="query.userPrompt">
              {{ query.userPrompt }}
            </span>
          </label>
        </template>
        <p v-else class="text-center text-gray-500 text-sm p-4">没有收藏的查询记录</p>
      </div>
    </div>
    <!-- 分享弹窗底部按钮 -->
    <div class="mt-6 flex justify-end space-x-3">
      <button
        @click="isShareModalOpen = false"
        class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200"
      >
        取消
      </button>
      <button
        @click="handleConfirmShare"
        :disabled="!selectedQueryId"
        class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 disabled:bg-primary/50 disabled:cursor-not-allowed"
      >
        确认分享
      </button>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted, computed } from 'vue'
import Modal from '../../ui/Modal.vue'
import QueryResult from '../query/QueryResult.vue'
import type { ChatMessage, Friend, QueryResultData } from '../../../types'
import { friendChatApi, queryLogApi, userApi, queryShareChatApi } from '../../../services/api.real'
import type { ChatMessageVO, SendChatMessageDTO } from '../../../services/api/friend'
import { parseQueryShareMessage as parseQueryShareMessageUtil } from '../../../utils/queryShareMessage'
import {
  connectWebSocket,
  disconnectWebSocket,
  sendPrivateMessage,
  sendTypingStatus,
  onMessage,
  onTyping,
  onError,
  isWebSocketConnected,
} from '../../../services/websocket/chatWebSocket'

// Props
const props = defineProps<{
  isOpen: boolean
  onClose: () => void
  friend: Friend
  savedQueries: QueryResultData[]
  currentUnreadCount: number
  updateUnreadCount: (friendId: string, count: number) => void
  messages: ChatMessage[]
  updateMessages: (newMessages: ChatMessage[]) => void
}>()

// 响应式数据
const inputText = ref('')
const messagesContainerRef = ref<HTMLElement | null>(null)

// 本地消息列表（直接从 props 同步，但可以独立更新）
const localMessages = ref<ChatMessage[]>([])

// 标志位：防止watch循环触发
const isUpdatingFromProps = ref(false)

// 使用本地消息列表，确保响应式更新
const displayedMessages = computed(() => {
  // 直接返回localMessages的副本，Vue会自动追踪localMessages的变化
  return [...(localMessages.value || [])]
})

// 计算未读消息数（对方发送且未读的消息）
const unreadCount = computed(() => {
  return displayedMessages.value.filter(msg => !msg.isSent && !msg.isRead).length
})

// 同步 props.messages 到本地消息列表
// 注意：这个watch主要用于聊天框打开时从friendMessages同步消息
// 实时消息更新应该通过WebSocket回调直接更新localMessages
watch(() => props.messages, (newMessages, oldMessages) => {
  // 如果正在从props更新，跳过（避免循环触发）
  if (isUpdatingFromProps.value) {
    return
  }
  
  console.log('[ChatModal] props.messages 变化，同步到本地:', {
    '旧消息数量': oldMessages?.length || 0,
    '新消息数量': newMessages.length,
    'localMessages数量': localMessages.value.length,
  })
  
  // 使用更智能的比较方式：比较消息ID列表和数量
  const newMessageIds = new Set(newMessages.map(m => String(m.id)))
  const localMessageIds = new Set(localMessages.value.map(m => String(m.id)))
  
  // 检查是否有新消息或消息被删除
  const hasNewMessages = newMessages.some(msg => !localMessageIds.has(String(msg.id)))
  const hasRemovedMessages = localMessages.value.some(msg => !newMessageIds.has(String(msg.id)))
  const countChanged = newMessages.length !== localMessages.value.length
  
  if (hasNewMessages || hasRemovedMessages || countChanged) {
    console.log('[ChatModal] 消息有变化，更新本地消息列表', {
      hasNewMessages,
      hasRemovedMessages,
      countChanged,
      '新消息IDs': Array.from(newMessageIds).sort().join(','),
      '本地消息IDs': Array.from(localMessageIds).sort().join(','),
    })
    
    // 合并消息：保留localMessages中可能已经通过WebSocket添加的实时消息
    // 使用Map来去重，保留最新的消息（优先使用props中的消息，因为它可能包含更新的状态）
    const messageMap = new Map<string, ChatMessage>()
    
    // 先添加localMessages中的消息（保留WebSocket实时添加的消息）
    localMessages.value.forEach(msg => {
      messageMap.set(String(msg.id), msg)
    })
    
    // 再添加props.messages中的消息（覆盖相同ID的消息，确保状态最新）
    newMessages.forEach(msg => {
      messageMap.set(String(msg.id), msg)
    })
    
    // 转换为数组并排序
    const mergedMessages = Array.from(messageMap.values()).sort(
      (a, b) => a.timestamp.getTime() - b.timestamp.getTime()
    )
    
    console.log('[ChatModal] 合并后的消息数量:', mergedMessages.length)
    
    // 只有当合并后的消息确实不同时才更新
    const mergedIds = mergedMessages.map(m => String(m.id)).sort().join(',')
    const currentIds = localMessages.value.map(m => String(m.id)).sort().join(',')
    
    if (mergedIds !== currentIds || mergedMessages.length !== localMessages.value.length) {
      isUpdatingFromProps.value = true
      localMessages.value = mergedMessages
      console.log('[ChatModal] 消息同步完成')
      
      // 使用nextTick确保DOM更新后再重置标志位
      nextTick(() => {
        isUpdatingFromProps.value = false
        scrollToBottom()
      })
    } else {
      console.log('[ChatModal] 合并后消息相同，跳过更新')
    }
  } else {
    console.log('[ChatModal] 消息内容相同，跳过更新')
  }
}, { deep: false, immediate: true })

const inputRef = ref<HTMLInputElement | null>(null)
const isShareModalOpen = ref(false)
const selectedQueryId = ref<string | null>(null)
const isQueryDetailModalOpen = ref(false)
const selectedQueryDetail = ref<QueryResultData | null>(null)
const isLoading = ref(false)
const isTyping = ref(false)
const typingTimeout = ref<number | null>(null)

// 当前用户信息（用于显示头像）
const currentUserAvatar = ref<string>('/default-avatar.png')

// WebSocket相关
let unsubscribeMessage: (() => void) | null = null
let unsubscribeTyping: (() => void) | null = null
let unsubscribeError: (() => void) | null = null

// 初始化WebSocket连接
const initWebSocket = async () => {
  const userId = Number(sessionStorage.getItem('userId') || '1')
  
  // 如果WebSocket未连接，先连接
  if (!isWebSocketConnected()) {
    try {
      console.log('[ChatModal] WebSocket未连接，正在连接...')
      await connectWebSocket(userId)
      console.log('[ChatModal] WebSocket连接成功')
    } catch (error) {
      console.error('WebSocket连接失败:', error)
      return
    }
  } else {
    console.log('[ChatModal] WebSocket已连接，直接注册回调')
  }
  
  // 无论是否新连接，都要注册回调（因为可能之前没有注册）
  // 先取消之前的订阅（如果存在）
  if (unsubscribeMessage) {
    unsubscribeMessage()
  }
  if (unsubscribeTyping) {
    unsubscribeTyping()
  }
  if (unsubscribeError) {
    unsubscribeError()
  }
  
  // 注册消息接收回调
  const currentUserId = Number(sessionStorage.getItem('userId') || '1')
  // 使用friendId（用户ID）而不是id（关系ID）
  const currentFriendId = Number((props.friend as any).friendId || props.friend.id)
  console.log('[ChatModal] 注册消息接收回调，当前好友:', {
    friendId: currentFriendId,
    friendName: props.friend.name,
    '当前用户ID': currentUserId,
    '聊天框是否打开': props.isOpen,
  })
  unsubscribeMessage = onMessage((message: ChatMessageVO) => {
    const msgSenderId = Number(message.senderId)
    const msgReceiverId = Number(message.receiverId)
    const isFromCurrentFriend = msgSenderId === currentFriendId && msgReceiverId === currentUserId
    const isFromCurrentUser = msgSenderId === currentUserId && msgReceiverId === currentFriendId
    
    console.log('[ChatModal] 收到WebSocket消息（ChatModal回调）:', {
      messageId: message.id,
      senderId: msgSenderId,
      receiverId: msgReceiverId,
      content: message.content,
      '当前用户ID': currentUserId,
      '当前好友ID': currentFriendId,
      '是否来自当前好友': isFromCurrentFriend,
      '是否来自当前用户': isFromCurrentUser,
      '应该显示': isFromCurrentFriend || isFromCurrentUser,
      '聊天框是否打开': props.isOpen,
    })
    
    // 处理消息（无论聊天框是否打开，都处理，但只有打开时才显示）
    handleReceivedMessage(message)
  })
  
  // 注册打字状态回调
  unsubscribeTyping = onTyping((senderId: number) => {
    // 使用friendId（用户ID）而不是id（关系ID）
    const friendId = Number((props.friend as any).friendId || props.friend.id)
    if (senderId === friendId) {
      isTyping.value = true
      if (typingTimeout.value) {
        clearTimeout(typingTimeout.value)
      }
      typingTimeout.value = window.setTimeout(() => {
        isTyping.value = false
      }, 3000)
    }
  })
  
  // 注册错误回调
  unsubscribeError = onError((error: string) => {
    console.error('WebSocket错误:', error)
    alert('消息发送失败: ' + error)
  })
}

// 使用统一的查询分享消息处理函数
const parseQueryShareMessage = (messageContent: any, messageId: string) => {
  return parseQueryShareMessageUtil(messageContent, messageId, props.savedQueries)
}

// 处理接收到的消息
const handleReceivedMessage = async (message: ChatMessageVO) => {
  const userId = Number(sessionStorage.getItem('userId') || '1')
  // 使用friendId（用户ID）而不是id（关系ID）
  const friendId = Number((props.friend as any).friendId || props.friend.id)
  
  // 确保类型一致（后端可能返回Long类型）
  const messageSenderId = Number(message.senderId)
  const messageReceiverId = Number(message.receiverId)
  
  console.log('[ChatModal] handleReceivedMessage - 收到WebSocket消息:', {
    messageId: message.id,
    message,
    userId,
    friendId,
    messageSenderId,
    messageReceiverId,
    'senderId === userId': messageSenderId === userId,
    'senderId === friendId': messageSenderId === friendId,
    'receiverId === userId': messageReceiverId === userId,
    'receiverId === friendId': messageReceiverId === friendId,
  })
  
  // 只处理当前好友的消息
  if (
    (messageSenderId === friendId && messageReceiverId === userId) ||
    (messageSenderId === userId && messageReceiverId === friendId)
  ) {
    console.log('[ChatModal] 消息匹配当前好友，处理消息')
    
    // 判断消息是否是自己发送的
    const isSent = messageSenderId === userId
    
    // 处理消息内容：支持文本和查询分享
    let messageContent: string | QueryResultData
    let contentType: 'text' | 'query_share' = 'text'
    
    // 检查是否是查询分享类型
    if (message.contentType === 'query_share' || 
        (typeof message.content === 'object' && message.content && ('queryId' in message.content || 'tableData' in message.content || 'queryLogId' in message.content))) {
      // 使用统一函数处理查询分享消息
      const result = await parseQueryShareMessage(message.content, message.id)
      messageContent = result.content
      contentType = result.contentType
    } else {
      // 普通文本消息
      if (message.content && typeof message.content === 'object' && 'text' in message.content) {
        messageContent = (message.content as any).text || ''
      } else {
        messageContent = String(message.content || '')
      }
    }
    
    const newMessage: ChatMessage = {
      id: message.id,
      content: messageContent,
      isSent: isSent,
      timestamp: message.sendTime ? new Date(message.sendTime) : new Date(),
      isRead: message.isRead || false,
      contentType: contentType,
    }
    
    // 检查消息是否已存在（避免重复）- 使用localMessages而不是props.messages
    const existingIndex = localMessages.value.findIndex((msg) => String(msg.id) === String(newMessage.id))
    const exists = existingIndex >= 0
    
    // 检查是否有临时消息需要替换（临时消息ID以temp-开头，且内容相同）
    const tempMessageIndex = localMessages.value.findIndex((msg) => {
      if (!String(msg.id).startsWith('temp-') || msg.isSent !== newMessage.isSent) {
        return false
      }
      
      // 对于查询分享类型，比较查询ID
      if (msg.contentType === 'query_share' && newMessage.contentType === 'query_share') {
        const msgContent = msg.content as QueryResultData
        const newContent = newMessage.content as QueryResultData
        return msgContent?.id === newContent?.id || 
               msgContent?.userPrompt === newContent?.userPrompt
      }
      
      // 对于文本消息，直接比较内容
      return msg.content === newMessage.content
    })
    
    console.log('[ChatModal] 检查消息是否已存在:', {
      messageId: newMessage.id,
      exists,
      existingIndex,
      tempMessageIndex,
      'localMessages数量': localMessages.value.length,
      'props.messages数量': props.messages.length,
      newMessage,
    })
    
    if (!exists) {
      console.log('[ChatModal] 添加新消息到列表:', newMessage)
      
      let updatedLocalMessages: ChatMessage[]
      
      // 如果有临时消息，替换它；否则添加新消息
      if (tempMessageIndex >= 0) {
        console.log('[ChatModal] 找到临时消息，替换为真实消息:', {
          tempId: localMessages.value[tempMessageIndex].id,
          realId: newMessage.id,
        })
        updatedLocalMessages = [...localMessages.value]
        updatedLocalMessages[tempMessageIndex] = newMessage
      } else {
        // 添加新消息
        updatedLocalMessages = [...localMessages.value, newMessage]
      }
      
      // 按时间排序
      updatedLocalMessages.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime())
      
      console.log('[ChatModal] 更新后的消息数量:', updatedLocalMessages.length)
      
      // 更新localMessages（创建新数组引用）
      localMessages.value = updatedLocalMessages
      
      // 同时更新父组件的消息列表（通过 props.updateMessages）- 确保friendMessages同步
      props.updateMessages(updatedLocalMessages)
      
      // 如果是对方发送的消息，更新未读数
      if (!newMessage.isSent) {
        props.updateUnreadCount(props.friend.id, unreadCount.value)
      }
      
      // 强制触发视图更新和滚动
      nextTick(() => {
        scrollToBottom()
      })
      
      // 如果是接收到的消息且聊天框打开，标记为已读
      if (!newMessage.isSent && !newMessage.isRead && props.isOpen) {
        markMessageAsRead(newMessage.id)
        // 更新消息状态
        const updatedIndex = localMessages.value.findIndex(msg => msg.id === newMessage.id)
        if (updatedIndex >= 0) {
          const updated = [...localMessages.value]
          updated[updatedIndex] = { ...updated[updatedIndex], isRead: true }
          localMessages.value = updated
          props.updateMessages(updated)
        }
      }
    } else {
      console.log('[ChatModal] 消息已存在，检查是否需要更新状态:', newMessage.id)
      
      // 即使消息已存在，也要检查是否需要更新状态（比如已读状态）
      if (existingIndex >= 0) {
        const existingMsg = localMessages.value[existingIndex]
        // 如果消息状态有变化（比如已读状态），更新它
        if (existingMsg.isRead !== newMessage.isRead || existingMsg.content !== newMessage.content) {
          console.log('[ChatModal] 消息状态有变化，更新消息:', {
            id: newMessage.id,
            '旧isRead': existingMsg.isRead,
            '新isRead': newMessage.isRead,
            '旧content': existingMsg.content,
            '新content': newMessage.content,
          })
          const updatedMessages = [...localMessages.value]
          updatedMessages[existingIndex] = { ...existingMsg, ...newMessage }
          localMessages.value = updatedMessages
          props.updateMessages(updatedMessages)
        }
      }
    }
  } else {
    console.log('[ChatModal] 消息不匹配当前好友，忽略')
  }
}

// 标记消息为已读
const markMessageAsRead = async (messageId: string) => {
  try {
    await friendChatApi.markMessageAsRead(messageId)
  } catch (error) {
    console.error('标记消息已读失败:', error)
  }
}

// 方法：格式化时间
const formatTime = (date: Date) => {
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 方法：滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainerRef.value) {
      const container = messagesContainerRef.value
      container.scrollTop = container.scrollHeight

      // 双重保险
      if (Math.abs(container.scrollHeight - (container.scrollTop + container.clientHeight)) > 10) {
        container.scrollTop = container.scrollHeight
      }
    }
  })
}

// 监听弹窗打开
watch(
  () => props.isOpen,
  async (isOpen) => {
    if (isOpen) {
      console.log('[ChatModal] 聊天框打开:', {
        friendId: props.friend.id,
        friendName: props.friend.name,
        'props.messages数量': props.messages.length,
        'localMessages数量': localMessages.value.length,
      })
      
      // 初始化WebSocket连接（如果未连接则连接，已连接则只注册回调）
      await initWebSocket()
      
      // 立即同步消息（不等待setTimeout）
      console.log('[ChatModal] 同步消息到本地，数量:', props.messages.length)
      localMessages.value = [...props.messages]
      
      // 打开弹窗时
      setTimeout(async () => {
        // 1. 立即同步消息（确保显示最新消息，包括WebSocket实时收到的消息）
        console.log('[ChatModal] 打开聊天框，同步消息:', {
          'props.messages数量': props.messages.length,
          'localMessages数量': localMessages.value.length,
        })
        
        // 合并props.messages和localMessages，确保不丢失任何消息
        const messageMap = new Map<string, ChatMessage>()
        
        // 先添加props.messages中的消息（来自friendMessages）
        props.messages.forEach(msg => {
          messageMap.set(String(msg.id), msg)
        })
        
        // 再添加localMessages中的消息（可能包含之前通过WebSocket添加的消息）
        localMessages.value.forEach(msg => {
          messageMap.set(String(msg.id), msg)
        })
        
        // 转换为数组并排序
        const mergedMessages = Array.from(messageMap.values()).sort(
          (a, b) => a.timestamp.getTime() - b.timestamp.getTime()
        )
        
        console.log('[ChatModal] 合并后的消息数量:', mergedMessages.length)
        
        // 验证消息数据格式
        if (mergedMessages.length > 0) {
          console.log('[ChatModal] 消息数据验证:', {
            '第一条消息': {
              id: mergedMessages[0].id,
              content: mergedMessages[0].content,
              isSent: mergedMessages[0].isSent,
              timestamp: mergedMessages[0].timestamp,
              'timestamp类型': typeof mergedMessages[0].timestamp,
              'timestamp是否为Date': mergedMessages[0].timestamp instanceof Date,
              isRead: mergedMessages[0].isRead,
            }
          })
          
          // 确保 timestamp 是 Date 对象
          mergedMessages.forEach(msg => {
            if (!(msg.timestamp instanceof Date)) {
              console.warn('[ChatModal] 消息timestamp不是Date对象，转换:', msg.id, msg.timestamp)
              msg.timestamp = new Date(msg.timestamp)
            }
          })
        }
        
        localMessages.value = mergedMessages
        
        // 强制触发视图更新
        await nextTick()
        console.log('[ChatModal] 视图更新后，displayedMessages数量:', displayedMessages.value.length)
        
        // 2. 加载聊天历史记录
        // 即使 props.messages 有消息，也要加载历史记录，因为 props.messages 可能不完整
        // 检查是否已经有对方发送的消息，如果没有，说明需要加载历史记录
        const hasFriendMessages = mergedMessages.some(msg => !msg.isSent)
        console.log('[ChatModal] 检查是否需要加载历史记录:', {
          '当前消息数': mergedMessages.length,
          '是否有对方消息': hasFriendMessages,
          '自己发送': mergedMessages.filter(m => m.isSent).length,
          '对方发送': mergedMessages.filter(m => !m.isSent).length,
        })
        
        if (mergedMessages.length === 0 || !hasFriendMessages) {
          console.log('[ChatModal] 消息为空或缺少对方消息，加载历史记录')
          await loadChatHistory()
        } else {
          console.log('[ChatModal] 消息已存在且包含对方消息，已同步到本地，消息数:', mergedMessages.length)
          
          // 确保父组件也同步了最新消息
          props.updateMessages(mergedMessages)
        }
        
        // 3. 批量标记消息为已读（打开聊天框时自动标记为已读）
        await markAllMessagesAsRead()
        
        // 4. 清空当前好友的未读消息数（标记为已读后）
        props.updateUnreadCount(props.friend.id, 0)

        // 5. 滚动到底部
        nextTick(() => {
          scrollToBottom()
        })

        // 6. 聚焦输入框
        setTimeout(() => {
          inputRef.value?.focus()
        }, 100)
        
        console.log('[ChatModal] 聊天框初始化完成，消息数:', localMessages.value.length)
      }, 100) // 减少延迟时间

      // 每次打开弹窗时重置输入框
      inputText.value = ''
    } else {
      console.log('[ChatModal] 聊天框关闭')
      // 关闭弹窗时清理打字状态
      if (typingTimeout.value) {
        clearTimeout(typingTimeout.value)
        typingTimeout.value = null
      }
      isTyping.value = false
    }
  },
  { immediate: true },
)

// 加载聊天历史记录
const loadChatHistory = async () => {
  try {
    isLoading.value = true
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 使用friendId（用户ID）而不是id（关系ID）
    const friendId = Number((props.friend as any).friendId || props.friend.id)
    
    console.log('[ChatModal] 开始加载聊天历史记录:', {
      userId,
      friendId,
      'userId类型': typeof userId,
      'friendId类型': typeof friendId,
      'sessionStorage userId': sessionStorage.getItem('userId'),
      'props.friend.id': props.friend.id,
      'props.friend': props.friend,
    })
    
    const pageData = await friendChatApi.getChatHistoryPage({
      userId,
      friendId,
      page: 1,
      pageSize: 50,
    })
    
    console.log('[ChatModal] 加载历史记录 - API返回数据:', {
      '消息数量': pageData.content.length,
      '前3条消息的senderId': pageData.content.slice(0, 3).map(m => ({
        id: m.id,
        senderId: m.senderId,
        receiverId: m.receiverId,
        senderId类型: typeof m.senderId,
      })),
      userId,
      friendId,
    })
    
    // 转换为ChatMessage格式
    const messages: ChatMessage[] = await Promise.all(pageData.content.map(async (msg, index) => {
      // 处理消息内容：支持文本和查询分享
      let messageContent: string | QueryResultData
      let contentType: 'text' | 'query_share' = 'text'
      
      // 检查是否是查询分享类型
      if (msg.contentType === 'query_share' || 
          (typeof msg.content === 'object' && msg.content && ('queryId' in msg.content || 'tableData' in msg.content || 'queryLogId' in msg.content))) {
        // 使用统一函数处理查询分享消息
        const result = await parseQueryShareMessage(msg.content, msg.id)
        messageContent = result.content
        contentType = result.contentType
      } else {
        // 普通文本消息
        if (msg.content && typeof msg.content === 'object' && 'text' in msg.content) {
          messageContent = (msg.content as any).text || ''
        } else {
          messageContent = String(msg.content || '')
        }
      }
      
      // 确保类型一致
      const msgSenderId = Number(msg.senderId)
      const isSent = msgSenderId === userId
      
      // 调试日志：检查isSent判断（只记录前3条）
      if (index < 3) {
        console.log('[ChatModal] 加载历史记录 - 消息isSent判断:', {
          messageId: msg.id,
          'msg.senderId原始值': msg.senderId,
          'msg.senderId类型': typeof msg.senderId,
          msgSenderId,
          userId,
          'userId类型': typeof userId,
          isSent,
          'msg.receiverId': msg.receiverId,
        })
      }
      
      return {
        id: msg.id,
        content: messageContent,
        isSent: isSent,
        timestamp: msg.sendTime ? new Date(msg.sendTime) : new Date(),
        isRead: msg.isRead || false,
        contentType: contentType,
      }
    }))
    
    // 按时间排序
    messages.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime())
    
    console.log('[ChatModal] 加载历史记录完成:', {
      '消息总数': messages.length,
      '自己发送': messages.filter(m => m.isSent).length,
      '对方发送': messages.filter(m => !m.isSent).length,
    })
    
    // 合并历史记录和现有消息（避免重复）
    const messageMap = new Map<string, ChatMessage>()
    
    // 先添加现有消息
    localMessages.value.forEach(msg => {
      messageMap.set(String(msg.id), msg)
    })
    
    // 再添加历史记录中的消息（覆盖相同ID的消息）
    messages.forEach(msg => {
      messageMap.set(String(msg.id), msg)
    })
    
    // 转换为数组并排序
    const allMessages = Array.from(messageMap.values()).sort(
      (a, b) => a.timestamp.getTime() - b.timestamp.getTime()
    )
    
    console.log('[ChatModal] 合并后的所有消息:', {
      '消息总数': allMessages.length,
      '自己发送': allMessages.filter(m => m.isSent).length,
      '对方发送': allMessages.filter(m => !m.isSent).length,
    })
    
    // 更新本地消息列表
    localMessages.value = allMessages
    
    // 同时更新父组件
    props.updateMessages(allMessages)
    
    nextTick(() => {
      scrollToBottom()
    })
  } catch (error) {
    console.error('加载聊天历史失败:', error)
  } finally {
    isLoading.value = false
  }
}

// 批量标记消息为已读
const markAllMessagesAsRead = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 使用friendId（用户ID）而不是id（关系ID）
    const friendId = Number((props.friend as any).friendId || props.friend.id)
    
    await friendChatApi.markMessagesAsRead(userId, friendId)
    
    // 更新本地消息状态
    const updatedMessages = localMessages.value.map((msg) =>
      !msg.isSent && !msg.isRead ? { ...msg, isRead: true } : msg,
    )
    
    // 更新本地消息列表
    localMessages.value = updatedMessages
    
    // 同时更新父组件
    props.updateMessages(updatedMessages)
  } catch (error) {
    console.error('标记消息已读失败:', error)
  }
}

// 方法：发送消息
const handleSendMessage = async () => {
  const content = inputText.value.trim()
  if (!content) return

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 使用friendId（用户ID）而不是id（关系ID）
    const friendId = Number((props.friend as any).friendId || props.friend.id)

      // 优先使用WebSocket发送（实时）
      if (isWebSocketConnected()) {
        const dto: SendChatMessageDTO = {
          senderId: userId,
          receiverId: friendId,
          contentType: 'text',
          content: { text: content },
        }
        
        // 先添加一个临时消息（立即显示，优化用户体验）
        const tempId = `temp-${Date.now()}`
        const tempMessage: ChatMessage = {
          id: tempId,
          content: content,
          isSent: true,
          timestamp: new Date(),
          isRead: false,
        }
        
        // 直接更新本地消息列表（立即显示）
        const updatedMessages = [...localMessages.value, tempMessage].sort(
          (a, b) => a.timestamp.getTime() - b.timestamp.getTime()
        )
        localMessages.value = updatedMessages
        
        // 同时更新父组件
        props.updateMessages(updatedMessages)
        
        // 滚动到底部
        nextTick(() => {
          scrollToBottom()
        })
        
        // 通过WebSocket发送消息（后端会保存并转发，然后通过onMessage回调接收真实消息）
        try {
          sendPrivateMessage(dto)
          console.log('[ChatModal] 消息已通过WebSocket发送:', dto)
        } catch (error) {
          console.error('[ChatModal] WebSocket发送失败:', error)
          // 如果WebSocket发送失败，移除临时消息并使用REST API
          const filteredMessages = localMessages.value.filter(msg => msg.id !== tempId)
          localMessages.value = filteredMessages
          props.updateMessages(filteredMessages)
          throw error // 继续执行下面的REST API逻辑
        }
      } else {
      // 如果WebSocket未连接，使用REST API
      const savedChat = await friendChatApi.sendMessage({
        senderId: userId,
        receiverId: friendId,
        contentType: 'text',
        content: { text: content },
      })

      // 提取消息内容
      let messageContent = ''
      if (savedChat.content && typeof savedChat.content === 'object' && 'text' in savedChat.content) {
        messageContent = (savedChat.content as any).text || ''
      } else {
        messageContent = String(savedChat.content || '')
      }
      
      // 确保类型一致，判断是否是自己发送的
      const savedChatSenderId = Number(savedChat.senderId)
      const isSent = savedChatSenderId === userId
      
      const newMessage: ChatMessage = {
        id: savedChat.id,
        content: messageContent,
        isSent: isSent,
        timestamp: savedChat.sendTime ? new Date(savedChat.sendTime) : new Date(),
        isRead: savedChat.isRead || false,
      }

      // 直接更新本地消息列表（立即显示）
      const updatedMessages = [...localMessages.value, newMessage]
      localMessages.value = updatedMessages
      
      // 同时更新父组件
      props.updateMessages(updatedMessages)
    }

    // 清空输入框
    inputText.value = ''

    // 滚动到底部
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    alert('发送消息失败，请稍后重试')
  }
}

// 处理输入变化（发送打字状态）
watch(inputText, () => {
  if (inputText.value.trim() && isWebSocketConnected()) {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 使用friendId（用户ID）而不是id（关系ID）
    const friendId = Number((props.friend as any).friendId || props.friend.id)
    sendTypingStatus(userId, friendId)
  }
})

// 方法：打开分享弹窗
const handleOpenShareModal = () => {
  selectedQueryId.value = null
  isShareModalOpen.value = true
}

// 方法：打开查询详情弹窗
const openQueryDetailModal = (queryData: QueryResultData) => {
  selectedQueryDetail.value = queryData
  isQueryDetailModalOpen.value = true
}

// 方法：确认分享（使用queryShareChatApi）
const handleConfirmShare = async () => {
  if (!selectedQueryId.value) return

  const selectedQuery = props.savedQueries.find((q) => q.id === selectedQueryId.value)
  if (!selectedQuery) return

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 使用friendId（用户ID）而不是id（关系ID）
    const friendId = Number((props.friend as any).friendId || props.friend.id)
    
    // 解析执行时间（支持"123ms"或数字）
    let executionTime: number = 0
    if (typeof selectedQuery.executionTime === 'number') {
      executionTime = selectedQuery.executionTime
    } else if (typeof selectedQuery.executionTime === 'string') {
      const timeStr = selectedQuery.executionTime.trim()
      if (timeStr.endsWith('ms')) {
        executionTime = parseInt(timeStr) || 0
      } else if (timeStr.endsWith('s')) {
        executionTime = Math.round(parseFloat(timeStr) * 1000)
      } else {
        executionTime = parseInt(timeStr) || 0
      }
    }
    
    // 使用queryShareChatApi发送查询分享消息
    const shareData = {
      shareUserId: userId,
      receiveUserId: friendId,
      queryTitle: selectedQuery.userPrompt || '查询结果',
      sqlQuery: selectedQuery.sqlQuery || '',
      databaseName: selectedQuery.database || '',
      dbConnectionId: selectedQuery.dbConnectionId || null,
      llmName: selectedQuery.model || '',
      llmConfigId: selectedQuery.llmConfigId || null,
      executionTime: executionTime,
      queryTime: selectedQuery.queryTime || new Date().toISOString(),
      dialogId: selectedQuery.conversationId || '',
      tableData: selectedQuery.tableData ? {
        headers: selectedQuery.tableData.headers,
        rows: selectedQuery.tableData.rows,
      } : undefined,
      chartData: selectedQuery.chartData ? {
        type: selectedQuery.chartData.type,
        labels: selectedQuery.chartData.labels || [],
        datasets: selectedQuery.chartData.datasets || [],
      } : undefined,
    }
    
    // 先添加一个临时消息（立即显示，优化用户体验）
    const tempId = `temp-share-${Date.now()}`
    const tempMessage: ChatMessage = {
      id: tempId,
      content: selectedQuery, // 直接使用查询数据作为内容
      isSent: true,
      timestamp: new Date(),
      isRead: false,
      contentType: 'query_share',
    }
    
    // 直接更新本地消息列表（立即显示）
    const updatedMessages = [...localMessages.value, tempMessage].sort(
      (a, b) => a.timestamp.getTime() - b.timestamp.getTime()
    )
    localMessages.value = updatedMessages
    
    // 同时更新父组件
    props.updateMessages(updatedMessages)
    
    // 滚动到底部
    nextTick(() => {
      scrollToBottom()
    })
    
    // 关闭分享弹窗
    inputText.value = ''
    isShareModalOpen.value = false
    
    // 发送分享消息（后端会通过WebSocket转发，前端通过onMessage接收）
    // WebSocket消息到达后会替换临时消息
    try {
      await queryShareChatApi.share(shareData)
      console.log('[ChatModal] 分享消息已发送，等待WebSocket消息到达')
    } catch (shareError) {
      console.error('[ChatModal] 分享消息发送失败:', shareError)
      // 如果发送失败，移除临时消息
      const filteredMessages = localMessages.value.filter(msg => msg.id !== tempId)
      localMessages.value = filteredMessages
      props.updateMessages(filteredMessages)
      throw shareError // 重新抛出错误，让catch块处理
    }
  } catch (error) {
    console.error('分享失败:', error)
    alert('分享失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 加载当前用户信息（用于显示头像）
const loadCurrentUserInfo = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const userData = await userApi.getById(userId)
    currentUserAvatar.value = userData.avatarUrl || '/default-avatar.png'
  } catch (error) {
    console.error('加载当前用户信息失败:', error)
    currentUserAvatar.value = '/default-avatar.png'
  }
}

// 生命周期
onMounted(() => {
  // 加载当前用户信息
  loadCurrentUserInfo()
  
  if (props.isOpen) {
    initWebSocket()
  }
})

onUnmounted(() => {
  // 清理WebSocket订阅
  if (unsubscribeMessage) {
    unsubscribeMessage()
    unsubscribeMessage = null
  }
  if (unsubscribeTyping) {
    unsubscribeTyping()
    unsubscribeTyping = null
  }
  if (unsubscribeError) {
    unsubscribeError()
    unsubscribeError = null
  }
  if (typingTimeout.value) {
    clearTimeout(typingTimeout.value)
    typingTimeout.value = null
  }
})
</script>
