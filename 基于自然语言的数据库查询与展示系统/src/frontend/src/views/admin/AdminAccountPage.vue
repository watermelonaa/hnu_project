<!--
  @file views/admin/AdminAccountPage.vue
  @description 管理员账户页面

  功能：
  - 查看/编辑个人信息
  - 修改密码
  - 账户安全设置

  @author Frontend Team
-->
<template>
  <main class="flex-1 overflow-y-auto p-6 admin-account-main">

    <!-- 加载状态 -->
    <div v-if="loading" class="flex justify-center items-center h-64">
      <div class="text-gray-500">加载中...</div>
    </div>

    <!-- 内容区域 -->
    <div v-else>
      <!-- 基本信息卡片 -->
      <div class="bg-white rounded-xl p-6 shadow-sm">
        <div class="flex flex-col md:flex-row gap-6">
          <div class="flex flex-col items-center">
            <img :src="user.avatar" alt="Avatar" class="w-24 h-24 rounded-full object-cover mb-4" />
            <input
              type="file"
              ref="avatarInputRef"
              @change="handleAvatarChange"
              class="hidden"
              accept="image/*"
            />
            <button @click="triggerAvatarInput" class="px-4 py-2 border rounded-lg text-sm">
              <i class="fa fa-upload mr-1"></i> 更换头像
            </button>
          </div>
          <div class="flex-1">
            <h3 class="font-bold text-lg mb-4">个人基本信息</h3>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <p class="text-sm text-gray-500">用户名</p>
                <p>{{ user.name }}</p>
              </div>
              <div>
                <p class="text-sm text-gray-500">用户ID</p>
                <p>{{ user.id }}</p>
              </div>
              <div>
                <p class="text-sm text-gray-500">邮箱</p>
                <p>{{ user.email || '未设置' }}</p>
              </div>
              <div>
                <p class="text-sm text-gray-500">手机号码</p>
                <p>{{ user.phone || '未设置' }}</p>
              </div>
            </div>
            <div class="mt-6 flex justify-end">
              <button
                @click="handleOpenEditModal"
                class="px-4 py-2 bg-primary text-white rounded-lg text-sm"
              >
                <i class="fa fa-edit mr-1"></i> 编辑信息
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 安全设置卡片 -->
      <div class="bg-white rounded-xl p-6 shadow-sm mt-6">
        <h3 class="font-bold text-lg mb-6">安全设置</h3>
        <div class="space-y-6">
          <div class="flex justify-between items-center border-b pb-4">
            <div>
              <h4 class="font-medium">修改密码</h4>
              <p class="text-sm text-gray-500 mt-1">建议定期修改密码</p>
            </div>
            <button @click="openModal('password')" class="px-4 py-2 border rounded-lg text-sm">
              立即修改
            </button>
          </div>
          <div class="flex justify-between items-center">
            <div>
              <h4 class="font-medium">两步验证 (2FA)</h4>
              <p class="text-sm text-gray-500 mt-1">为账户增加额外保护</p>
            </div>
            <button @click="openModal('2fa')" class="px-4 py-2 border rounded-lg text-sm">
              设置
            </button>
          </div>
        </div>
      </div>

      <!-- 模态框 -->
      <AdminModal :is-open="modal === 'edit'" title="编辑信息" @close="closeModal">
        <div class="space-y-4">
          <div>
            <label class="block text-sm mb-1">用户名</label>
            <input 
              ref="editUsernameInput"
              v-model="editForm.username" 
              required
              class="w-full px-3 py-2 border rounded-lg" 
            />
          </div>
          <div>
            <label class="block text-sm mb-1">邮箱</label>
            <input
              ref="editEmailInput"
              v-model="editForm.email"
              type="email"
              class="w-full px-3 py-2 border rounded-lg"
            />
          </div>
          <div>
            <label class="block text-sm mb-1">手机号</label>
            <input
              ref="editPhoneInput"
              v-model="editForm.phonenumber"
              type="tel"
              pattern="[0-9]{11}"
              title="请输入 11 位数字手机号码（可选）"
              class="w-full px-3 py-2 border rounded-lg"
            />
          </div>
        </div>
        <template #footer>
          <div class="flex justify-end space-x-2">
            <button 
              type="button"
              @click="closeModal" 
              class="px-4 py-2 border rounded-lg"
            >
              取消
            </button>
            <button 
              type="button"
              @click="handleSaveProfile" 
              class="px-4 py-2 bg-primary text-white rounded-lg"
            >
              确定
            </button>
          </div>
        </template>
      </AdminModal>

      <AdminModal :is-open="modal === 'password'" title="修改密码" @close="closeModal">
        <div class="space-y-4">
          <div>
            <label class="block text-sm mb-1">当前密码</label>
            <input
              v-model="passwordForm.oldPassword"
              type="password"
              class="w-full px-3 py-2 border rounded-lg"
            />
          </div>
          <div>
            <label class="block text-sm mb-1">新密码</label>
            <input
              ref="newPasswordInput"
              v-model="passwordForm.newPassword"
              type="password"
              minlength="6"
              required
              class="w-full px-3 py-2 border rounded-lg"
            />
          </div>
          <div>
            <label class="block text-sm mb-1">确认新密码</label>
            <input
              ref="confirmPasswordInput"
              v-model="passwordForm.confirmPassword"
              type="password"
              minlength="6"
              required
              class="w-full px-3 py-2 border rounded-lg"
            />
          </div>
        </div>
        <template #footer>
          <div class="flex justify-end space-x-2">
            <button 
              type="button"
              @click="closeModal" 
              class="px-4 py-2 border rounded-lg"
            >
              取消
            </button>
            <button
              type="button"
              @click="handleChangePassword"
              class="px-4 py-2 bg-primary text-white rounded-lg"
            >
              确定
            </button>
          </div>
        </template>
      </AdminModal>

      <AdminModal :is-open="modal === '2fa'" title="设置两步验证" @close="closeModal">
        <div class="text-center p-4">
          <div class="w-32 h-32 bg-gray-200 mx-auto mb-4 flex items-center justify-center">
            <span class="text-sm text-gray-500">二维码</span>
          </div>
          <p class="text-sm mb-4">请使用您的验证器应用扫描二维码。</p>
          <input
            placeholder="输入6位验证码"
            class="w-full px-3 py-2 border rounded-lg text-center"
          />
        </div>
        <template #footer>
          <div class="flex justify-end space-x-2">
            <button 
              type="button"
              @click="closeModal" 
              class="px-4 py-2 border rounded-lg"
            >
              取消
            </button>
            <button 
              type="button"
              @click="closeModal" 
              class="px-4 py-2 bg-primary text-white rounded-lg"
            >
              启用
            </button>
          </div>
        </template>
      </AdminModal>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import { userApi, type ChangePasswordRequest } from '../../services/api.real'

// 用户ID
const userId = Number(sessionStorage.getItem('userId') || '1')

// 响应式数据
const loading = ref(true)
const user = ref({
  name: '',
  id: '',
  email: '',
  phone: '',
  avatar: '',
})

const modal = ref<'edit' | 'password' | '2fa' | null>(null)
const editForm = ref({
  username: '',
  email: '',
  phonenumber: '',
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const avatarInputRef = ref<HTMLInputElement | null>(null)
const editUsernameInput = ref<HTMLInputElement | null>(null)
const editEmailInput = ref<HTMLInputElement | null>(null)
const editPhoneInput = ref<HTMLInputElement | null>(null)
const newPasswordInput = ref<HTMLInputElement | null>(null)
const confirmPasswordInput = ref<HTMLInputElement | null>(null)

// 生命周期钩子
onMounted(() => {
  loadUserProfile()
})

// 方法
const loadUserProfile = async () => {
  try {
    loading.value = true
    const userData = await userApi.getById(userId)
    user.value = {
      name: userData.username || '',
      id: `#${userData.id}`,
      email: userData.email || '',
      phone: userData.phonenumber ? maskPhoneNumber(userData.phonenumber) : '未设置',
      avatar: userData.avatarUrl || '/default-avatar.png',
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const maskPhoneNumber = (phone: string) => {
  if (phone && phone.length === 11) {
    return `${phone.substring(0, 3)}****${phone.substring(7)}`
  }
  return phone
}

const triggerAvatarInput = () => {
  avatarInputRef.value?.click()
}

const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (file) {
    const reader = new FileReader()

    reader.onload = async (event) => {
      const newAvatar = event.target?.result as string
      try {
        await userApi.update({
          id: userId,
          avatarUrl: newAvatar,
        })
        user.value.avatar = newAvatar

        // 触发全局刷新事件
        window.dispatchEvent(
          new CustomEvent('userAvatarUpdated', {
            detail: { avatarUrl: newAvatar },
          }),
        )

        alert('头像更新成功！')
      } catch (error) {
        console.error('更新头像失败:', error)
        alert('更新头像失败，请重试')
      }
    }

    reader.readAsDataURL(file)
  }
}

const handleOpenEditModal = () => {
  editForm.value = {
    username: user.value.name,
    email: user.value.email,
    phonenumber: user.value.phone && user.value.phone !== '未设置' ? user.value.phone.replace(/\*/g, '') : '',
  }
  modal.value = 'edit'
}

const handleSaveProfile = async () => {
  // 验证表单
  if (editUsernameInput.value && !editUsernameInput.value.checkValidity()) {
    editUsernameInput.value.reportValidity()
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
    await userApi.update({
      id: userId,
      username: editForm.value.username,
      email: editForm.value.email,
      phonenumber: editForm.value.phonenumber,
      avatarUrl: user.value.avatar,
    })
    await loadUserProfile()
    closeModal()
    alert('个人信息更新成功！')
  } catch (error) {
    console.error('更新个人信息失败:', error)
    alert('更新失败，请重试')
    // 发生错误时不关闭模态框，让用户有机会修正
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
    const changePasswordRequest: ChangePasswordRequest = {
      userId: userId,
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    }
    await userApi.changePassword(changePasswordRequest)
    alert('密码修改成功！')
    closeModal()
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    console.error('修改密码失败:', error)
    alert(error instanceof Error ? error.message : '修改失败，请检查当前密码是否正确')
    // 发生错误时不关闭模态框，让用户有机会修正
  }
}

const openModal = (type: 'edit' | 'password' | '2fa') => {
  modal.value = type
}

const closeModal = () => {
  modal.value = null
}
</script>

<style scoped>
/* 确保管理员账户页面的间距正确显示，避免被全局样式覆盖 */
.admin-account-main {
  display: flex;
  flex-direction: column;
}

.admin-account-main > * + * {
  margin-top: 1.5rem !important; /* space-y-6 = 24px = 1.5rem */
}

.admin-account-main > div:first-child {
  margin-top: 0 !important;
}
</style>
