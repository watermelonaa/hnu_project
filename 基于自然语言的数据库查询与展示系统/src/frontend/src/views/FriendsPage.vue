<!--
  @file views/FriendsPage.vue
  @description 好友管理页面

  功能：
  - 好友列表展示
  - 好友请求处理
  - 查询分享管理
  - 聊天功能
  - 添加/搜索好友

  @author Frontend Team
-->
<template>
  <section class="p-6 flex flex-col space-y-4 overflow-y-auto bg-neutral h-full">
    <!-- 标签栏（整合添加好友按钮） -->
    <div class="bg-white rounded-xl shadow-sm sticky top-0 z-10 border border-gray-200">
      <div class="flex justify-between items-center px-6 py-3">
        <div class="flex space-x-1">
          <button @click="activeTab = 'friends'" :class="tabClass('friends')">
            好友列表
            <span v-if="friends.length > 0" class="ml-1 text-xs"> ({{ friends.length }}) </span>
          </button>
          <button @click="activeTab = 'requests'" :class="tabClass('requests')">
            好友请求
            <span v-if="requests.length > 0" class="ml-1 text-xs"> ({{ requests.length }}) </span>
          </button>
          <button @click="activeTab = 'shares'" :class="tabClass('shares')">
            好友分享
            <span v-if="unreadSharesCount > 0" class="ml-1 text-xs"> ({{ unreadSharesCount }}) </span>
          </button>
          <button @click="activeTab = 'shared'" :class="tabClass('shared')">
            我的分享
            <span v-if="sharedQueries.length > 0" class="ml-1 text-xs"> ({{ sharedQueries.length }}) </span>
          </button>
        </div>
        <button
          @click="isAddFriendModalOpen = true"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:shadow-md transition-all duration-200"
        >
          <i class="fa fa-plus mr-2"></i> 添加好友
        </button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="flex-grow pt-0">
      <!-- 1. 好友列表标签 -->
      <div v-if="activeTab === 'friends'">
        <!-- 搜索框 -->
        <div class="mb-3">
          <div class="relative w-full">
            <i class="fa fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></i>
            <input
              type="text"
              placeholder="搜索好友（名称/备注）..."
              v-model="searchTerm"
              class="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30"
            />
          </div>
        </div>

        <!-- 好友列表 -->
        <div v-if="filteredFriends.length > 0" class="space-y-3">
          <div
            v-for="friend in filteredFriends"
            :key="friend.id"
            class="p-4 bg-white rounded-xl shadow-sm border flex items-center justify-between transition-shadow hover:shadow-md w-full"
          >
            <div class="flex items-center space-x-4">
              <div class="relative cursor-pointer" @click="handleOpenProfileModal(friend)">
                <img
                  :src="friend.avatarUrl"
                  :alt="friend.name"
                  class="w-12 h-12 rounded-full hover:ring-2 hover:ring-primary transition-all"
                  @error="handleImageError"
                />
                <span
                  v-if="friend.isOnline"
                  class="absolute bottom-0 right-0 block h-3 w-3 rounded-full bg-green-400 ring-2 ring-white"
                ></span>
              </div>
              <div>
                <div class="flex items-center">
                  <p class="font-semibold text-dark">
                    {{ friend.remark || friend.name }}
                  </p>
                  <span
                    v-if="unreadMessages[friend.id] > 0"
                    class="ml-2 bg-primary text-white text-xs rounded-full px-2 py-0.5"
                  >
                    {{ unreadMessages[friend.id] }}
                  </span>
                </div>
                <p v-if="friend.remark" class="text-sm text-gray-500 italic">
                  「{{ friend.name }}」
                </p>
              </div>
            </div>
            <div class="flex space-x-3">
              <button
                @click="handleOpenChat(friend)"
                class="p-3 w-12 h-12 flex items-center justify-center bg-gray-100 text-gray-600 rounded-xl text-lg hover:bg-primary/10 hover:text-primary transition-all shadow-sm hover:shadow-md"
                title="聊天"
              >
                <i class="fa fa-comment-o"></i>
              </button>
              <button
                @click="handleOpenRemarkModal(friend)"
                class="p-3 w-12 h-12 flex items-center justify-center bg-gray-100 text-gray-600 rounded-xl text-lg hover:bg-primary/10 hover:text-primary transition-all shadow-sm hover:shadow-md"
                title="备注好友"
              >
                <i class="fa fa-pencil"></i>
              </button>
              <button
                @click="handleOpenProfileModal(friend)"
                class="p-3 w-12 h-12 flex items-center justify-center bg-gray-100 text-gray-600 rounded-xl text-lg hover:bg-primary/10 hover:text-primary transition-all shadow-sm hover:shadow-md"
                title="查看好友主页"
              >
                <i class="fa fa-user-o"></i>
              </button>
              <button
                @click="handleDeleteRequest(friend)"
                class="p-3 w-12 h-12 flex items-center justify-center bg-gray-100 text-gray-600 rounded-xl text-lg hover:bg-danger/10 hover:text-danger transition-all shadow-sm hover:shadow-md"
                title="删除好友"
              >
                <i class="fa fa-trash-o"></i>
              </button>
              <button
                @click="handleOpenShareToFriendModal(friend)"
                class="p-3 w-12 h-12 flex items-center justify-center bg-gray-100 text-gray-600 rounded-xl text-lg hover:bg-primary/10 hover:text-primary transition-all shadow-sm hover:shadow-md"
                title="分享查询"
              >
                <i class="fa fa-share-alt"></i>
              </button>
            </div>
          </div>
        </div>
        <div v-else class="text-center text-gray-500 py-16 bg-white rounded-xl shadow-sm">
          <i class="fa fa-user-times text-4xl mb-3 text-gray-400"></i>
          <p>未找到匹配的好友</p>
        </div>
      </div>

      <!-- 2. 好友请求标签 -->
      <div v-if="activeTab === 'requests'" class="space-y-3">
        <div v-if="requests.length > 0">
          <div
            v-for="req in requests"
            :key="req.id"
            class="p-4 bg-white rounded-xl shadow-sm border flex items-center justify-between"
          >
            <div class="flex items-center space-x-4">
              <img
                :src="req.fromUser.avatarUrl"
                :alt="req.fromUser.name"
                class="w-12 h-12 rounded-full"
                @error="handleImageError"
              />
              <div>
                <p class="font-semibold text-dark">{{ req.fromUser.name }}</p>
                <p class="text-sm text-gray-500">{{ req.timestamp }}</p>
              </div>
            </div>
            <div class="flex space-x-2">
              <button
                @click="handleAcceptRequest(req)"
                class="px-3 py-1 bg-primary text-white rounded-lg text-sm"
              >
                同意
              </button>
              <button
                @click="handleRejectRequest(req.id)"
                class="px-3 py-1 bg-gray-200 rounded-lg text-sm"
              >
                拒绝
              </button>
            </div>
          </div>
        </div>
        <div v-else class="text-center text-gray-500 py-16 bg-white rounded-xl shadow-sm">
          <i class="fa fa-inbox text-4xl mb-3 text-gray-400"></i>
          <p>没有新的好友请求</p>
        </div>
      </div>

      <!-- 3. 分享记录标签 -->
      <div v-if="activeTab === 'shares'" class="space-y-3">
        <div v-if="shares.length > 0">
          <div v-for="share in shares" :key="share.id" :class="shareClass(share)">
            <div class="flex items-start space-x-4">
              <img
                :src="share.sender.avatarUrl"
                :alt="share.sender.name"
                class="w-12 h-12 rounded-full"
                @error="handleImageError"
              />
              <div>
                <p class="font-semibold text-dark font-bold">
                  {{ share.sender.name
                  }}<span class="text-xs text-gray-500 font-normal ml-1">分享了一个查询:</span>
                </p>
                <p class="text-md text-primary text-sm mt-1">
                  "{{ share.querySnapshot.userPrompt }}"
                </p>
                <p v-if="share.shareMessage" class="text-xs text-gray-500 mt-1 italic">
                  💬 {{ share.shareMessage }}
                </p>
                <p class="text-xs text-gray-400 mt-2">{{ formatDate(share.timestamp) }}</p>
                <p v-if="share.receiveStatus === 1" class="text-xs text-green-600 mt-1">
                  ✓ 已读
                </p>
                <p v-else-if="share.receiveStatus === 2" class="text-xs text-gray-500 mt-1">
                  🗑️ 已删除
                </p>
              </div>
            </div>
            <div class="flex flex-col space-y-2 items-end">
              <div class="flex space-x-2">
                <button
                  @click="handleViewShare(share)"
                  class="px-3 py-1 border border-gray-300 rounded-lg text-xs hover:bg-gray-50"
                >
                  <i class="fa fa-eye mr-1"></i> 查看结果
                </button>
                <button
                  @click="handleToggleReadStatus(share)"
                  :class="`px-3 py-1 border rounded-lg text-xs ${
                    share.isRead 
                      ? 'border-gray-300 bg-gray-50 text-gray-600' 
                      : 'border-primary/30 bg-primary/5 text-primary hover:bg-primary/10'
                  }`"
                  :title="share.isRead ? '标记为未读' : '标记为已读'"
                >
                  <i :class="`fa ${share.isRead ? 'fa-check-circle' : 'fa-circle-o'} mr-1`"></i> 
                  {{ share.isRead ? '已读' : '未读' }}
                </button>
                <button
                  @click="handleRerunShare(share)"
                  class="px-3 py-1 bg-primary text-white rounded-lg text-xs hover:shadow-md"
                >
                  <i class="fa fa-paper-plane mr-1"></i> 导入并重新执行
                </button>
                <button
                  @click="handleDeleteShareRequest(share)"
                  class="px-3 py-1 bg-red-500 hover:bg-red-600 text-white rounded-lg text-xs transition-colors"
                >
                  <i class="fa fa-trash-o mr-1"></i> 删除
                </button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="text-center text-gray-500 py-16 bg-white rounded-xl shadow-sm">
          <i class="fa fa-share-alt-square text-4xl mb-3 text-gray-400"></i>
          <p>没有收到任何分享</p>
        </div>
      </div>

      <!-- 4. 我的分享标签 -->
      <div v-if="activeTab === 'shared'" class="space-y-3">
        <div v-if="sharedQueries.length > 0">
          <div v-for="share in sharedQueries" :key="share.id" :class="shareClassForShared(share)">
            <div class="flex items-start space-x-4">
              <img
                :src="share.receiver.avatarUrl"
                :alt="share.receiver.name"
                class="w-12 h-12 rounded-full"
                @error="handleImageError"
              />
              <div>
                <p class="font-semibold text-dark font-bold">
                  分享给 <span class="text-primary">{{ share.receiver.name }}</span> 的查询:
                </p>
                <p class="text-md text-primary text-sm mt-1">
                  "{{ share.querySnapshot.userPrompt }}"
                </p>
                <p v-if="share.shareMessage" class="text-xs text-gray-500 mt-1 italic">
                  💬 {{ share.shareMessage }}
                </p>
                <p class="text-xs text-gray-400 mt-2">{{ formatDate(share.timestamp) }}</p>
                <!-- 显示接收者的处理状态（如果有） -->
                <p v-if="share.receiveStatus === 1" class="text-xs text-green-600 mt-1">
                  ✓ 对方已读
                </p>
                <p v-else-if="share.receiveStatus === 2" class="text-xs text-gray-500 mt-1">
                  🗑️ 对方已删除
                </p>
                <p v-else-if="share.receiveStatus === 0" class="text-xs text-blue-500 mt-1">
                  ⏳ 待处理
                </p>
              </div>
            </div>
            <div class="flex flex-col space-y-2 items-end">
              <div class="flex space-x-2">
                <button
                  @click="handleViewShare(share)"
                  class="px-3 py-1 border border-gray-300 rounded-lg text-xs hover:bg-gray-50"
                >
                  <i class="fa fa-eye mr-1"></i> 查看结果
                </button>
                <button
                  @click="handleRerunShare(share)"
                  class="px-3 py-1 bg-primary text-white rounded-lg text-xs hover:shadow-md"
                >
                  <i class="fa fa-paper-plane mr-1"></i> 导入并重新执行
                </button>
                <button
                  @click="handleDeleteSharedQuery(share)"
                  class="px-3 py-1 bg-red-500 hover:bg-red-600 text-white rounded-lg text-xs transition-colors"
                >
                  <i class="fa fa-trash-o mr-1"></i> 删除
                </button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="text-center text-gray-500 py-16 bg-white rounded-xl shadow-sm">
          <i class="fa fa-share-alt-square text-4xl mb-3 text-gray-400"></i>
          <p>还没有分享任何查询</p>
        </div>
      </div>
    </div>

    <!-- 所有模态框 -->

    <!-- 添加好友模态框 -->
    <Modal :is-open="isAddFriendModalOpen" @close="closeAddFriendModal" title="添加好友">
      <template v-slot:default>
        <div class="space-y-6">
          <!-- 搜索方式选择 -->
          <div>
            <label class="block text-base font-semibold text-gray-800 mb-3">搜索方式</label>
            <div class="grid grid-cols-2 gap-2">
              <label class="flex items-center p-2 border-2 rounded-lg cursor-pointer transition-all border-gray-200 hover:border-gray-300">
                <input
                  type="radio"
                  name="searchType"
                  value="id"
                  v-model="searchType"
                  class="mr-2 w-4 h-4 text-primary focus:ring-primary"
                />
                <span class="text-sm font-medium text-gray-700">用户ID</span>
              </label>
              <label class="flex items-center p-2 border-2 rounded-lg cursor-pointer transition-all border-gray-200 hover:border-gray-300">
                <input
                  type="radio"
                  name="searchType"
                  value="username"
                  v-model="searchType"
                  class="mr-2 w-4 h-4 text-primary focus:ring-primary"
                />
                <span class="text-sm font-medium text-gray-700">用户名</span>
              </label>
              <label class="flex items-center p-2 border-2 rounded-lg cursor-pointer transition-all border-gray-200 hover:border-gray-300">
                <input
                  type="radio"
                  name="searchType"
                  value="email"
                  v-model="searchType"
                  class="mr-2 w-4 h-4 text-primary focus:ring-primary"
                />
                <span class="text-sm font-medium text-gray-700">邮箱</span>
              </label>
              <label class="flex items-center p-2 border-2 rounded-lg cursor-pointer transition-all border-gray-200 hover:border-gray-300">
                <input
                  type="radio"
                  name="searchType"
                  value="phone"
                  v-model="searchType"
                  class="mr-2 w-4 h-4 text-primary focus:ring-primary"
                />
                <span class="text-sm font-medium text-gray-700">手机号</span>
              </label>
            </div>
          </div>

          <!-- 搜索输入框 -->
          <div>
            <label class="block text-base font-semibold text-gray-800 mb-2">
              {{ getSearchPlaceholder() }}
            </label>
            <div class="flex space-x-3">
              <input
                :type="getSearchInputType()"
                :placeholder="getSearchPlaceholder()"
                v-model="searchKey"
                class="flex-1 px-4 py-3 text-base border-2 border-gray-200 rounded-lg focus:outline-none focus:border-primary transition-colors"
                @keyup.enter="handleSearchUser"
              />
              <button
                @click="handleSearchUser"
                type="button"
                class="px-6 py-3 bg-primary text-white rounded-lg text-base font-medium hover:bg-primary/90 hover:shadow-md transition-all cursor-pointer flex items-center justify-center min-w-[120px]"
              >
                <i v-if="isSearching" class="fa fa-spinner fa-spin"></i>
                <i v-else class="fa fa-search"></i>
              </button>
            </div>
          </div>

          <!-- 搜索结果 -->
          <div v-if="searchResult" class="p-5 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl border-2 border-primary/20">
            <div class="flex items-center space-x-4">
              <div class="relative">
                <img
                  :src="searchResult.avatarUrl || '/default-avatar.png'"
                  :alt="searchResult.username"
                  class="w-16 h-16 rounded-full border-2 border-white shadow-md object-cover"
                  @error="handleImageError"
                />
                <div class="absolute -bottom-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-white"></div>
              </div>
              <div class="flex-1">
                <p class="text-lg font-bold text-gray-800 mb-1">{{ searchResult.username }}</p>
                <p class="text-sm text-gray-600 mb-1">
                  <i class="fa fa-id-card mr-1"></i>
                  ID: <span class="font-semibold">{{ searchResult.id }}</span>
                </p>
                <p class="text-sm text-gray-600">
                  <i class="fa fa-envelope mr-1"></i>
                  {{ searchResult.email || '未设置邮箱' }}
                </p>
              </div>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="searchError" class="p-4 bg-red-50 rounded-lg border-2 border-red-200">
            <div class="flex items-center">
              <i class="fa fa-exclamation-circle text-red-500 mr-2"></i>
              <p class="text-sm font-medium text-red-600">{{ searchError }}</p>
            </div>
          </div>

          <!-- 确认添加提示和确认按钮 -->
          <div v-if="searchResult" class="space-y-3">
            <div class="p-4 bg-blue-50 rounded-lg border border-blue-200">
              <p class="text-sm text-blue-700">
                <i class="fa fa-info-circle mr-2"></i>
                找到用户，点击下方"确认添加"按钮发送好友请求
              </p>
            </div>
            <button
              @click="handleSendFriendRequest"
              class="w-full px-6 py-3 bg-primary text-white rounded-lg text-base font-medium hover:bg-primary/90 hover:shadow-md transition-all"
            >
              <i class="fa fa-check mr-2"></i>
              确认添加好友
            </button>
          </div>
        </div>
      </template>
      <template v-slot:footer>
        <!-- 移除取消按钮 -->
      </template>
    </Modal>

    <!-- 删除好友确认模态框 -->
    <Modal :is-open="!!friendToDelete" @close="friendToDelete = null" title="删除好友">
      <template v-slot:default>
        <p class="text-sm text-gray-700">
          您确定要删除好友 "{{ friendToDelete?.name }}" 吗？此操作无法撤销。
        </p>
      </template>
      <template v-slot:footer>
        <div class="mt-6 flex justify-end space-x-3">
          <button
            @click="friendToDelete = null"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm"
          >
            取消
          </button>
          <button
            @click="handleConfirmDelete"
            class="px-4 py-2 bg-danger text-white rounded-lg text-sm"
          >
            确认删除
          </button>
        </div>
      </template>
    </Modal>

    <!-- 聊天窗口 -->
    <ChatModal
      v-if="chattingWith"
      :is-open="!!chattingWith"
      :on-close="() => chattingWith = null"
      :friend="chattingWith"
      :saved-queries="savedQueries"
      :current-unread-count="unreadMessages[chattingWith.id] || 0"
      :update-unread-count="updateUnreadCount"
      :messages="chattingWith ? (friendMessages[chattingWith.id] || []) : []"
      :update-messages="(msgs) => chattingWith && updateFriendMessages(chattingWith.id, msgs)"
    />

    <!-- 查看分享结果 -->
    <Modal :is-open="!!viewingShare" @close="viewingShare = null" :title="viewShareTitle">
      <div v-if="viewingShare && viewingShare.querySnapshot" class="max-h-[70vh] overflow-y-auto -m-6 p-6">
        <QueryResult
          :result="viewingShare.querySnapshot"
          :show-actions="{ save: false, share: false, export: true }"
        />
      </div>
      <div v-else-if="viewingShare" class="p-6 text-center text-gray-500">
        <i class="fa fa-exclamation-circle text-4xl mb-4 text-gray-400"></i>
        <p>分享结果数据加载失败，请稍后重试</p>
        <button @click="viewingShare = null" class="mt-4 px-4 py-2 bg-primary text-white rounded-lg">
          关闭
        </button>
      </div>
    </Modal>

    <!-- 保存成功提示 -->
    <Modal :is-open="activeModal === 'saveSuccess'" @close="activeModal = null" :hide-title="true">
      <template v-slot:default>
        <div class="text-center">
          <div
            class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4"
          >
            <i class="fa fa-check text-green-500 text-3xl"></i>
          </div>
          <h3 class="text-xl font-bold text-gray-800 mb-2">保存成功</h3>
          <p class="text-sm text-gray-500">该查询结果已保存至您的历史记录中。</p>
          <button
            @click="activeModal = null"
            class="mt-4 px-6 py-2 bg-primary text-white rounded-lg"
          >
            确定
          </button>
        </div>
      </template>
    </Modal>

    <!-- 备注好友模态框 -->
    <Modal :is-open="isRemarkModalOpen" @close="closeRemarkModal" title="备注好友">
      <template v-slot:default>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              好友名称：{{ currentRemarkFriend?.name }}
            </label>
            <input
              type="text"
              placeholder="输入备注（可选）"
              v-model="remarkInput"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg"
              maxlength="20"
            />
            <p class="text-xs text-gray-400 mt-1">备注将显示在好友名称下方，最多20个字符</p>
          </div>
        </div>
      </template>
      <template v-slot:footer>
        <div class="mt-6 flex justify-end space-x-3">
          <button
            @click="closeRemarkModal"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm"
          >
            取消
          </button>
          <button
            @click="handleSaveRemark"
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm"
          >
            保存备注
          </button>
        </div>
      </template>
    </Modal>

    <!-- 好友主页模态框 -->
    <Modal :is-open="isProfileModalOpen" @close="closeProfileModal" title="好友主页" content-class-name="max-w-md">
      <template v-slot:default>
        <div v-if="currentProfileFriend" class="space-y-6">
          <div class="text-center">
            <div class="w-32 h-32 rounded-full mx-auto overflow-hidden border-4 border-primary/20 shadow-lg cursor-pointer hover:border-primary/40 transition-all">
              <img
                :src="currentProfileFriend.avatarUrl"
                :alt="currentProfileFriend.name"
                class="w-full h-full object-cover"
                @error="handleImageError"
              />
            </div>
            <h3 class="text-2xl font-bold mt-4 text-gray-800">{{ currentProfileFriend.remark || currentProfileFriend.name }}</h3>
            <p v-if="currentProfileFriend.remark" class="text-gray-500 italic mt-1">
              「{{ currentProfileFriend.name }}」
            </p>
            <span :class="profileStatusClass" class="mt-2 inline-block">
              {{ currentProfileFriend.isOnline ? '在线' : '离线' }}
            </span>
          </div>

          <!-- 详细信息 -->
          <div class="bg-gray-50 rounded-xl p-4 space-y-3">
            <div class="flex items-center justify-between py-2 border-b border-gray-200">
              <span class="text-sm font-medium text-gray-500">用户ID</span>
              <span class="text-sm text-gray-800 font-semibold">#{{ currentProfileFriend.friendId || currentProfileFriend.id }}</span>
            </div>
            <div class="flex items-center justify-between py-2 border-b border-gray-200">
              <span class="text-sm font-medium text-gray-500 flex items-center">
                <i class="fa fa-envelope-o mr-2 text-primary"></i>
                邮箱
              </span>
              <span class="text-sm text-gray-800">{{ currentProfileFriend?.email || '未设置' }}</span>
            </div>
            <div class="flex items-center justify-between py-2">
              <span class="text-sm font-medium text-gray-500 flex items-center">
                <i class="fa fa-phone mr-2 text-primary"></i>
                手机号
              </span>
              <span class="text-sm text-gray-800">{{ currentProfileFriend?.phoneNumber || '未设置' }}</span>
            </div>
          </div>

          <div class="flex flex-col space-y-3">
            <button
              @click="handleOpenChatFromProfile"
              class="w-full py-3 bg-primary text-white rounded-lg text-base font-medium flex items-center justify-center hover:bg-primary/90 hover:shadow-md transition-all"
            >
              <i class="fa fa-comment-o mr-2"></i> 发起聊天
            </button>
            <button
              @click="handleOpenRemarkFromProfile"
              class="w-full py-3 border-2 border-gray-300 rounded-lg text-base font-medium flex items-center justify-center hover:bg-gray-50 hover:border-primary/30 transition-all"
            >
              <i class="fa fa-pencil mr-2"></i> 编辑备注
            </button>
          </div>
        </div>
      </template>
    </Modal>

    <!-- 分享给好友模态框 -->
    <Modal :is-open="isShareToFriendModalOpen" @close="closeShareToFriendModal" title="分享查询给好友">
      <template v-slot:default>
        <div v-if="currentShareFriend" class="space-y-4">
          <div class="p-3 bg-blue-50 rounded-lg border border-blue-200">
            <p class="text-sm text-blue-700">
              <i class="fa fa-info-circle mr-2"></i>
              从您的收藏夹中选择要分享给 <span class="font-semibold">{{ currentShareFriend.remark || currentShareFriend.name }}</span> 的查询记录
            </p>
          </div>
          
          <div class="max-h-96 overflow-y-auto space-y-2 border rounded-lg p-2">
            <template v-if="shareToFriendQueries.length > 0">
              <label
                v-for="query in shareToFriendQueries"
                :key="query.id"
                :class="`flex items-center p-3 rounded-lg cursor-pointer transition-colors ${
                  selectedQueriesForShare.includes(query.id) ? 'bg-primary/20 border-2 border-primary' : 'hover:bg-gray-100 border border-gray-200'
                }`"
              >
                <input
                  type="checkbox"
                  :value="query.id"
                  v-model="selectedQueriesForShare"
                  class="mr-3"
                />
                <div class="flex-1">
                  <p class="text-sm font-medium text-gray-800 truncate" :title="query.userPrompt || '未命名查询'">
                    {{ query.userPrompt || '未命名查询' }}
                  </p>
                  <p class="text-xs text-gray-500 mt-1">
                    {{ new Date(query.queryTime).toLocaleString() }}
                  </p>
                </div>
              </label>
            </template>
            <p v-else class="text-center text-gray-500 text-sm p-4">收藏夹中没有可分享的查询记录</p>
          </div>
          <div v-if="shareToFriendQueries.length > 0" class="flex items-center justify-between text-sm text-gray-600 mt-2">
            <span>已选择 {{ selectedQueriesForShare.length }} 个查询</span>
            <button
              @click="selectedQueriesForShare = shareToFriendQueries.map(q => q.id)"
              class="text-primary hover:underline"
            >
              全选
            </button>
          </div>
        </div>
      </template>
      <template v-slot:footer>
        <div class="flex justify-end space-x-3">
          <button
            @click="closeShareToFriendModal"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-50"
          >
            取消
          </button>
          <button
            @click="handleConfirmShareToFriend"
            :disabled="selectedQueriesForShare.length === 0"
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary/90 disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            确认分享
          </button>
        </div>
      </template>
    </Modal>

    <!-- 通知模态框 -->
    <Modal :is-open="isNotificationModalOpen" @close="closeNotificationModal" title="通知">
      <template v-slot:default>
        <div v-if="currentNotificationShare" class="space-y-4">
          <div v-if="currentNotificationShare.notifications && currentNotificationShare.notifications.length > 0">
            <div
              v-for="notif in currentNotificationShare.notifications"
              :key="notif.id"
              class="p-4 bg-gray-50 rounded-lg border border-gray-200"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <h4 class="font-semibold text-gray-800 mb-1">{{ notif.title }}</h4>
                  <p class="text-sm text-gray-600">{{ notif.content }}</p>
                  <p class="text-xs text-gray-400 mt-2">
                    {{ new Date(notif.publishTime || notif.createTime).toLocaleString() }}
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="text-center text-gray-500 py-8">
            <i class="fa fa-bell-slash text-3xl mb-2 block"></i>
            <p>暂无相关通知</p>
          </div>
        </div>
      </template>
      <template v-slot:footer>
        <div class="flex justify-end">
          <button
            @click="closeNotificationModal"
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:bg-primary/90"
          >
            关闭
          </button>
        </div>
      </template>
    </Modal>

    <!-- 删除分享确认模态框 -->
    <Modal :is-open="!!shareToDelete" @close="shareToDelete = null" title="删除分享">
      <template v-slot:default>
        <p class="text-sm text-gray-700">
          您确定要删除 "{{ shareToDelete?.sender.name }}" 分享的查询吗？此操作无法撤销。
        </p>
      </template>
      <template v-slot:footer>
        <div class="mt-6 flex justify-end space-x-3">
          <button
            @click="shareToDelete = null"
            class="px-4 py-2 border border-gray-300 rounded-lg text-sm"
          >
            取消
          </button>
          <button
            @click="handleConfirmDeleteShare"
            class="px-4 py-2 bg-danger text-white rounded-lg text-sm"
          >
            确认删除
          </button>
        </div>
      </template>
    </Modal>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import Modal from '../components/ui/Modal.vue'
import ChatModal from '../components/feature/chat/ChatModal.vue'
import QueryResult from '../components/feature/query/QueryResult.vue'
import { friendRelationApi, friendRequestApi, friendChatApi, userApi, userSearchApi, queryShareApi, queryLogApi, notificationApi, queryShareChatApi, dialogDetailApi, collectionRecordApi, queryCollectionApi } from '../services/api.real'
import type { QueryResultData, Friend, ChatMessage } from '../types'
import { connectWebSocket, onMessage, isWebSocketConnected } from '../services/websocket/chatWebSocket'
import type { ChatMessageVO } from '../services/api/friend'
import { parseQueryShareMessage as parseQueryShareMessageUtil } from '../utils/queryShareMessage'

// 扩展 Friend 类型以包含额外属性
interface ExtendedFriend extends Friend {
  relationId?: number
  friendId?: number
}

interface Props {
  // 暂无 props
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'rerun-query': [prompt: string, dbConnectionId?: number]
  'update:query-title': [title: string]
  'switch-to-query': []
}>()

// 内部管理的状态
const savedQueries = ref<QueryResultData[]>([])
const shares = ref<any[]>([])
const sharedQueries = ref<any[]>([]) // 我分享出去的查询

// 页面状态
const friends = ref<ExtendedFriend[]>([])
const requests = ref<any[]>([])
const loading = ref(true)
const isAddFriendModalOpen = ref(false)
const chattingWith = ref<ExtendedFriend | null>(null)
const searchTerm = ref('')
const friendToDelete = ref<ExtendedFriend | null>(null)
const activeTab = ref<'friends' | 'requests' | 'shares' | 'shared'>('friends')
const viewingShare = ref<any | null>(null)
const activeModal = ref<string | null>(null)

// 备注相关状态
const isRemarkModalOpen = ref(false)
const currentRemarkFriend = ref(null)
const remarkInput = ref('')

// 好友主页相关状态
const isProfileModalOpen = ref(false)
const currentProfileFriend = ref(null)

// 分享给好友相关状态
const isShareToFriendModalOpen = ref(false)
const currentShareFriend = ref<ExtendedFriend | null>(null)
const shareToFriendQueries = ref<QueryResultData[]>([])
const selectedQueriesForShare = ref<string[]>([]) // 改为数组，支持批量选择

// 通知相关状态
const isNotificationModalOpen = ref(false)
const currentNotificationShare = ref<any | null>(null)

// 添加好友相关状态
const searchKey = ref('')
const searchType = ref<'id' | 'username' | 'email' | 'phone'>('id')
const searchResult = ref<any | null>(null)
const isSearching = ref(false)
const searchError = ref('')

// 未读消息状态
const unreadMessages = ref<Record<string, number>>({})

// 聊天记录状态
const friendMessages = ref<Record<string, ChatMessage[]>>({})

// 待删除的分享
const shareToDelete = ref(null)

// 计算属性
const filteredFriends = computed(() =>
  friends.value.filter(
    (friend) =>
      friend.name.toLowerCase().includes(searchTerm.value.toLowerCase()) ||
      (friend.remark && friend.remark.toLowerCase().includes(searchTerm.value.toLowerCase())),
  ),
)

const unreadSharesCount = computed(() => shares.value.filter((s) => s.receiveStatus === 0).length)

const viewShareTitle = computed(() => {
  if (!viewingShare.value) return '查看分享'
  if (!viewingShare.value.querySnapshot) return '查看分享（数据加载中）'
  return `查看分享: ${viewingShare.value.querySnapshot.userPrompt || '查询结果'}`
})

const profileStatusClass = computed(() => {
  if (!currentProfileFriend.value) return ''
  return currentProfileFriend.value.isOnline
    ? 'inline-block mt-2 px-3 py-1 rounded-full text-xs bg-green-100 text-green-800'
    : 'inline-block mt-2 px-3 py-1 rounded-full text-xs bg-gray-100 text-gray-600'
})

// 方法
const tabClass = (tab) => {
  return `py-2 px-4 text-sm font-medium transition-colors duration-200 relative ${
    activeTab.value === tab
      ? 'text-primary border-b-2 border-primary'
      : 'text-gray-500 hover:text-dark'
  }`
}

/**
 * 分享记录的样式类（用于"分享记录"标签页）
 * 
 * @param share 分享记录对象
 * @returns 样式类字符串
 */
const shareClass = (share) => {
  return `p-4 rounded-xl shadow-sm border flex items-start justify-between transition-shadow hover:shadow-md ${
    share.receiveStatus === 0 ? 'bg-primary/5 border-primary/20' : 'bg-white'
  }`
}

/**
 * 我的分享记录的样式类（用于"我的分享"标签页）
 * 
 * <p>功能说明：
 * <ul>
 *   <li>为我的分享记录提供统一的样式类</li>
 *   <li>使用白色背景，与"分享记录"保持一致的视觉效果</li>
 * </ul>
 * 
 * @param share 分享记录对象
 * @returns 样式类字符串
 */
const shareClassForShared = (share) => {
  // 我的分享记录统一使用白色背景，与"分享记录"保持一致的视觉效果
  return `p-4 rounded-xl shadow-sm border flex items-start justify-between transition-shadow hover:shadow-md bg-white`
}

// 加载好友列表
const loadFriends = async () => {
  try {
    loading.value = true
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const relations = await friendRelationApi.getByUser(userId)

    const friendsData = await Promise.all(
      relations.map(async (relation) => {
        const friendUser = await userApi.getById(relation.friendId)
        return {
          id: String(relation.id),
          name: friendUser.username,
          avatarUrl: friendUser.avatarUrl || '/default-avatar.png',
          isOnline: friendUser.status === 1,
          remark: relation.remarkName || relation.remark || '',
          email: friendUser.email,
          phoneNumber: friendUser.phonenumber || '',
          relationId: relation.id,
          friendId: relation.friendId,
        }
      }),
    )

    friends.value = friendsData
    
    // 加载每个好友的未读消息数
    await loadUnreadCounts(userId)
  } catch (error) {
    console.error('加载好友列表失败:', error)
    alert('加载好友列表失败')
  } finally {
    loading.value = false
  }
}

// 加载未读消息数
const loadUnreadCounts = async (userId: number) => {
  try {
    const unreadCounts = await friendChatApi.getUnreadCountByFriends(userId)
    const newUnreadMessages: Record<string, number> = {}
    
    // 将未读数映射到好友ID
    unreadCounts.forEach((item) => {
      // 找到对应的好友
      const friend = friends.value.find(f => 
        Number(f.friendId || f.id) === item.friendId
      )
      if (friend) {
        newUnreadMessages[String(friend.id)] = item.unreadCount
      }
    })
    
    unreadMessages.value = {
      ...unreadMessages.value,
      ...newUnreadMessages,
    }
  } catch (error) {
    console.error('加载未读消息数失败:', error)
  }
}

// 加载好友请求
const loadRequests = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const requestsData = await friendRequestApi.getByRecipientAndStatus(userId, 0)

    const requestsWithUser = await Promise.all(
      requestsData.map(async (req) => {
        const applicantUser = await userApi.getById(req.applicantId)
        return {
          id: String(req.id),
          fromUser: {
            id: String(applicantUser.id),
            name: applicantUser.username,
            avatarUrl: applicantUser.avatarUrl || '/default-avatar.png',
          },
          timestamp: new Date(req.createTime).toLocaleString(),
          requestId: req.id,
        }
      }),
    )

    requests.value = requestsWithUser
  } catch (error) {
    console.error('加载好友请求失败:', error)
  }
}

// 加载分享记录 - 使用新的 queryShareChatApi
const loadShares = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 使用新的 API 获取收到的查询分享
    const sharesData = await queryShareChatApi.getReceived(userId)
    
    // 转换数据格式以匹配前端显示
    const sharesWithUser = sharesData.map((share) => {
      // 从 QueryShareVO 转换为前端需要的格式
      return {
        id: share.shareId,
        sender: {
          id: String(share.shareUserId),
          name: share.shareUserName || '未知用户',
          avatarUrl: share.shareUserAvatar || '/default-avatar.png',
        },
        querySnapshot: {
          id: share.shareId,
          userPrompt: share.queryTitle,
          sqlQuery: share.sqlQuery,
          conversationId: share.dialogId || '',
          queryTime: share.queryTime,
          executionTime: share.executionTimeText || `${share.executionTime || 0}ms`,
          database: share.databaseName,
          model: share.llmName || '',
          tableData: share.tableData || { headers: [], rows: [] },
          chartData: share.chartData,
          dbConnectionId: share.dbConnectionId, // 保存数据库连接ID
          llmConfigId: share.llmConfigId, // 保存大模型配置ID
        },
        timestamp: share.shareTime,
        receiveStatus: share.receiveStatus || 0, // 保存原始状态，默认为0（未处理）
        shareMessage: share.shareMessage || '', // 保存分享留言
        isRead: share.receiveStatus !== 0, // 已读状态：已保存或已删除视为已读
      }
    })
    
    shares.value = sharesWithUser
    console.log('加载分享记录成功，数量:', shares.value.length)
  } catch (error) {
    console.error('加载分享记录失败:', error)
    // 即使加载失败，也保持空数组，避免UI崩溃
    shares.value = []
  }
}

/**
 * 加载我分享出去的查询记录
 * 
 * <p>功能说明：
 * <ul>
 *   <li>从后端API获取当前用户分享给好友的所有查询记录</li>
 *   <li>获取接收者信息并显示接收者的处理状态（已保存/已删除/待处理）</li>
 *   <li>转换数据格式以匹配前端显示需求</li>
 *   <li>用于在"我的分享"标签页中显示分享记录</li>
 * </ul>
 */
const loadSharedQueries = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 使用新的 API 获取分享出去的查询
    const sharedData = await queryShareChatApi.getShared(userId)
    
    // 转换数据格式以匹配前端显示
    const sharedWithUser = await Promise.all(
      sharedData.map(async (share) => {
        // 获取接收者信息
        const receiveUser = await userApi.getById(share.receiveUserId)
        
        return {
          id: share.shareId,
          receiver: {
            id: String(share.receiveUserId),
            name: receiveUser.username || '未知用户',
            avatarUrl: receiveUser.avatarUrl || '/default-avatar.png',
          },
          querySnapshot: {
            id: share.shareId,
            userPrompt: share.queryTitle,
            sqlQuery: share.sqlQuery,
            conversationId: share.dialogId || '',
            queryTime: share.queryTime,
            executionTime: share.executionTimeText || `${share.executionTime || 0}ms`,
            database: share.databaseName,
            model: share.llmName || '',
            tableData: share.tableData || { headers: [], rows: [] },
            chartData: share.chartData,
            dbConnectionId: share.dbConnectionId, // 保存数据库连接ID
            llmConfigId: share.llmConfigId, // 保存大模型配置ID
          },
          timestamp: share.shareTime,
          shareMessage: share.shareMessage || '',
          receiveStatus: share.receiveStatus || 0, // 接收者的处理状态：0-未处理，1-已保存，2-已删除
        }
      })
    )
    
    sharedQueries.value = sharedWithUser
    console.log('加载我分享的查询成功，数量:', sharedQueries.value.length)
  } catch (error) {
    console.error('加载我分享的查询失败:', error)
    // 即使加载失败，也保持空数组，避免UI崩溃
    sharedQueries.value = []
  }
}

// 统一的转换函数：将 CollectionRecord 转换为 QueryResultData
// 使用 record.id 作为唯一标识，确保收藏夹页面和分享功能使用相同的ID
const recordToQueryResult = (record: any): QueryResultData | null => {
  if (!record || !record.id) {
    console.warn('[FriendsPage] 无效的收藏记录:', record)
    return null
  }
  
  // 如果没有queryResult，尝试从其他字段构建
  let result: any = {}
  if (record.queryResult) {
    try {
      result = typeof record.queryResult === 'string' 
        ? JSON.parse(record.queryResult) 
        : record.queryResult
    } catch (e) {
      console.warn('[FriendsPage] 解析queryResult失败:', e, 'record:', record)
      result = {}
    }
  }
  
  // 确保tableData格式正确
  let tableData = result?.tableData
  if (!tableData || !Array.isArray(tableData.headers) || !Array.isArray(tableData.rows)) {
    tableData = { headers: [], rows: [] }
  }
  
  // 使用 record.id 作为唯一标识（与收藏夹页面保持一致）
  // 这样收藏夹页面显示的和分享功能中的记录ID就能匹配上
  const recordId = String(record.id)
  
  return {
    id: recordId,
    userPrompt: record.userPrompt || '查询结果',
    sqlQuery: record.sqlContent || '',
    conversationId: result?.conversationId || '',
    queryTime: record.createTime || record.addTime || new Date().toISOString(),
    executionTime: result?.executionTime || '0ms',
    database: result?.database || String(record.dbConnectionId || ''),
    model: result?.model || String(record.llmConfigId || ''),
    tableData: tableData,
    chartData: result?.chartData,
    dbConnectionId: record.dbConnectionId,
    llmConfigId: record.llmConfigId,
  }
}

// 加载已保存的查询记录（用于 ChatModal 分享功能）
// 按收藏夹加载记录，与收藏夹页面保持一致
const loadSavedQueries = async () => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    
    // 先加载所有收藏夹
    const collections = await queryCollectionApi.getByUser(userId)
    console.log('[FriendsPage] 加载收藏夹，数量:', collections.length)
    
    // 为每个收藏夹加载记录（与收藏夹页面的逻辑一致）
    const allRecords: any[] = []
    for (const collection of collections) {
      try {
        const collectionId = String(collection.id)
        const records = await collectionRecordApi.getByCollection(collectionId)
        console.log('[FriendsPage] 收藏夹:', collection.collectionName, '记录数:', records.length)
        
        // 过滤掉无效的记录（与收藏夹页面保持一致）
        const validRecords = (records || []).filter(record => 
          record && record.id && (record.queryLogId || record.userPrompt)
        )
        
        allRecords.push(...validRecords)
      } catch (err) {
        console.error('[FriendsPage] 加载收藏夹记录失败:', err, 'collectionId:', collection.id)
      }
    }
    
    // 将 CollectionRecord 转换为 QueryResultData 格式
    savedQueries.value = allRecords.map(recordToQueryResult).filter((q): q is QueryResultData => q !== null)
    
    console.log('[FriendsPage] 从收藏夹加载查询记录成功，总数量:', savedQueries.value.length)
  } catch (error) {
    console.error('[FriendsPage] 从收藏夹加载查询记录失败:', error)
    savedQueries.value = []
  }
}

// 初始化加载
onMounted(async () => {
  await loadFriends()
  loadRequests()
  loadShares()
  loadSharedQueries()
  await loadSavedQueries() // 加载已保存的查询记录，供 ChatModal 分享使用
  
  // 初始化 WebSocket 监听
  await initWebSocketListener()
})

onUnmounted(() => {
  // 清理 WebSocket 订阅
  if (unsubscribeMessage) {
    unsubscribeMessage()
    unsubscribeMessage = null
  }
})

// 打开备注弹窗时初始化输入值
watch(currentRemarkFriend, (newVal) => {
  if (newVal) {
    remarkInput.value = newVal.remark || ''
  }
})

/**
 * 监听标签切换，当切换到"我的分享"标签时刷新数据
 * 
 * <p>功能说明：
 * <ul>
 *   <li>当用户切换到"我的分享"标签时，自动刷新分享记录列表</li>
 *   <li>确保显示的是最新的分享数据</li>
 * </ul>
 */
watch(activeTab, (newTab) => {
  if (newTab === 'shared') {
    // 切换到"我的分享"标签时，刷新数据
    loadSharedQueries()
  } else if (newTab === 'shares') {
    // 切换到"分享记录"标签时，刷新数据
    loadShares()
  }
})

// 更新未读消息数量
const updateUnreadCount = (friendId: string, count: number) => {
  unreadMessages.value = {
    ...unreadMessages.value,
    [friendId]: count,
  }
}

// WebSocket 消息监听
let unsubscribeMessage: (() => void) | null = null

// 初始化 WebSocket 并监听消息
const initWebSocketListener = async () => {
  const userId = Number(sessionStorage.getItem('userId') || '1')
  
  console.log('[FriendsPage] 初始化WebSocket监听，userId:', userId)
  
  // 如果 WebSocket 未连接，先连接
  if (!isWebSocketConnected()) {
    try {
      console.log('[FriendsPage] WebSocket未连接，正在连接...')
      await connectWebSocket(userId)
      console.log('[FriendsPage] WebSocket连接成功')
    } catch (error) {
      console.error('[FriendsPage] WebSocket连接失败:', error)
      return
    }
  } else {
    console.log('[FriendsPage] WebSocket已连接，直接注册回调')
  }
  
  // 注册消息接收回调（无论聊天框是否打开都监听）
  unsubscribeMessage = onMessage(async (message: ChatMessageVO) => {
    const currentUserId = Number(sessionStorage.getItem('userId') || '1')
    const messageSenderId = Number(message.senderId)
    const messageReceiverId = Number(message.receiverId)
    
    // 只处理发送给当前用户的消息（对方发送给我的）
    if (messageReceiverId === currentUserId && messageSenderId !== currentUserId) {
      // 找到对应的好友
      const friend = friends.value.find(f => 
        Number(f.friendId || f.id) === messageSenderId
      )
      
      if (friend) {
        const friendId = String(friend.id)
        
        // 如果聊天框没有打开，或者打开的不是这个好友的聊天框，增加未读数
        const isChattingWithThisFriend = chattingWith.value && chattingWith.value.id === friend.id
        if (!isChattingWithThisFriend) {
          const currentUnread = unreadMessages.value[friendId] || 0
          updateUnreadCount(friendId, currentUnread + 1)
        }
        
        // 更新消息列表（无论聊天框是否打开都更新，这样打开时能看到最新消息）
        const existingMessages = friendMessages.value[friendId] || []
        
        // 处理消息内容（支持文本和查询分享）
        let messageContent: string | QueryResultData
        let contentType: 'text' | 'query_share' = 'text'
        
        if (message.contentType === 'query_share' || 
            (typeof message.content === 'object' && message.content && ('queryId' in message.content || 'tableData' in message.content || 'queryLogId' in message.content))) {
          // 使用统一函数处理查询分享消息
          const result = await parseQueryShareMessageUtil(message.content, message.id, [])
          messageContent = result.content
          contentType = result.contentType
        } else {
          if (typeof message.content === 'string') {
            messageContent = message.content
          } else if (message.content && typeof message.content === 'object' && 'text' in message.content) {
            messageContent = (message.content as any).text || ''
          } else {
            messageContent = String(message.content || '')
          }
        }
        
        const newChatMessage: ChatMessage = {
          id: message.id,
          content: messageContent,
          isSent: false,
          timestamp: message.sendTime ? new Date(message.sendTime) : new Date(),
          isRead: message.isRead || false,
          contentType: contentType,
        }
        
        // 检查消息是否已存在
        const exists = existingMessages.some(msg => msg.id === newChatMessage.id)
        if (!exists) {
          updateFriendMessages(friendId, [...existingMessages, newChatMessage])
        }
      }
    }
    
    // 处理自己发送的消息（用于同步）
    if (messageSenderId === currentUserId) {
      const friend = friends.value.find(f => 
        Number(f.friendId || f.id) === messageReceiverId
      )
      
      if (friend) {
        const friendId = String(friend.id)
        const existingMessages = friendMessages.value[friendId] || []
        
        // 处理消息内容
        let messageContent: string | QueryResultData
        let contentType: 'text' | 'query_share' = 'text'
        
        if (message.contentType === 'query_share' || 
            (typeof message.content === 'object' && message.content && ('queryId' in message.content || 'tableData' in message.content || 'queryLogId' in message.content))) {
          // 使用统一函数处理查询分享消息
          const result = await parseQueryShareMessageUtil(message.content, message.id, [])
          messageContent = result.content
          contentType = result.contentType
        } else {
          if (typeof message.content === 'string') {
            messageContent = message.content
          } else if (message.content && typeof message.content === 'object' && 'text' in message.content) {
            messageContent = (message.content as any).text || ''
          } else {
            messageContent = String(message.content || '')
          }
        }
        
        const newChatMessage: ChatMessage = {
          id: message.id,
          content: messageContent,
          isSent: true,
          timestamp: message.sendTime ? new Date(message.sendTime) : new Date(),
          isRead: message.isRead || false,
          contentType: contentType,
        }
        
        const exists = existingMessages.some(msg => msg.id === newChatMessage.id)
        if (!exists) {
          updateFriendMessages(friendId, [...existingMessages, newChatMessage])
        }
      }
    }
  })
  
  console.log('[FriendsPage] WebSocket消息监听已注册')
}

// 更新聊天记录
const updateFriendMessages = (friendId: string, newMessages: ChatMessage[]) => {
  friendMessages.value = {
    ...friendMessages.value,
    [friendId]: newMessages,
  }
}

// 打开聊天
const handleOpenChat = async (friend) => {
  chattingWith.value = friend
  
  // 清除该好友的未读消息数
  updateUnreadCount(friend.id, 0)
  
  // 如果还没有加载过该好友的消息，初始化为空数组（让ChatModal自己加载）
  if (!friendMessages.value[friend.id]) {
    friendMessages.value = {
      ...friendMessages.value,
      [friend.id]: [],
    }
  }
}

// 获取搜索输入框类型
const getSearchInputType = () => {
  if (searchType.value === 'email') return 'email'
  if (searchType.value === 'phone') return 'tel'
  if (searchType.value === 'id') return 'number'
  return 'text'
}

// 获取搜索占位符
const getSearchPlaceholder = () => {
  switch (searchType.value) {
    case 'id':
      return '输入用户ID'
    case 'username':
      return '输入用户名'
    case 'email':
      return '输入好友的邮箱地址'
    case 'phone':
      return '输入好友的手机号'
    default:
      return '输入搜索关键词'
  }
}

// 搜索用户
const handleSearchUser = async () => {
  // 确保 searchKey.value 是字符串
  const searchValue = String(searchKey.value || '').trim()
  
  if (!searchValue) {
    alert('请输入搜索关键词')
    return
  }

  isSearching.value = true
  searchError.value = ''
  searchResult.value = null

  try {
    let user: any = null

    if (searchType.value === 'id') {
      const userId = Number(searchValue)
      if (isNaN(userId) || userId <= 0) {
        searchError.value = '请输入有效的用户ID'
        alert('请输入有效的用户ID')
        return
      }
      user = await userApi.getById(userId)
      if (!user) {
        alert('未找到该用户')
        searchError.value = '未找到该用户'
        return
      }
    } else if (searchType.value === 'username') {
      user = await userApi.getByUsername(searchValue)
      if (!user) {
        alert('未找到该用户')
        searchError.value = '未找到该用户'
        return
      }
    } else if (searchType.value === 'email') {
      const users = await userSearchApi.searchByEmail(searchValue)
      if (users && users.length > 0) {
        user = users[0]
      } else {
        alert('未找到该用户')
        searchError.value = '未找到该用户'
        return
      }
    } else if (searchType.value === 'phone') {
      try {
        user = await userSearchApi.searchByPhoneNumber(searchValue)
        if (!user) {
          alert('未找到该用户')
          searchError.value = '未找到该用户'
          return
        }
      } catch (phoneError) {
        // 后端API在找不到用户时会返回错误
        alert('未找到该用户')
        searchError.value = '未找到该用户'
        return
      }
    }

    if (user) {
      // 检查是否是系统管理员（roleId === 1）
      if (user.roleId === 1) {
        alert('不能添加系统管理员为好友')
        searchError.value = '不能添加系统管理员为好友'
        searchResult.value = null
        return
      }
      
      searchResult.value = {
        id: user.id,
        username: user.username,
        email: user.email,
        avatarUrl: user.avatarUrl || '/default-avatar.png',
      }
      searchError.value = ''
    } else {
      alert('未找到该用户')
      searchError.value = '未找到该用户'
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    const errorMessage = error instanceof Error ? error.message : '搜索失败，请稍后重试'
    alert(errorMessage)
    searchError.value = errorMessage
    searchResult.value = null
  } finally {
    isSearching.value = false
  }
}

// 发送好友请求
const handleSendFriendRequest = async () => {
  if (!searchResult.value) {
    alert('请先搜索用户')
    return
  }

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 检查是否是自己
    if (userId === searchResult.value.id) {
      alert('不能添加自己为好友')
      return
    }
    
    await friendRequestApi.create({
      applicantId: userId,
      recipientId: searchResult.value.id,
      status: 0,
    })
    alert('好友请求已发送')
    closeAddFriendModal()
  } catch (error) {
    console.error('发送好友请求失败:', error)
    const errorMessage = error instanceof Error ? error.message : '发送好友请求失败'
    alert(errorMessage)
  }
}

// 删除好友逻辑
const handleDeleteRequest = (friend) => {
  friendToDelete.value = friend
}

const handleConfirmDelete = async () => {
  if (!friendToDelete.value) return
  
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const friendId = Number(friendToDelete.value.friendId)
    
    if (!friendId || isNaN(friendId)) {
      alert('好友ID无效，无法删除')
      friendToDelete.value = null
      return
    }
    
    // 调用API删除好友关系
    await friendRelationApi.deleteByUserAndFriend(userId, friendId)
    
    // 删除成功后重新加载好友列表
    await loadFriends()
    
    // 清理相关的未读消息和聊天记录
    const friendRelationId = friendToDelete.value.id
    const newUnread = { ...unreadMessages.value }
    delete newUnread[friendRelationId]
    unreadMessages.value = newUnread

    const newMessages = { ...friendMessages.value }
    delete newMessages[friendRelationId]
    friendMessages.value = newMessages

    friendToDelete.value = null
    alert('已删除好友')
  } catch (error) {
    console.error('删除好友失败:', error)
    const errorMessage = error instanceof Error ? error.message : '删除好友失败，请重试'
    alert(errorMessage)
    friendToDelete.value = null
  }
}

// 好友请求处理
const handleAcceptRequest = async (request) => {
  try {
    await friendRequestApi.accept(request.requestId)
    alert('已接受好友请求')

    await loadFriends()
    await loadRequests()
  } catch (error) {
    console.error('接受好友请求失败:', error)
    alert(error instanceof Error ? error.message : '接受好友请求失败')
  }
}

const handleRejectRequest = async (requestId) => {
  try {
    // 获取请求信息，以便发送通知给申请人
    const request = await friendRequestApi.getById(Number(requestId))
    
    // 拒绝请求
    await friendRequestApi.reject(Number(requestId))
    
    // 发送通知给申请人
    const currentUserId = Number(sessionStorage.getItem('userId') || '1')
    const currentUser = await userApi.getById(currentUserId)
    
    try {
      await notificationApi.create({
        title: '好友请求被拒绝',
        content: `${currentUser.username} 拒绝了您的好友请求`,
        targetId: request.applicantId,
        priorityId: 2, // 普通优先级
        publisherId: currentUserId,
        isTop: 0,
      })
    } catch (notifError) {
      console.error('发送通知失败:', notifError)
      // 通知发送失败不影响拒绝操作
    }
    
    alert('已拒绝好友请求')
    await loadRequests()
  } catch (error) {
    console.error('拒绝好友请求失败:', error)
    alert('拒绝好友请求失败')
  }
}

/**
 * 查看分享详情
 * 
 * <p>功能说明：
 * <ul>
 *   <li>在模态框中显示分享的查询结果详情</li>
 *   <li>支持查看查询结果、SQL语句、图表等完整信息</li>
 *   <li>用于"分享记录"和"我的分享"两个标签页</li>
 * </ul>
 * 
 * @param share 分享记录对象，包含querySnapshot等详细信息
 */
const handleViewShare = async (share) => {
  viewingShare.value = share
  // 查看分享时不需要调用保存API，只是查看
  // 如果需要标记为已读，可以在后端自动处理或调用专门的已读API
}

/**
 * 导入并重新执行分享的查询
 * 
 * <p>功能说明：
 * <ul>
 *   <li>将分享的查询重新导入到查询页面执行</li>
 *   <li>使用分享时的用户提示（userPrompt）作为查询内容</li>
 *   <li>传递数据库连接ID，确保使用正确的数据库连接</li>
 *   <li>触发切换到查询页面并执行查询</li>
 * </ul>
 * 
 * @param share 分享记录对象，包含querySnapshot等信息
 */
const handleRerunShare = (share) => {
  const dbConnectionId = share.querySnapshot?.dbConnectionId
  console.log('[FriendsPage] 导入并重新执行 - share:', share)
  console.log('[FriendsPage] 导入并重新执行 - dbConnectionId:', dbConnectionId)
  console.log('[FriendsPage] 导入并重新执行 - userPrompt:', share.querySnapshot?.userPrompt)
  
  // 确保dbConnectionId是有效的数字
  const validDbConnectionId = dbConnectionId && dbConnectionId > 0 ? Number(dbConnectionId) : undefined
  emit('rerun-query', share.querySnapshot.userPrompt, validDbConnectionId)
}

// 切换已读/未读状态
const handleToggleReadStatus = async (share) => {
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const newStatus = share.isRead ? 0 : 1 // 切换状态：已读->未读(0)，未读->已读(1)
    
    if (newStatus === 1) {
      // 标记为已读（保存）
      const result = await queryShareChatApi.save(share.id, userId)
      if (result.success) {
        share.isRead = true
        share.receiveStatus = 1
      } else {
        alert(result.message || '操作失败')
      }
    } else {
      // 标记为未读（需要后端支持，暂时只更新前端状态）
      share.isRead = false
      share.receiveStatus = 0
    }
    
    // 重新加载分享列表以更新状态
    await loadShares()
  } catch (error) {
    console.error('切换已读状态失败:', error)
    alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 备注功能相关函数
const handleOpenRemarkModal = (friend) => {
  currentRemarkFriend.value = friend
  isRemarkModalOpen.value = true
}

const handleSaveRemark = async () => {
  if (currentRemarkFriend.value) {
    try {
      const userId = Number(sessionStorage.getItem('userId') || '1')
      await friendRelationApi.update({
        id: currentRemarkFriend.value.relationId,
        userId: userId,  // 添加当前用户ID以通过权限检查
        remarkName: remarkInput.value.trim(),  // 使用后端实际字段名
      })

      friends.value = friends.value.map((friend) =>
        friend.id === currentRemarkFriend.value.id
          ? { ...friend, remark: remarkInput.value.trim() }
          : friend,
      )
      closeRemarkModal()
      alert('备注已保存')
    } catch (error) {
      console.error('保存备注失败:', error)
      alert('保存备注失败')
    }
  }
}

const closeRemarkModal = () => {
  isRemarkModalOpen.value = false
  currentRemarkFriend.value = null
  remarkInput.value = ''
}

// 好友主页功能相关函数
const handleOpenProfileModal = (friend) => {
  currentProfileFriend.value = friend
  isProfileModalOpen.value = true
}

const closeProfileModal = () => {
  isProfileModalOpen.value = false
  currentProfileFriend.value = null
}

const handleOpenChatFromProfile = () => {
  if (currentProfileFriend.value) {
    closeProfileModal()
    // 延迟打开聊天，确保主页模态框已关闭
    setTimeout(() => {
      handleOpenChat(currentProfileFriend.value)
    }, 100)
  }
}

const handleOpenRemarkFromProfile = () => {
  if (currentProfileFriend.value) {
    closeProfileModal()
    // 延迟打开备注模态框，确保主页模态框已关闭
    setTimeout(() => {
      handleOpenRemarkModal(currentProfileFriend.value)
    }, 100)
  }
}


// 处理分享删除请求
const handleDeleteShareRequest = (share) => {
  shareToDelete.value = share
}

/**
 * 确认删除分享（用于"分享记录"标签页）
 * 
 * <p>删除收到的分享记录
 */
const handleConfirmDeleteShare = async () => {
  if (shareToDelete.value) {
    try {
      const userId = Number(sessionStorage.getItem('userId') || '1')
      // 使用新的 API 删除分享
      const result = await queryShareChatApi.delete(shareToDelete.value.id, userId)
      
      if (result.success) {
        await loadShares() // 重新加载分享列表
        shareToDelete.value = null
      } else {
        alert(result.message || '删除失败')
      }
    } catch (error) {
      console.error('删除分享失败:', error)
      alert('删除分享失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
}

/**
 * 删除我分享的查询记录（用于"我的分享"标签页）
 * 
 * <p>功能说明：
 * <ul>
 *   <li>删除当前用户分享给好友的查询记录</li>
 *   <li>删除后重新加载"我的分享"列表</li>
 *   <li>提示用户确认删除操作</li>
 * </ul>
 * 
 * @param share 要删除的分享记录对象
 */
const handleDeleteSharedQuery = async (share) => {
  if (!share || !share.id) {
    alert('无效的分享记录')
    return
  }

  // 确认删除
  if (!confirm(`确定要删除分享给 "${share.receiver.name}" 的查询记录吗？此操作无法撤销。`)) {
    return
  }

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    // 注意：当前的 delete API 是用于接收者删除收到的分享
    // 对于分享者删除自己分享出去的记录，可能需要后端支持专门的方法
    // 这里暂时使用现有的 delete API，如果后端不支持，会返回错误
    const result = await queryShareChatApi.delete(share.id, userId)
    
    if (result.success) {
      // 重新加载我的分享列表
      await loadSharedQueries()
      alert('删除成功')
    } else {
      alert(result.message || '删除失败')
    }
  } catch (error) {
    console.error('删除我分享的查询失败:', error)
    alert('删除失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 打开分享给好友模态框
const handleOpenShareToFriendModal = async (friend) => {
  currentShareFriend.value = friend
  selectedQueriesForShare.value = []
  
  // 从收藏夹加载查询记录（按收藏夹加载，与收藏夹页面保持一致）
  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    
    // 先加载所有收藏夹
    const collections = await queryCollectionApi.getByUser(userId)
    console.log('[FriendsPage] 分享弹窗 - 加载收藏夹，数量:', collections.length)
    
    // 为每个收藏夹加载记录（与收藏夹页面的逻辑一致）
    const allRecords: any[] = []
    for (const collection of collections) {
      try {
        const collectionId = String(collection.id)
        const records = await collectionRecordApi.getByCollection(collectionId)
        console.log('[FriendsPage] 分享弹窗 - 收藏夹:', collection.collectionName, '记录数:', records.length)
        
        // 过滤掉无效的记录（与收藏夹页面保持一致）
        const validRecords = (records || []).filter(record => 
          record && record.id && (record.queryLogId || record.userPrompt)
        )
        
        allRecords.push(...validRecords)
      } catch (err) {
        console.error('[FriendsPage] 分享弹窗 - 加载收藏夹记录失败:', err, 'collectionId:', collection.id)
      }
    }
    
    // 将 CollectionRecord 转换为 QueryResultData 格式（使用统一的转换函数）
    shareToFriendQueries.value = allRecords.map(recordToQueryResult).filter((q): q is QueryResultData => q !== null)
    
    console.log('[FriendsPage] 分享弹窗 - 加载查询记录成功，总数量:', shareToFriendQueries.value.length)
    isShareToFriendModalOpen.value = true
  } catch (error) {
    console.error('[FriendsPage] 分享弹窗 - 从收藏夹加载查询记录失败:', error)
    alert('加载收藏夹查询记录失败')
  }
}

// 确认分享给好友（使用统一服务）
// ⚠️ 注意：后端API不完整，前端跳过检查
// 后端 QueryShare 实体类缺少 queryLogId 字段，可能导致分享失败
// 当前添加容错处理，避免因后端问题导致前端崩溃
// 确认分享给好友（支持批量分享）
const handleConfirmShareToFriend = async () => {
  if (!currentShareFriend.value || selectedQueriesForShare.value.length === 0) {
    alert('请至少选择一个要分享的查询记录')
    return
  }

  try {
    const friendId = Number(currentShareFriend.value.friendId || currentShareFriend.value.id)
    const userId = Number(sessionStorage.getItem('userId') || '1')
    
    // 批量分享
    const sharePromises = selectedQueriesForShare.value.map(async (queryId) => {
      const queryToShare = shareToFriendQueries.value.find(q => q.id === queryId)
      if (!queryToShare) {
        throw new Error(`查询记录 ${queryId} 不存在`)
      }

      // 构建分享数据
      // 确保 dbConnectionId 和 llmConfigId 是有效的数字（大于0）
      const validDbConnectionId = queryToShare.dbConnectionId && Number(queryToShare.dbConnectionId) > 0 
        ? Number(queryToShare.dbConnectionId) 
        : undefined
      const validLlmConfigId = queryToShare.llmConfigId && Number(queryToShare.llmConfigId) > 0 
        ? Number(queryToShare.llmConfigId) 
        : undefined
      
      // 如果没有有效的 dbConnectionId，跳过该查询
      if (!validDbConnectionId) {
        console.warn('[FriendsPage] 查询缺少有效的数据库连接ID，跳过分享:', queryToShare)
        throw new Error(`查询 "${queryToShare.userPrompt}" 缺少数据库连接信息，无法分享`)
      }
      
      const shareData = {
        shareUserId: userId,
        receiveUserId: friendId,
        queryTitle: queryToShare.userPrompt,
        sqlQuery: queryToShare.sqlQuery,
        databaseName: queryToShare.database || '',
        dbConnectionId: validDbConnectionId,
        llmName: queryToShare.model || '',
        llmConfigId: validLlmConfigId,
        executionTime: queryToShare.executionTime ? parseInt(queryToShare.executionTime) : 0,
        executionTimeText: queryToShare.executionTime || '0ms',
        queryTime: queryToShare.queryTime || new Date().toISOString(),
        dialogId: queryToShare.conversationId || '',
        tableData: queryToShare.tableData,
        chartData: queryToShare.chartData,
        shareMessage: '', // 可以后续添加分享留言功能
      }

      return await queryShareChatApi.share(shareData)
    })

    await Promise.all(sharePromises)
    
    alert(`成功分享 ${selectedQueriesForShare.value.length} 个查询`)
    closeShareToFriendModal()
    await loadShares() // 重新加载收到的分享列表
    await loadSharedQueries() // 重新加载我分享的列表
  } catch (error) {
    console.error('分享失败:', error)
    alert('分享失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

// 关闭分享给好友模态框
const closeShareToFriendModal = () => {
  isShareToFriendModalOpen.value = false
  currentShareFriend.value = null
  selectedQueriesForShare.value = []
}

// 已移除通知功能，改为已读/未读功能

// 关闭通知模态框
const closeNotificationModal = () => {
  isNotificationModalOpen.value = false
  currentNotificationShare.value = null
}

// 其他工具函数
const closeAddFriendModal = () => {
  isAddFriendModalOpen.value = false
  searchKey.value = ''
  searchResult.value = null
  searchError.value = ''
  searchType.value = 'id'
  isSearching.value = false
}

const handleImageError = (e) => {
  e.target.src = '/default-avatar.png'
}

const formatDate = (timestamp) => {
  return new Date(timestamp).toLocaleString()
}

// 加载状态
if (loading.value) {
  // 返回加载状态
  const loadingElement = document.createElement('div')
  loadingElement.innerHTML = `
    <section class="p-6 flex justify-center items-center h-full">
      <div class="text-gray-500">加载中...</div>
    </section>
  `
}
</script>
