<!--
  @file views/data-admin/UserPermissionPage.vue
  @description 用户权限管理页面

  功能：
  - 已分配用户列表
  - 未分配用户列表
  - 分配/修改/删除权限
  - 数据源和表级别权限控制

  @author Frontend Team
-->
<template>
  <main class="flex-1 overflow-y-auto p-6 space-y-8">
    <!-- 搜索栏 -->
    <div class="w-full max-w-2xl flex items-center gap-3">
      <select
        v-model="searchCategory"
        class="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 bg-white font-bold"
      >
        <option value="all">全部</option>
        <option value="username">用户名</option>
        <option value="email">邮箱</option>
        <option value="datasource">数据源名</option>
        <option value="table">表名</option>
      </select>
      <input
        type="text"
        :placeholder="searchPlaceholder"
        v-model="searchKeyword"
        class="flex-1 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
      />
    </div>

    <!-- 待分配权限用户 -->
    <div>
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-bold">待分配权限用户 ({{ filteredUnassignedUsers.length }})</h2>
        <button
          @click="openAssignModalForSelected"
          :disabled="selectedUserIds.size === 0"
          class="bg-primary text-white rounded-lg px-4 py-2 text-sm btn-effect disabled:bg-primary/50 disabled:cursor-not-allowed"
        >
          <i class="fa fa-key mr-1"></i> 批量分配权限
        </button>
      </div>
      <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-gray-50">
              <th class="px-6 py-3 text-left w-12">
                <input
                  type="checkbox"
                  :checked="areAllUnassignedUsersSelected"
                  @change="handleSelectAll"
                />
              </th>
              <th class="text-left px-6 py-3">用户名</th>
              <th class="text-left px-6 py-3">邮箱</th>
              <th class="text-left px-6 py-3">注册时间</th>
              <th class="text-left px-6 py-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredUnassignedUsers.length === 0">
              <td colspan="5" class="px-6 py-8 text-center text-gray-500">
                未找到匹配的待分配用户
              </td>
            </tr>
            <tr
              v-for="user in filteredUnassignedUsers"
              :key="user.id"
              class="border-b hover:bg-gray-50"
            >
              <td class="px-6 py-4">
                <input
                  type="checkbox"
                  :checked="selectedUserIds.has(user.id)"
                  @change="(e) => handleSelectUser(user.id, (e.target as HTMLInputElement).checked)"
                />
              </td>
              <td class="px-6 py-4">{{ user.username }}</td>
              <td class="px-6 py-4">{{ user.email }}</td>
              <td class="px-6 py-4">{{ user.regTime }}</td>
              <td class="px-6 py-4">
                <button @click="openAssignModal([user])" class="text-primary hover:underline">
                  分配权限
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 已分配权限用户 -->
    <div>
      <h2 class="text-xl font-bold mb-4">
        已分配权限用户 ({{ filteredAssignedPermissions.length }})
      </h2>
      <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-gray-50">
              <th class="text-left px-6 py-3">用户名</th>
              <th class="text-left px-6 py-3">数据源权限</th>
              <th class="text-left px-6 py-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredAssignedPermissions.length === 0">
              <td colspan="3" class="px-6 py-8 text-center text-gray-500">
                未找到匹配的已分配权限用户
              </td>
            </tr>
            <tr
              v-for="p in filteredAssignedPermissions"
              :key="p.id"
              class="border-b hover:bg-gray-50"
            >
              <td class="px-6 py-4 font-medium">{{ p.username }}</td>
              <td class="px-6 py-4">
                <div v-for="perm in p.permissions" :key="perm.dataSourceId" class="mb-1">
                  <span class="font-semibold">{{ perm.dataSourceName }}:</span>
                  <span 
                    class="text-gray-600"
                    :class="{ 'text-primary font-semibold': perm.tables.includes('所有表') }"
                  >
                    {{ perm.tables.join(', ') }}
                  </span>
                </div>
              </td>
              <td class="px-6 py-4">
                <!-- 数据管理员不需要管理权限按钮，因为他们默认拥有所有权限 -->
                <button 
                  v-if="!p.id.startsWith('data-admin-')"
                  @click="openManageModal(p)" 
                  class="text-primary hover:underline"
                >
                  管理权限
                </button>
                <span v-else class="text-gray-400 text-sm italic">数据管理员默认拥有所有权限</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 权限分配/管理模态框 -->
    <AdminModal
      :isOpen="modalType === 'assign' || modalType === 'manage'"
      :title="modalTitle"
      @close="closeModal"
    >
        <PermissionModalContent
        v-if="modalType === 'assign' || modalType === 'manage'"
          :users="modalUsers"
          :existingPermissions="currentItem?.permissions || []"
          :dataSources="dataSources"
          :availableTables="availableTables"
          :loadError="tableLoadErrors"
          @save="handleSavePermissions"
          @close="closeModal"
        />
    </AdminModal>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="text-center">
        <i class="fa fa-spinner fa-spin text-3xl text-primary mb-4"></i>
        <p class="text-gray-500">加载中...</p>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import AdminModal from '../../components/admin/AdminModal.vue'
import PermissionModalContent from '../../components/data-admin/PermissionModalContent.vue'
import type { UserPermissionAssignment, UnassignedUser, DataSourcePermission } from '../../types'
import {
  userDbPermissionApi,
  userApi,
  dbConnectionApi,
  type DbConnection,
} from '../../services/api.real'
import { logOperation, LogModule, LogOperationType, LogStatus } from '../../utils/logger'

// 定义支持的搜索类别
type SearchCategory = 'all' | 'username' | 'email' | 'datasource' | 'table'

// 响应式数据
const loading = ref(true)
const unassignedUsers = ref<UnassignedUser[]>([])
const assignedPermissions = ref<UserPermissionAssignment[]>([])
const dataSources = ref<DbConnection[]>([])
const selectedUserIds = ref<Set<string>>(new Set())
const searchKeyword = ref('')
const searchCategory = ref<SearchCategory>('all')
const modalType = ref<'assign' | 'manage' | null>(null)
const currentItem = ref<UserPermissionAssignment | null>(null)
const usersToAssign = ref<UnassignedUser[]>([])
const availableTables = ref<Record<string, string[]>>({})
const tableLoadErrors = ref<Record<string, string>>({})

// 计算属性
const searchPlaceholder = computed(() => {
  switch (searchCategory.value) {
    case 'all':
      return '搜索（用户名/数据源/表名）'
    case 'username':
      return '搜索用户名'
    case 'email':
      return '搜索邮箱'
    case 'datasource':
      return '搜索数据源名'
    case 'table':
      return '搜索表名'
    default:
      return '搜索'
  }
})

const filteredUnassignedUsers = computed(() => {
  const keyword = searchKeyword.value.toLowerCase().trim()
  if (!keyword) return unassignedUsers.value

  return unassignedUsers.value.filter((user) => {
    switch (searchCategory.value) {
      case 'username':
        return user.username.toLowerCase().includes(keyword)
      case 'email':
        return user.email.toLowerCase().includes(keyword)
      case 'all':
        return (
          user.username.toLowerCase().includes(keyword) ||
          user.email.toLowerCase().includes(keyword)
        )
      default:
        return false
    }
  })
})

const filteredAssignedPermissions = computed(() => {
  const keyword = searchKeyword.value.toLowerCase().trim()
  if (!keyword) return assignedPermissions.value

  return assignedPermissions.value.filter((assignment) => {
    switch (searchCategory.value) {
      case 'username':
        return assignment.username.toLowerCase().includes(keyword)
      case 'email':
        return false // 已分配用户无邮箱字段
      case 'datasource':
        return assignment.permissions.some((perm) =>
          perm.dataSourceName.toLowerCase().includes(keyword),
        )
      case 'table':
        return assignment.permissions.some((perm) =>
          perm.tables.some((table) => table.toLowerCase().includes(keyword)),
        )
      case 'all':
        return (
          assignment.username.toLowerCase().includes(keyword) ||
          assignment.permissions.some((perm) =>
            perm.dataSourceName.toLowerCase().includes(keyword),
          ) ||
          assignment.permissions.some((perm) =>
            perm.tables.some((table) => table.toLowerCase().includes(keyword)),
          )
        )
      default:
        return false
    }
  })
})

const areAllUnassignedUsersSelected = computed(() => {
  return (
    filteredUnassignedUsers.value.length > 0 &&
    selectedUserIds.value.size === filteredUnassignedUsers.value.length
  )
})

const modalTitle = computed(() => {
  if (modalType.value === 'assign' && usersToAssign.value.length > 0) {
    return usersToAssign.value.length > 1
      ? `为 ${usersToAssign.value.length} 位用户分配权限`
      : `为 ${usersToAssign.value[0]?.username || '用户'} 分配权限`
  } else if (modalType.value === 'manage' && currentItem.value) {
    return `管理 ${currentItem.value.username} 的权限`
  }
  return '权限管理'
})

const modalUsers = computed(() => {
  if (modalType.value === 'assign' && usersToAssign.value.length > 0) {
    return usersToAssign.value.map((u) => ({
      id: u.id,
      username: u.username,
    }))
  } else if (modalType.value === 'manage' && currentItem.value) {
    return [
      {
        id: currentItem.value.userId,
        username: currentItem.value.username,
      },
    ]
  }
  return []
})

// 生命周期
onMounted(() => {
  loadData()
})

// 方法
const loadData = async () => {
  try {
    loading.value = true
    const [allUsers, assignedPerms, connections] = await Promise.all([
      userApi.getList(),
      userDbPermissionApi.getAssigned(),
      dbConnectionApi.getList(),
    ])

    dataSources.value = connections
    const userMap = new Map(allUsers.map((u) => [u.id, u]))

    // 数据管理员角色ID（roleId=2）
    const DATA_ADMIN_ROLE_ID = 2

    // 解析已分配权限（排除系统管理员和数据管理员）
    // 数据管理员不应该在数据库中存储权限记录，因为他们默认拥有所有权限
    const parsedAssigned = assignedPerms
      .filter((perm) => {
        const user = userMap.get(perm.userId)
        // 排除系统管理员（roleId=1）和数据管理员（roleId=2）
        return user && user.roleId !== 1 && user.roleId !== DATA_ADMIN_ROLE_ID
      })
      .map((perm) => {
        const user = userMap.get(perm.userId)
        let permissions: DataSourcePermission[] = []
        try {
          const details = JSON.parse(perm.permissionDetails || '[]')
          permissions = details.map((detail: any) => {
            const conn = connections.find((c) => c.id === detail.db_connection_id)
            // 优先使用 table_names，如果没有则使用 table_ids（兼容旧数据）
            const tableNames = detail.table_names || (detail.table_ids?.map((tid: number) => `table_${tid}`) || [])
            return {
              dataSourceId: String(detail.db_connection_id),
              dataSourceName: conn?.name || '未知数据源',
              tables: tableNames,
            }
          })
        } catch (e) {
          console.error('解析权限详情失败:', e)
        }
        return {
          id: String(perm.id),
          userId: String(perm.userId),
          username: user?.username || '未知用户',
          permissions,
        }
      })

    // 自动添加数据管理员到已分配权限列表
    // 数据管理员默认拥有所有数据库和表的查询权限
    const dataAdminUsers = allUsers.filter((u) => u.roleId === DATA_ADMIN_ROLE_ID)
    const dataAdminAssigned = dataAdminUsers.map((user) => {
      // 为每个数据源创建权限，显示"所有表"
      const permissions: DataSourcePermission[] = connections.map((conn) => ({
        dataSourceId: String(conn.id),
        dataSourceName: conn.name,
        tables: ['所有表'], // 显示"所有表"表示拥有所有表的权限
      }))

      return {
        id: `data-admin-${user.id}`, // 使用特殊ID标识数据管理员
        userId: String(user.id),
        username: user.username,
        permissions,
      }
    })

    // 合并已分配权限和数据管理员权限
    const allAssignedPermissions = [...parsedAssigned, ...dataAdminAssigned]

    // 找出未分配权限的用户（排除系统管理员和数据管理员）
    // 数据管理员不需要分配权限，因为他们默认拥有所有权限
    const assignedUserIds = new Set(assignedPerms.map((p) => p.userId))
    const businessUsers = allUsers.filter((u) => u.roleId !== 1 && u.roleId !== DATA_ADMIN_ROLE_ID)
    const unassigned = businessUsers
      .filter((u) => !assignedUserIds.has(u.id))
      .map((u) => ({
        id: String(u.id),
        username: u.username,
        email: u.email,
        regTime: new Date().toISOString().split('T')[0],
      }))

    assignedPermissions.value = allAssignedPermissions
    unassignedUsers.value = unassigned
  } catch (error) {
    console.error('加载权限数据失败:', error)
    alert('加载权限数据失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSelectAll = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.checked) {
    selectedUserIds.value = new Set(filteredUnassignedUsers.value.map((u) => u.id))
  } else {
    selectedUserIds.value = new Set()
  }
}

const handleSelectUser = (id: string, checked: boolean) => {
  const newSet = new Set(selectedUserIds.value)
  if (checked) newSet.add(id)
  else newSet.delete(id)
  selectedUserIds.value = newSet
}

const openAssignModalForSelected = () => {
  const selectedUsers = filteredUnassignedUsers.value.filter((u) => selectedUserIds.value.has(u.id))
  if (selectedUsers.length === 0) return
  openAssignModal(selectedUsers)
}

const openAssignModal = (users: UnassignedUser[]) => {
  if (users.length === 0) return
  usersToAssign.value = users
  currentItem.value = null
  modalType.value = 'assign'
}

const openManageModal = (permission: UserPermissionAssignment) => {
  currentItem.value = permission
  modalType.value = 'manage'
}

const closeModal = () => {
  modalType.value = null
  currentItem.value = null
  usersToAssign.value = []
}

const handleSavePermissions = async (userIds: string[], permissions: DataSourcePermission[]) => {
  try {
    const filteredPerms = permissions.filter((p) => p.tables.length > 0)

    if (modalType.value === 'assign') {
      const currentUserId = Number(sessionStorage.getItem('userId') || '1')
      const permissionDetails = filteredPerms.map((p) => ({
        db_connection_id: Number(p.dataSourceId),
        table_names: p.tables, // 使用真实的表名
        table_ids: [], // 保持为空数组，后端主要使用 table_names
      }))

      for (const userId of userIds) {
        const user = usersToAssign.value.find((u) => u.id === userId)
        await userDbPermissionApi.create({
          userId: Number(userId),
          permissionDetails: JSON.stringify(permissionDetails),
          isAssigned: 1,
          lastGrantUserId: currentUserId,
        })
        await logOperation(
          LogModule.PERMISSION,
          LogOperationType.ASSIGN,
          `为用户 ${user?.username} 分配数据权限`,
          LogStatus.SUCCESS,
        )
      }
      alert('分配权限成功')
      await loadData()
    } else if (modalType.value === 'manage' && currentItem.value) {
      const permissionDetails = filteredPerms.map((p) => ({
        db_connection_id: Number(p.dataSourceId),
        table_names: p.tables, // 使用真实的表名
        table_ids: [], // 保持为空数组，后端主要使用 table_names
      }))
      await userDbPermissionApi.update({
        id: Number(currentItem.value.id),
        permissionDetails: JSON.stringify(permissionDetails),
        lastGrantUserId: Number(sessionStorage.getItem('userId') || '1'),
      })
      await logOperation(
        LogModule.PERMISSION,
        LogOperationType.UPDATE,
        `更新用户 ${currentItem.value.username} 的数据权限`,
        LogStatus.SUCCESS,
      )
      alert('更新权限成功')
      await loadData()
    }
  } catch (error) {
    console.error('保存权限失败:', error)
    const operationType =
      modalType.value === 'assign' ? LogOperationType.ASSIGN : LogOperationType.UPDATE
    await logOperation(
      LogModule.PERMISSION,
      operationType,
      `保存权限失败：${error instanceof Error ? error.message : '未知错误'}`,
      LogStatus.FAILURE,
    )
    alert('保存权限失败: ' + (error instanceof Error ? error.message : '未知错误'))
    return
  }
  closeModal()
}

// 加载表列表
const loadAvailableTables = async () => {
  const tablesMap: Record<string, string[]> = {}
  const errors: Record<string, string> = {}
  
  for (const ds of dataSources.value) {
    try {
      const tables = await dbConnectionApi.getTables(Number(ds.id))
      tablesMap[String(ds.id)] = tables || []
      // 清除之前的错误
      delete errors[String(ds.id)]
    } catch (error: any) {
      console.error(`加载数据源 ${ds.name} 表失败:`, error)
      // 提取错误消息
      const errorMessage = error?.message || error?.response?.data?.message || '未知错误'
      errors[String(ds.id)] = errorMessage
      tablesMap[String(ds.id)] = []
    }
  }
  
  availableTables.value = tablesMap
  tableLoadErrors.value = errors
}

// 监听数据源变化，加载表列表
watch(dataSources, () => {
  if (dataSources.value.length > 0) {
    loadAvailableTables()
  }
})
</script>
