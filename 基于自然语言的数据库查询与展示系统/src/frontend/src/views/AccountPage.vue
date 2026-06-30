<!--
  @file views/AccountPage.vue
  @description 账户管理页面

  功能：
  - 查看/编辑个人信息
  - 修改密码
  - 账户设置

  @author Frontend Team
-->
<template>
  <section
    v-if="loading"
    class="p-6 overflow-y-auto bg-neutral flex justify-center items-center h-full"
  >
    <div class="text-gray-500">加载中...</div>
  </section>

  <section
    v-else-if="!user"
    class="p-6 overflow-y-auto bg-neutral flex justify-center items-center h-full"
  >
    <div class="text-gray-500">加载用户信息失败</div>
  </section>

  <section v-else class="p-6 overflow-y-auto bg-neutral">
    <div class="max-w-4xl mx-auto space-y-6">
      <!-- Personal Info Card -->
      <div class="bg-white p-8 rounded-xl shadow-sm border">
        <div class="flex flex-col md:flex-row items-start gap-8">
          <!-- Left side: Avatar -->
          <div class="flex flex-col items-center flex-shrink-0 w-full md:w-48">
            <img
              :src="avatarPreview || user?.avatarUrl || '/default-avatar.png'"
              alt="User Avatar"
              class="w-40 h-40 rounded-full object-cover mb-4 ring-4 ring-white shadow-lg"
            />
            <input
              type="file"
              ref="fileInputRef"
              @change="handleAvatarChange"
              accept="image/*"
              class="hidden"
            />
            <button
              @click="triggerFileInput"
              class="px-4 py-2 border rounded-lg text-sm w-full hover:bg-gray-50 transition-colors"
            >
              <i class="fa fa-upload mr-2"></i> 更换头像
            </button>
          </div>

          <!-- Right side: Info -->
          <div class="flex-1 w-full">
            <div class="border-b pb-4 mb-6">
              <h3 class="text-xl font-bold text-gray-900">个人基本信息</h3>
            </div>
            <dl class="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-6">
              <div>
                <dt class="text-sm font-medium text-gray-500">用户名</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ user?.name || '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">用户ID</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ user?.userId || '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">邮箱</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ user?.email || '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">手机号码</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ user?.phoneNumber ? maskPhoneNumber(user.phoneNumber) : '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">注册时间</dt>
                <dd class="mt-1 text-sm text-gray-900">{{ user?.registrationDate || '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500">账户状态</dt>
                <dd class="mt-1 text-sm text-gray-900">
                  <span v-if="user?.accountStatus" :class="[
                    'px-2 py-1 text-xs font-medium rounded-full',
                    user.accountStatus === 'normal' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  ]">
                    {{ user.accountStatus === 'normal' ? '正常' : '禁用' }}
                  </span>
                  <span v-else>未设置</span>
                </dd>
              </div>
            </dl>
            <div class="mt-8 flex justify-end">
              <button
                @click="handleOpenEditModal"
                class="px-6 py-2.5 bg-primary text-white rounded-lg font-semibold hover:shadow-lg transition-all duration-200"
              >
                <i class="fa fa-pencil mr-2"></i> 编辑信息
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Security Settings Card -->
      <div class="bg-white p-8 rounded-xl shadow-sm border">
        <h3 class="text-xl font-bold text-gray-900 border-b pb-4 mb-6">安全设置</h3>
        <div class="space-y-6">
          <div class="flex justify-between items-center">
            <div>
              <p class="font-medium text-gray-800">修改密码</p>
              <p class="text-sm text-gray-500 mt-1">上次修改时间：2025-05-10</p>
            </div>
            <button
              @click="isPasswordModalOpen = true"
              class="px-4 py-2 border rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors"
            >
              立即修改
            </button>
          </div>
          <div class="flex justify-between items-center">
            <div>
              <p class="font-medium text-gray-800">绑定手机</p>
              <p class="text-sm text-gray-500 mt-1">
                已绑定：{{ user.phoneNumber ? maskPhoneNumber(user.phoneNumber) : '未绑定' }}
              </p>
            </div>
            <button
              @click="isPhoneModalOpen = true"
              class="px-4 py-2 border rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors"
            >
              更换绑定
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Profile Modal -->
    <Modal :is-open="isEditModalOpen" @close="isEditModalOpen = false" title="编辑个人信息">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
          <input
            ref="editNameInput"
            type="text"
            v-model="editForm.name"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-primary/30 focus:border-primary"
          />
        </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">邮箱</label>
            <input
              ref="editEmailInput"
              type="email"
              v-model="editForm.email"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-primary/30 focus:border-primary"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">手机号码</label>
            <input
              ref="editPhoneInput"
              type="tel"
              v-model="editForm.phoneNumber"
              pattern="[0-9]{11}"
              title="请输入 11 位数字手机号码（可选）"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-primary/30 focus:border-primary"
            />
          </div>
      </div>
      <div class="mt-6 flex justify-end space-x-3">
        <button
          type="button"
          @click="handleCancelEdit"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-100 transition-colors"
        >
          取消
        </button>
        <button
          type="button"
          @click="handleSaveProfile"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:shadow-md transition-shadow"
        >
          确定
        </button>
      </div>
    </Modal>

    <!-- Password Change Modal -->
    <Modal :is-open="isPasswordModalOpen" @close="closePasswordModal" title="修改密码">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">当前密码</label>
          <input
            type="password"
            placeholder="请输入当前密码"
            v-model="passwordForm.oldPassword"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">新密码</label>
          <input
            ref="newPasswordInput"
            type="password"
            placeholder="请输入新密码"
            v-model="passwordForm.newPassword"
            minlength="6"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">确认新密码</label>
          <input
            ref="confirmPasswordInput"
            type="password"
            placeholder="请再次输入新密码"
            v-model="passwordForm.confirmPassword"
            minlength="6"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg"
          />
        </div>
      </div>
      <div class="mt-6 flex justify-end space-x-3">
        <button
          type="button"
          @click="closePasswordModal"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-100 transition-colors"
        >
          取消
        </button>
        <button
          type="button"
          @click="handleChangePassword"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:shadow-md transition-shadow"
        >
          确定
        </button>
      </div>
    </Modal>

    <!-- Phone Change Modal -->
    <Modal :is-open="isPhoneModalOpen" @close="closePhoneModal" title="更换绑定手机">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">新手机号码</label>
          <input
            ref="newPhoneInput"
            type="tel"
            v-model="phoneForm.newPhone"
            placeholder="请输入新手机号"
            pattern="[0-9]{11}"
            title="请输入 11 位数字手机号码"
            required
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-primary/30 focus:border-primary"
          />
        </div>
      </div>
      <div class="mt-6 flex justify-end space-x-3">
        <button
          type="button"
          @click="closePhoneModal"
          class="px-4 py-2 border border-gray-300 rounded-lg text-sm hover:bg-gray-100 transition-colors"
        >
          取消
        </button>
        <button
          type="button"
          @click="handleChangePhone"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm hover:shadow-md transition-shadow"
        >
          确认更换
        </button>
      </div>
    </Modal>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Modal from '../components/ui/Modal.vue'
import type { UserProfile } from '../types'
import { userApi, type ChangePasswordRequest } from '../services/api.real'

// InfoField 组件已移除，直接使用HTML模板

// 响应式数据
const user = ref<UserProfile | null>(null)
const loading = ref(true)
const isEditModalOpen = ref(false)
const isPasswordModalOpen = ref(false)
const isPhoneModalOpen = ref(false)
const editForm = ref({
  name: '',
  email: '',
  phoneNumber: '',
})
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const phoneForm = ref({
  newPhone: '',
})
const avatarPreview = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const editNameInput = ref<HTMLInputElement | null>(null)
const editEmailInput = ref<HTMLInputElement | null>(null)
const editPhoneInput = ref<HTMLInputElement | null>(null)
const newPasswordInput = ref<HTMLInputElement | null>(null)
const confirmPasswordInput = ref<HTMLInputElement | null>(null)
const newPhoneInput = ref<HTMLInputElement | null>(null)

// 工具函数
const maskPhoneNumber = (phone: string) => {
  if (phone.length === 11) {
    return `${phone.substring(0, 3)}****${phone.substring(7)}`
  }
  return phone
}

// 生命周期钩子
onMounted(() => {
  loadUserProfile()
})

// 方法
const loadUserProfile = async () => {
  try {
    loading.value = true
    user.value = null // 重置用户数据
    
    const userId = Number(sessionStorage.getItem('userId') || '1')
    if (!userId || isNaN(userId)) {
      throw new Error('用户ID无效')
    }
    
    const userData = await userApi.getById(userId)

    if (!userData) {
      throw new Error('用户数据为空')
    }

    const profile: UserProfile = {
      id: String(userData.id),
      userId: String(userData.id),
      name: userData.username || '未设置',
      email: userData.email || '未设置',
      phoneNumber: userData.phonenumber || '',
      avatarUrl: userData.avatarUrl || '/default-avatar.png',
      registrationDate: (userData as any).createTime?.split('T')[0] || '未知',
      accountStatus: userData.status === 1 ? 'normal' : 'disabled',
      preferences: {
        defaultModel: '',
        defaultDatabase: '',
      },
    }

    user.value = profile
    avatarPreview.value = profile.avatarUrl
  } catch (error) {
    console.error('加载用户信息失败:', error)
    const errorMessage = error instanceof Error ? error.message : '加载用户信息失败，请刷新页面重试'
    console.error('错误详情:', error)
    // 不设置user.value为null，允许显示部分信息或空状态
    // 只在真正无法加载时才显示错误提示
    if (!user.value) {
      user.value = null
    }
  } finally {
    loading.value = false
  }
}

const handleOpenEditModal = () => {
  if (user.value) {
    editForm.value = {
      name: user.value.name || '',
      email: user.value.email || '',
      phoneNumber: user.value.phoneNumber || '',
    }
    isEditModalOpen.value = true
  }
}

const handleCancelEdit = () => {
  isEditModalOpen.value = false
}

const handleSaveProfile = async () => {
  // 验证表单
  if (editNameInput.value && !editNameInput.value.checkValidity()) {
    editNameInput.value.reportValidity()
    return
  }
  if (editEmailInput.value && !editEmailInput.value.checkValidity()) {
    editEmailInput.value.reportValidity()
    return
  }
  if (editPhoneInput.value && !editPhoneInput.value.checkValidity()) {
    editPhoneInput.value.reportValidity()
    return
  }

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    await userApi.update({
      id: userId,
      username: editForm.value.name,
      email: editForm.value.email,
      phonenumber: editForm.value.phoneNumber,
      avatarUrl: avatarPreview.value,
    })
    await loadUserProfile() // 重新加载用户信息
    isEditModalOpen.value = false
    alert('个人信息更新成功！')
  } catch (error) {
    console.error('更新个人信息失败:', error)
    alert('更新失败，请重试')
    // 发生错误时不关闭模态框，让用户有机会修正
  }
}

const triggerFileInput = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (file) {
    const reader = new FileReader()
    reader.onloadend = async () => {
      const base64Avatar = reader.result as string
      avatarPreview.value = base64Avatar

      // 立即保存到后端
      try {
        const userId = Number(sessionStorage.getItem('userId') || '1')
        await userApi.update({
          id: userId,
          avatarUrl: base64Avatar,
        })
        await loadUserProfile()
      } catch (error) {
        console.error('更新头像失败:', error)
        alert('更新头像失败')
      }
    }
    reader.readAsDataURL(file)
  }
}

const handleChangePassword = async () => {
  // 验证表单
  if (newPasswordInput.value && !newPasswordInput.value.checkValidity()) {
    newPasswordInput.value.reportValidity()
    return
  }
  if (confirmPasswordInput.value && !confirmPasswordInput.value.checkValidity()) {
    confirmPasswordInput.value.reportValidity()
    return
  }

  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert('两次输入的新密码不一致！')
    return
  }
  if (!passwordForm.value.oldPassword) {
    alert('请输入当前密码！')
    return
  }

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    const changePasswordRequest: ChangePasswordRequest = {
      userId: userId,
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    }

    await userApi.changePassword(changePasswordRequest)
    alert('密码修改成功！')
    closePasswordModal()
  } catch (error) {
    console.error('修改密码失败:', error)
    alert(error instanceof Error ? error.message : '修改失败，请检查当前密码是否正确')
    // 发生错误时不关闭模态框，让用户有机会修正
  }
}

const closePasswordModal = () => {
  isPasswordModalOpen.value = false
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
}

const handleChangePhone = async () => {
  // 验证手机号格式
  if (newPhoneInput.value && !newPhoneInput.value.checkValidity()) {
    newPhoneInput.value.reportValidity()
    return
  }

  if (!phoneForm.value.newPhone) {
    alert('请输入新手机号码')
    return
  }

  const phonePattern = /^1[3-9]\d{9}$/
  if (!phonePattern.test(phoneForm.value.newPhone)) {
    alert('请输入有效的 11 位手机号码')
    return
  }

  try {
    const userId = Number(sessionStorage.getItem('userId') || '1')
    await userApi.update({
      id: userId,
      phonenumber: phoneForm.value.newPhone,
    })
    alert('手机号更换成功')
    await loadUserProfile()
    closePhoneModal()
  } catch (error) {
    console.error('更换手机号失败:', error)
    alert(error instanceof Error ? error.message : '更换失败，请稍后重试')
  }
}

const closePhoneModal = () => {
  isPhoneModalOpen.value = false
  phoneForm.value = { newPhone: '' }
}
</script>

<style scoped>
/* 如果有需要添加的组件特定样式 */
</style>
