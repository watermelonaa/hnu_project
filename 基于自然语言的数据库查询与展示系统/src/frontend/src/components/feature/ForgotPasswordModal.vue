<!--
  @file components/feature/ForgotPasswordModal.vue
  @description 忘记密码模态框

  功能：
  - 邮箱输入验证
  - 发送重置密码链接
  - 表单提交处理

  @author Frontend Team
-->
<template>
  <div
    class="fixed inset-0 bg-black/60 flex items-center justify-center z-50"
    @click="$emit('close')"
  >
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6" @click.stop>
      <div v-if="!resetEmailSent">
        <h2 class="text-xl font-bold mb-4 text-center">重置密码</h2>
        <form @submit.prevent="handleRequestReset">
          <p class="text-sm text-gray-600 mb-4">
            请输入您注册时使用的邮箱地址，我们将向您发送一封密码重置邮件。
          </p>
          <div class="mb-4">
            <label for="reset-email" class="block text-gray-700 font-medium mb-2">邮箱</label>
            <input
              type="email"
              id="reset-email"
              v-model="resetEmail"
              placeholder="请输入邮箱"
              required
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30 focus:border-primary"
            />
          </div>
          <div class="flex justify-end space-x-2">
            <button
              type="button"
              @click="$emit('close')"
              class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="submit"
              class="px-4 py-2 bg-primary text-white rounded-lg hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
            >
              发送重置链接
            </button>
          </div>
        </form>
      </div>

      <div v-else class="text-center">
        <div
          class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4"
        >
          <i class="fa fa-check text-green-500 text-3xl"></i>
        </div>
        <h3 class="text-xl font-bold text-gray-800 mb-2">已发送</h3>
        <p class="text-sm text-gray-500">
          如果该邮箱地址已注册，一封包含密码重置链接的邮件已经发送到您的邮箱。
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 1. Emits：定义组件向父组件发出的事件
const emit = defineEmits(['close'])

// 2. State
const resetEmail = ref('')
const resetEmailSent = ref(false)

// 3. Methods
const handleRequestReset = async () => {
  try {
    // 调用后端API发送重置密码邮件
    const response = await fetch('/api/auth/forgot-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email: resetEmail.value }),
    })

    const data = await response.json()
    
    if (data.code === 200 || data.code === 0) {
      resetEmailSent.value = true

      // 3秒后自动关闭模态框
      setTimeout(() => {
        emit('close')
        // 重置状态以便下次打开
        setTimeout(() => {
          resetEmailSent.value = false
          resetEmail.value = ''
        }, 300)
      }, 3000)
    } else {
      console.error('发送重置邮件失败:', data.message)
      alert(data.message || '发送失败，请稍后重试')
    }
  } catch (error) {
    console.error('请求失败:', error)
    alert('网络错误，请稍后重试')
  }
}
</script>
