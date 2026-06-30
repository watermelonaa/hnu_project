<!--
  @file views/admin/UserManagementPage.vue
  @description 用户管理页面

  功能：
  - 用户列表展示（分页、筛选）
  - 创建/编辑/删除用户
  - 用户状态管理（启用/禁用）
  - 角色分配

  @author Frontend Team
-->
<template>
  <div class="flex-1 overflow-y-auto p-6 space-y-6">
    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="text-center">
        <i class="fa fa-spinner fa-spin text-3xl text-primary mb-4"></i>
        <p class="text-gray-500">加载中...</p>
      </div>
    </div>

    <!-- 主内容 -->
    <div v-else class="space-y-6">
      <!-- 顶部操作按钮 -->
      <div class="flex justify-between items-center">
        <button
          @click="requestAddUser"
          class="bg-primary text-white rounded-lg px-4 py-2 text-sm btn-effect"
        >
          <i class="fa fa-plus mr-1"></i> 添加用户
        </button>
      </div>

        <!-- 过滤和搜索区域 -->
      <div class="bg-white rounded-xl p-6 shadow-sm">
        <div class="flex flex-col md:flex-row gap-4 mb-4">
          <div class="flex-1">
            <div class="relative">
              <i class="fa fa-search absolute left-3 top-3 text-gray-400"></i>
              <input
                type="text"
                name="search"
                v-model="filters.search"
                @input="handleFilterChange"
                placeholder="搜索用户名/邮箱"
                class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30 focus:border-primary"
              />
            </div>
          </div>
          <div class="w-full md:w-40">
            <select
              name="role"
              v-model="filters.role"
              @change="handleFilterChange"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30 focus:border-primary"
            >
              <option value="">所有角色</option>
              <option value="sys-admin">系统管理员</option>
              <option value="data-admin">数据管理员</option>
              <option value="normal-user">普通用户</option>
            </select>
          </div>
          <div class="w-full md:w-32">
            <select
              name="status"
              v-model="filters.status"
              @change="handleFilterChange"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/30 focus:border-primary"
            >
              <option value="">所有状态</option>
              <option value="active">正常</option>
              <option value="disabled">禁用</option>
            </select>
          </div>
        </div>

        <!-- 批量操作区域 -->
        <div class="border-t pt-4 mt-4 flex items-center space-x-4">
          <label class="flex items-center text-sm font-medium cursor-pointer">
            <input
              type="checkbox"
              @change="handleSelectAll"
              :checked="paginatedUsers.length > 0 && selectedUserIds.size === paginatedUsers.length"
              class="w-4 h-4 text-primary focus:ring-primary/30 mr-2"
            />
            <span>选中项 ({{ selectedUserIds.size }})</span>
          </label>
          <select
            v-model="batchAction"
            @change="handleBatchActionChange"
            class="border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-primary/30 focus:border-primary"
          >
            <option value="">批量操作</option>
            <option value="enable">启用</option>
            <option value="disable">禁用</option>
            <option value="delete">删除</option>
          </select>
          <button
            @click="handleApplyBatchAction"
            class="bg-primary text-white px-4 py-2 rounded-lg text-sm btn-effect"
          >
            应用
          </button>
        </div>
      </div>

      <!-- 用户表格 -->
      <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="bg-gray-50">
                <th class="px-6 py-3 text-left">
                  <input
                    type="checkbox"
                    @change="handleSelectAll"
                    :checked="
                      paginatedUsers.length > 0 && selectedUserIds.size === paginatedUsers.length
                    "
                    class="w-4 h-4 text-primary focus:ring-primary/30"
                  />
                </th>
                <th class="px-6 py-3 text-left">ID</th>
                <th class="px-6 py-3 text-left">用户名</th>
                <th class="px-6 py-3 text-left">角色</th>
                <th class="px-6 py-3 text-left">邮箱</th>
                <th class="px-6 py-3 text-left">手机号</th>
                <th class="px-6 py-3 text-left">状态</th>
                <th class="px-6 py-3 text-left">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in paginatedUsers" :key="user.id" class="border-b hover:bg-gray-50">
                <td class="px-6 py-4">
                  <input
                    type="checkbox"
                    :checked="selectedUserIds.has(user.id)"
                    @change="
                      (e) => handleSelectUser(user.id, (e.target as HTMLInputElement).checked)
                    "
                    class="w-4 h-4 text-primary focus:ring-primary/30"
                  />
                </td>
                <td class="px-6 py-4">{{ user.id }}</td>
                <td class="px-6 py-4">{{ user.username }}</td>
                <td class="px-6 py-4">
                  <span :class="`px-2 py-1 rounded-full text-xs ${getRoleClass(user.role)}`">
                    {{ getRoleName(user.role) }}
                  </span>
                </td>
                <td class="px-6 py-4">{{ user.email }}</td>
                <td class="px-6 py-4">{{ user.phonenumber || '-' }}</td>
                <td class="px-6 py-4">
                  <span
                    :class="`px-2 py-1 rounded-full text-xs ${
                      user.status === 'active'
                        ? 'bg-success/10 text-success'
                        : 'bg-gray-200 text-gray-700'
                    }`"
                  >
                    {{ user.status === 'active' ? '正常' : '禁用' }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <div class="flex space-x-3">
                    <button
                      @click="() => requestEditUser(user)"
                      :disabled="user.role === 'sys-admin'"
                      :class="`${user.role === 'sys-admin' ? 'text-gray-300 cursor-not-allowed' : 'text-primary hover:text-primary/80'}`"
                      :title="user.role === 'sys-admin' ? '系统管理员不可编辑' : '编辑'"
                    >
                      <i class="fa fa-edit"></i>
                    </button>
                    <button
                      @click="() => requestResetPassword(user)"
                      :disabled="user.role === 'sys-admin'"
                      :class="`${user.role === 'sys-admin' ? 'text-gray-300 cursor-not-allowed' : 'text-indigo-500 hover:text-indigo-400'}`"
                      :title="user.role === 'sys-admin' ? '系统管理员不可重置密码' : '重置密码'"
                    >
                      <i class="fa fa-key"></i>
                    </button>
                    <button
                      @click="() => requestToggleUserStatus(user)"
                      :disabled="user.role === 'sys-admin'"
                      :class="`${user.role === 'sys-admin' ? 'text-gray-300 cursor-not-allowed' : 'text-secondary hover:text-secondary/80'}`"
                      :title="user.role === 'sys-admin' ? '系统管理员不可修改状态' : (user.status === 'active' ? '禁用' : '启用')"
                    >
                      <i :class="`fa ${user.status === 'active' ? 'fa-ban' : 'fa-check'}`"></i>
                    </button>
                    <button
                      @click="() => requestDeleteUser(user)"
                      :disabled="user.role === 'sys-admin'"
                      :class="`${user.role === 'sys-admin' ? 'text-gray-300 cursor-not-allowed' : 'text-danger hover:text-danger/80'}`"
                      :title="user.role === 'sys-admin' ? '系统管理员不可删除' : '删除'"
                    >
                      <i class="fa fa-trash"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="px-6 py-4 border-t flex justify-between items-center">
          <p class="text-sm text-gray-500">
            显示 {{ paginatedUsers.length }} 条，共 {{ filteredUsers.length }} 条
          </p>
          <!-- 分页控件 -->
          <div class="flex items-center space-x-2">
            <button
              @click="goToPage(currentPage - 1)"
              :disabled="currentPage === 1"
              class="px-3 py-1 border rounded-lg text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              <i class="fa fa-chevron-left"></i>
            </button>
            <span class="text-sm text-gray-700">
              第 {{ currentPage }} / {{ totalPages }} 页
            </span>
            <button
              @click="goToPage(currentPage + 1)"
              :disabled="currentPage === totalPages"
              class="px-3 py-1 border rounded-lg text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-100"
            >
              <i class="fa fa-chevron-right"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 模态框 -->
    <AdminModal
      :isOpen="modal === 'edit' || modal === 'add'"
      @close="setModal(null)"
      :title="modal === 'add' ? '添加新用户' : '编辑用户信息'"
    >
      <form @submit.prevent="handleSaveUser" class="space-y-4">
        <div>
          <label class="block text-sm mb-1">用户名</label>
          <input
            ref="usernameInput"
            name="username"
            :value="userToProcess?.username || ''"
            required
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">邮箱</label>
          <input
            ref="emailInput"
            name="email"
            type="email"
            :value="userToProcess?.email || ''"
            required
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">手机号</label>
          <input
            ref="phoneInput"
            name="phonenumber"
            pattern="[0-9]{11}"
            title="请输入 11 位数字手机号码"
            :value="userToProcess?.phonenumber || ''"
            required
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div v-if="modal === 'add'">
          <label class="block text-sm mb-1">密码</label>
          <input
            ref="passwordInput"
            name="password"
            type="password"
            minlength="6"
            required
            class="w-full px-3 py-2 border rounded-lg"
          />
        </div>
        <div>
          <label class="block text-sm mb-1">角色</label>
          <select
            ref="roleSelect"
            name="role"
            :value="userToProcess?.role || 'normal-user'"
            class="w-full px-3 py-2 border rounded-lg"
          >
            <option value="data-admin">数据管理员</option>
            <option value="normal-user">普通用户</option>
          </select>
        </div>
        <div class="flex justify-end space-x-2 pt-4">
          <button 
            type="button" 
            @click="handleCancel" 
            class="px-4 py-2 border rounded-lg"
          >
            取消
          </button>
          <button 
            type="submit" 
            class="px-4 py-2 bg-primary text-white rounded-lg"
          >
            确定
          </button>
        </div>
      </form>
    </AdminModal>

    <AdminModal :isOpen="modal === 'confirmDelete'" @close="setModal(null)" title="确认删除用户">
      <p>您确定要删除用户 "{{ userToProcess?.username }}" 吗？此操作不可撤销。</p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="setModal(null)" class="px-4 py-2 border rounded-lg text-sm">取消</button>
        <button
          @click="confirmDeleteUser"
          class="px-4 py-2 bg-danger text-white rounded-lg text-sm"
        >
          确认删除
        </button>
      </div>
    </AdminModal>

    <AdminModal
      :isOpen="modal === 'confirmResetPassword'"
      @close="setModal(null)"
      title="确认重置密码"
    >
      <p>
        您确定要为用户 "{{ userToProcess?.username }}"
        重置密码吗？重置后密码将统一为 "123456"。
      </p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="setModal(null)" class="px-4 py-2 border rounded-lg">取消</button>
        <button @click="confirmResetPassword" class="px-4 py-2 bg-warning text-white rounded-lg">
          确认重置
        </button>
      </div>
    </AdminModal>

    <AdminModal
      :isOpen="modal === 'confirmToggleStatus'"
      @close="setModal(null)"
      :title="`确认${userToProcess?.status === 'active' ? '禁用' : '启用'}用户`"
    >
      <p>
        您确定要{{ userToProcess?.status === 'active' ? '禁用' : '启用' }}用户 "{{
          userToProcess?.username
        }}" 吗？
      </p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="setModal(null)" class="px-4 py-2 border rounded-lg text-sm">取消</button>
        <button
          @click="confirmToggleUserStatus"
          :class="`px-4 py-2 text-white rounded-lg text-sm ${
            userToProcess?.status === 'active' ? 'bg-danger' : 'bg-success'
          }`"
        >
          确认
        </button>
      </div>
    </AdminModal>

    <AdminModal :isOpen="modal === 'confirmBatch'" @close="setModal(null)" title="确认批量操作">
      <p>
        您确定要对选中的 {{ selectedUserIds.size }} 个用户执行 "{{ batchActionMap[batchAction] }}"
        操作吗？
      </p>
      <div class="flex justify-end space-x-2 mt-4">
        <button @click="setModal(null)" class="px-4 py-2 border rounded-lg text-sm">取消</button>
        <button
          @click="confirmBatchAction"
          class="px-4 py-2 bg-primary text-white rounded-lg text-sm"
        >
          确认
        </button>
      </div>
    </AdminModal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import type { AdminUser, UserRole } from '../../types'
import { userApi, authApi } from '../../services/api.real'
import { logOperation, LogModule, LogOperationType, LogStatus } from '../../utils/logger'

// 状态定义
const users = ref<AdminUser[]>([])
const loading = ref(true)
const filters = ref({
  search: '',
  role: '',
  status: '',
})
const selectedUserIds = ref<Set<number>>(new Set())
const modal = ref<
  | 'add'
  | 'edit'
  | 'confirmDelete'
  | 'confirmBatch'
  | 'confirmResetPassword'
  | 'confirmToggleStatus'
  | null
>(null)
const userToProcess = ref<AdminUser | null>(null)
const batchAction = ref<'enable' | 'disable' | 'delete' | ''>('')

// 分页状态
const currentPage = ref(1)
const pageSize = ref(10)

// 模板引用
const usernameInput = ref<HTMLInputElement>()
const emailInput = ref<HTMLInputElement>()
const phoneInput = ref<HTMLInputElement>()
const passwordInput = ref<HTMLInputElement>()
const roleSelect = ref<HTMLSelectElement>()

// 批量操作映射
const batchActionMap = {
  enable: '启用',
  disable: '禁用',
  delete: '删除',
} as const

// 生命周期
onMounted(() => {
  loadUsers()
})

// 计算属性 - 过滤后的用户
const filteredUsers = computed(() => {
  return users.value.filter(
    (user) =>
      (user.username.toLowerCase().includes(filters.value.search.toLowerCase()) ||
        user.email.toLowerCase().includes(filters.value.search.toLowerCase())) &&
      (filters.value.role ? user.role === filters.value.role : true) &&
      (filters.value.status ? user.status === filters.value.status : true),
  )
})

// 计算属性 - 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredUsers.value.length / pageSize.value) || 1
})

// 计算属性 - 当前页的用户
const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredUsers.value.slice(start, end)
})

// 监听筛选变化，重置到第一页
watch(filteredUsers, () => {
  currentPage.value = 1
})

// 映射角色ID到前端角色类型
const mapRoleIdToRole = (roleId: number): UserRole => {
  if (roleId === 1) return 'sys-admin'
  if (roleId === 2) return 'data-admin'
  return 'normal-user'
}

// 映射前端角色到后端角色ID
const mapRoleToRoleId = (role: UserRole): number => {
  if (role === 'sys-admin') return 1
  if (role === 'data-admin') return 2
  return 3
}

// 获取角色名称
const getRoleName = (role: UserRole) =>
  ({
    'sys-admin': '系统管理员',
    'data-admin': '数据管理员',
    'normal-user': '普通用户',
  })[role]

// 获取角色样式
const getRoleClass = (role: UserRole) =>
  ({
    'sys-admin': 'bg-primary/10 text-primary',
    'data-admin': 'bg-success/10 text-success',
    'normal-user': 'bg-secondary/10 text-secondary',
  })[role]

// 方法
const loadUsers = async () => {
  try {
    loading.value = true
    const userList = await userApi.getList()
    // 显示所有用户，包括系统管理员
    // 转换后端User格式到前端AdminUser格式
    const adminUsers: AdminUser[] = userList.map((user) => ({
      id: user.id,
      username: user.username,
      role: mapRoleIdToRole(user.roleId),
      email: user.email,
      regTime: new Date().toISOString().split('T')[0], // 后端没有返回createTime，暂时用当前日期
      status: user.status === 1 ? 'active' : 'disabled',
      phonenumber: user.phonenumber || '',
    }))
    users.value = adminUsers
  } catch (error) {
    console.error('加载用户列表失败:', error)
    alert('加载用户列表失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleFilterChange = (e: Event) => {
  const target = e.target as HTMLInputElement | HTMLSelectElement
  filters.value = { ...filters.value, [target.name]: target.value }
  currentPage.value = 1 // 筛选时重置到第一页
}

// 分页方法
const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

const handleSelectAll = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.checked) {
    selectedUserIds.value = new Set(paginatedUsers.value.map((u) => u.id))
  } else {
    selectedUserIds.value = new Set()
  }
}

const handleSelectUser = (id: number, checked: boolean) => {
  const newSet = new Set(selectedUserIds.value)
  if (checked) {
    newSet.add(id)
  } else {
    newSet.delete(id)
  }
  selectedUserIds.value = newSet
}

const handleBatchActionChange = (e: Event) => {
  const target = e.target as HTMLSelectElement
  batchAction.value = target.value as any
}

const requestDeleteUser = (user: AdminUser) => {
  userToProcess.value = user
  modal.value = 'confirmDelete'
}

const confirmDeleteUser = async () => {
  if (userToProcess.value) {
    try {
      await userApi.delete(userToProcess.value.id)
      await logOperation(
        LogModule.USER_MANAGEMENT,
        LogOperationType.DELETE,
        `删除用户：${userToProcess.value.username}`,
        LogStatus.SUCCESS,
      )
      users.value = users.value.filter((u) => u.id !== userToProcess.value!.id)
      alert('删除成功')
    } catch (error) {
      console.error('删除用户失败:', error)
      await logOperation(
        LogModule.USER_MANAGEMENT,
        LogOperationType.DELETE,
        `删除用户失败：${error instanceof Error ? error.message : '未知错误'}`,
        LogStatus.FAILURE,
      )
      alert('删除失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
  modal.value = null
}

const requestToggleUserStatus = (user: AdminUser) => {
  userToProcess.value = user
  modal.value = 'confirmToggleStatus'
}

const confirmToggleUserStatus = async () => {
  if (userToProcess.value) {
    try {
      const newStatus = userToProcess.value.status === 'active' ? 0 : 1
      const operationType =
        userToProcess.value.status === 'active' ? LogOperationType.DISABLE : LogOperationType.ENABLE

      await userApi.update({
        id: userToProcess.value.id,
        status: newStatus,
      })

      await logOperation(
        LogModule.USER_MANAGEMENT,
        operationType,
        `${operationType}用户：${userToProcess.value.username}`,
        LogStatus.SUCCESS,
      )

      users.value = users.value.map((u) =>
        u.id === userToProcess.value!.id
          ? { ...u, status: u.status === 'active' ? 'disabled' : 'active' }
          : u,
      )

      alert('操作成功')
    } catch (error) {
      console.error('更新用户状态失败:', error)
      const operationType =
        userToProcess.value.status === 'active' ? LogOperationType.DISABLE : LogOperationType.ENABLE

      await logOperation(
        LogModule.USER_MANAGEMENT,
        operationType,
        `${operationType}用户失败：${error instanceof Error ? error.message : '未知错误'}`,
        LogStatus.FAILURE,
      )

      alert('操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
    }
  }
  modal.value = null
}

const handleApplyBatchAction = () => {
  if (!batchAction.value || selectedUserIds.value.size === 0) return
  modal.value = 'confirmBatch'
}

const confirmBatchAction = async () => {
  try {
    if (batchAction.value === 'delete') {
      // 批量删除
      const deletePromises = Array.from(selectedUserIds.value).map((id) => userApi.delete(id))
      await Promise.all(deletePromises)
      users.value = users.value.filter((u) => !selectedUserIds.value.has(u.id))
    } else {
      // 批量启用/禁用
      const newStatus = batchAction.value === 'enable' ? 1 : 0
      const updatePromises = Array.from(selectedUserIds.value).map((id) =>
        userApi.update({ id, status: newStatus }),
      )
      await Promise.all(updatePromises)
      users.value = users.value.map((u) =>
        selectedUserIds.value.has(u.id)
          ? { ...u, status: batchAction.value === 'enable' ? 'active' : 'disabled' }
          : u,
      )
    }
    alert('批量操作成功')
  } catch (error) {
    console.error('批量操作失败:', error)
    alert('批量操作失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
  selectedUserIds.value = new Set()
  modal.value = null
}

const requestEditUser = (user: AdminUser) => {
  userToProcess.value = user
  modal.value = 'edit'
}

const requestAddUser = () => {
  userToProcess.value = null
  modal.value = 'add'
}

const handleCancel = () => {
  setModal(null)
}

const handleSaveUser = async (e?: Event) => {
  // 阻止默认表单提交行为
  if (e) {
    e.preventDefault()
  }

  try {
    // 验证表单
    const form = e?.target as HTMLFormElement
    if (form && !form.checkValidity()) {
      form.reportValidity()
      return
    }

    if (modal.value === 'add') {
      const password = passwordInput.value?.value || ''
      // HTML5原生验证会自动检查minlength，这里不需要额外检查
      if (passwordInput.value && !passwordInput.value.checkValidity()) {
        passwordInput.value.reportValidity()
        return
      }

      const role = (roleSelect.value?.value || 'normal-user') as UserRole
      const phonenumber = phoneInput.value?.value || ''

      await userApi.create({
        username: usernameInput.value?.value || '',
        email: emailInput.value?.value || '',
        password: password,
        roleId: mapRoleToRoleId(role),
        status: 1,
        phonenumber,
      } as any)

      alert('添加成功')
      await loadUsers() // 重新加载列表
      setModal(null)
    } else if (modal.value === 'edit' && userToProcess.value) {
      // 验证编辑表单
      if (usernameInput.value && !usernameInput.value.checkValidity()) {
        usernameInput.value.reportValidity()
        return
      }
      if (emailInput.value && !emailInput.value.checkValidity()) {
        emailInput.value.reportValidity()
        return
      }
      if (phoneInput.value && !phoneInput.value.checkValidity()) {
        phoneInput.value.reportValidity()
        return
      }

      const phonenumber = phoneInput.value?.value || ''

      await userApi.update({
        id: userToProcess.value.id,
        username: usernameInput.value?.value || '',
        email: emailInput.value?.value || '',
        phonenumber,
        roleId: mapRoleToRoleId((roleSelect.value?.value || 'normal-user') as UserRole),
      })

      alert('更新成功')
      await loadUsers() // 重新加载列表
      setModal(null)
    }
  } catch (error) {
    console.error('保存用户失败:', error)
    alert('保存失败: ' + (error instanceof Error ? error.message : '未知错误'))
    // 发生错误时不关闭模态框，让用户有机会修正
    return
  }
}

const requestResetPassword = (user: AdminUser) => {
  userToProcess.value = user
  modal.value = 'confirmResetPassword'
}

const confirmResetPassword = async () => {
  if (!userToProcess.value) {
    modal.value = null
    return
  }

  try {
    // 调用API直接重置用户密码
    const result = await userApi.resetPassword(userToProcess.value.id)

    // 记录操作日志
    await logOperation(
      LogModule.USER_MANAGEMENT,
      LogOperationType.UPDATE,
      `重置用户密码：${userToProcess.value.username} (${userToProcess.value.email})`,
      LogStatus.SUCCESS,
    )

    alert(result || `用户 ${userToProcess.value.username} 的密码已重置成功。`)
  } catch (error) {
    console.error('重置密码失败:', error)
    
    // 记录失败日志
    await logOperation(
      LogModule.USER_MANAGEMENT,
      LogOperationType.UPDATE,
      `重置用户密码失败：${userToProcess.value.username} - ${error instanceof Error ? error.message : '未知错误'}`,
      LogStatus.FAILURE,
    )

    alert('重置密码失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    modal.value = null
  }
}

const setModal = (value: typeof modal.value) => {
  modal.value = value
}
</script>
