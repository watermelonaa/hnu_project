<!--
  @file components/data-admin/PermissionModalContent.vue
  @description 权限分配模态框内容

  功能：
  - 数据源权限选择
  - 表级别权限控制
  - 批量用户分配

  @author Frontend Team
-->
<template>
  <div class="space-y-4 max-h-[60vh] overflow-y-auto pr-2">
    <div v-for="ds in dataSources" :key="ds.id" class="p-3 border rounded-lg">
      <h4 class="font-semibold mb-2">{{ ds.name }}</h4>
      <div class="border-t pt-2">
        <div v-if="allTablesForDs(String(ds.id)).length > 0">
          <label class="flex items-center mb-2 font-medium text-sm">
            <input
              type="checkbox"
              @change="
                (e) => handleSelectAllTables(String(ds.id), (e.target as HTMLInputElement).checked)
              "
              :checked="areAllTablesSelected(String(ds.id))"
              class="mr-2 h-4 w-4"
            />
            全选
          </label>
          <div class="grid grid-cols-2 gap-2 text-sm">
            <label v-for="table in allTablesForDs(String(ds.id))" :key="table" class="flex items-center">
              <input
                type="checkbox"
                :checked="isTableSelected(String(ds.id), table)"
                @change="
                  (e) =>
                    handleTableToggle(String(ds.id), table, (e.target as HTMLInputElement).checked)
                "
                class="mr-2 h-4 w-4"
              />
              {{ table }}
            </label>
          </div>
        </div>
        <div v-else class="text-sm text-gray-500 py-2">
          <div>暂无可用表或加载失败</div>
          <div v-if="loadError[ds.id]" class="text-red-500 text-xs mt-1">
            {{ loadError[ds.id] }}
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="flex justify-end space-x-2 mt-6">
    <button @click="onClose" class="px-4 py-2 border rounded-lg">取消</button>
    <button @click="handleSave" class="px-4 py-2 bg-primary text-white rounded-lg">保存</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { DataSourcePermission } from '../../types'
import type { DbConnection } from '../../services/api.real'

interface Props {
  users: { id: string; username: string }[]
  existingPermissions: DataSourcePermission[]
  dataSources: DbConnection[]
  availableTables: Record<string, string[]>
  loadError?: Record<string, string>
}

interface Emits {
  (e: 'save', userIds: string[], permissions: DataSourcePermission[]): void
  (e: 'close'): void
}

const props = withDefaults(defineProps<Props>(), {
  loadError: () => ({}),
})

const emit = defineEmits<Emits>()

// 权限状态
const permissions = ref<DataSourcePermission[]>(
  props.dataSources.map((ds) => {
    const existing = props.existingPermissions.find((p) => p.dataSourceId === String(ds.id))
    return {
      dataSourceId: String(ds.id),
      dataSourceName: ds.name,
      tables: existing ? [...existing.tables] : [],
    }
  }),
)

// 计算属性
const allTablesForDs = computed(() => (dsId: string) => {
  return props.availableTables[dsId] || []
})

const areAllTablesSelected = computed(() => (dsId: string) => {
  const currentPerm = permissions.value.find((p) => p.dataSourceId === dsId)
  const allTables = allTablesForDs.value(dsId)
  return currentPerm
    ? currentPerm.tables.length === allTables.length && allTables.length > 0
    : false
})

const isTableSelected = computed(() => (dsId: string, table: string) => {
  const currentPerm = permissions.value.find((p) => p.dataSourceId === dsId)
  return currentPerm ? currentPerm.tables.includes(table) : false
})

// 方法
const handleTableToggle = (dsId: string, table: string, checked: boolean) => {
  permissions.value = permissions.value.map((p) => {
    if (p.dataSourceId === dsId) {
      const newTables = new Set(p.tables)
      if (checked) newTables.add(table)
      else newTables.delete(table)
      return { ...p, tables: Array.from(newTables) }
    }
    return p
  })
}

const handleSelectAllTables = (dsId: string, checked: boolean) => {
  const currentTables = allTablesForDs.value(dsId)
  permissions.value = permissions.value.map((p) =>
    p.dataSourceId === dsId ? { ...p, tables: checked ? currentTables : [] } : p,
  )
}

const handleSave = () => {
  const userIds = props.users.map((u) => u.id)
  emit('save', userIds, permissions.value)
}

const onClose = () => {
  emit('close')
}
</script>
