<!--
  @file views/LoginPage.vue
  @description 用户登录页面

  功能：
  - 用户名密码登录
  - 登录状态验证
  - 忘记密码弹窗

  @author Frontend Team
-->
<template>
  <div class="font-inter animated-gradient min-h-screen flex items-center justify-center p-4">
    <div
      class="w-full max-w-5xl bg-white rounded-2xl shadow-2xl overflow-hidden grid lg:grid-cols-2"
    >
      <div class="p-8 md:p-12 flex flex-col justify-center">
        <div>
          <div class="flex items-center space-x-3 mb-6">
            <h1 class="text-4xl font-bold text-dark">自然语言查询系统</h1>
          </div>
          <h2 class="text-3xl font-bold text-dark mt-6">欢迎回来</h2>
          <p class="text-gray-500 mt-2">请输入您的账号和密码登录。</p>
        </div>

        <form @submit.prevent="handleLogin" class="mt-8">
          <div class="mb-4">
            <div class="relative">
              <span class="absolute left-3.5 top-3.5 text-gray-400"
                ><i class="fa fa-user"></i
              ></span>
              <input
                type="text"
                placeholder="请输入账号"
                v-model="username"
                class="w-full px-4 py-3 pl-10 bg-gray-50 text-dark border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/50 focus:border-primary placeholder-gray-400"
              />
            </div>
          </div>
          <div class="mb-6">
            <div class="relative">
              <span class="absolute left-3.5 top-3.5 text-gray-400"
                ><i class="fa fa-lock"></i
              ></span>
              <input
                type="password"
                placeholder="请输入密码"
                v-model="password"
                @keypress.enter="handleLogin"
                class="w-full px-4 py-3 pl-10 bg-gray-50 text-dark border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/50 focus:border-primary placeholder-gray-400"
              />
            </div>
          </div>

          <div
            v-if="error"
            class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm"
          >
            {{ error }}
          </div>

          <div class="flex flex-col space-y-4">
            <button
              type="submit"
              class="bg-primary text-white py-3 rounded-lg font-bold hover:shadow-lg hover:shadow-primary/30 hover:-translate-y-0.5 transition-all duration-200 flex items-center justify-center disabled:bg-primary/50 disabled:cursor-wait"
              :disabled="isLoading"
            >
              <i v-if="isLoading" class="fa fa-spinner fa-spin"></i>
              <span v-else>安全登录</span>
            </button>
            <div class="text-center text-sm text-gray-500">
              <a
                href="#"
                class="hover:text-primary transition-colors"
                @click.prevent="isForgotModalOpen = true"
                >忘记密码？</a
              >
            </div>
          </div>
        </form>

        <div class="text-center text-xs text-gray-400 mt-12">
          © 2024 Your Company. All Rights Reserved.
        </div>
      </div>

      <LoginSidebar />
    </div>

    <ForgotPasswordModal v-if="isForgotModalOpen" @close="isForgotModalOpen = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { UserRole } from '../types'
import { authApi, type LoginRequest } from '../services/api.real'
import { logOperation, LogModule, LogOperationType, LogStatus } from '../utils/logger'
import LoginSidebar from '../components/layout/sidebars/LoginSidebar.vue'
import ForgotPasswordModal from '../components/feature/ForgotPasswordModal.vue'

// 1. Emits
const emit = defineEmits<{
  (e: 'loginSuccess', role: UserRole): void
}>()

// 2. State
const isForgotModalOpen = ref(false)
const isLoading = ref(false)
const username = ref('')
const password = ref('')
const error = ref<string | null>(null)

// 3. Methods
const handleLogin = async () => {
  error.value = null

  if (!username.value.trim() || !password.value.trim()) {
    error.value = '请输入用户名和密码'
    return
  }

  isLoading.value = true
  try {
    const loginRequest: LoginRequest = {
      username: username.value.trim(),
      password: password.value,
    }

    const response = await authApi.login(loginRequest)

    let role: UserRole = 'normal-user'
    if (response.roleId === 1) {
      role = 'sys-admin'
    } else if (response.roleId === 2) {
      role = 'data-admin'
    } else if (response.roleId === 3) {
      role = 'normal-user'
    } else {
      throw new Error('未知的用户角色')
    }

    sessionStorage.setItem('userRole', role)

    await logOperation(
      LogModule.SYSTEM,
      LogOperationType.LOGIN,
      `用户 ${response.username} 登录系统`,
      LogStatus.SUCCESS,
    )

    emit('loginSuccess', role)
    window.location.reload()
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : '登录失败，请检查用户名和密码'
    error.value = errorMessage

    await logOperation(
      LogModule.SYSTEM,
      LogOperationType.LOGIN,
      `用户 ${username.value} 登录失败：${errorMessage}`,
      LogStatus.FAILURE,
    )
  } finally {
    isLoading.value = false
  }
}

// 4. 生命周期 - 检查是否已登录
onMounted(() => {
  // 检查是否已经登录（有 token 和 userRole）
  if (authApi.isAuthenticated()) {
    const savedRole = sessionStorage.getItem('userRole') as UserRole
    if (savedRole) {
      // 如果已登录且有角色信息，触发登录成功事件，让 App.vue 处理状态更新
      emit('loginSuccess', savedRole)
    }
  }
})
</script>
