<template>
  <el-dialog
    v-model="visible"
    title="分享查询结果"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="选择好友">
        <el-select
          v-model="form.receiveUserId"
          placeholder="请选择要分享的好友"
          filterable
          style="width: 100%"
        >
          <el-option
            v-for="friend in friendList"
            :key="friend.friendId"
            :label="friend.friendName"
            :value="friend.friendId"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <img
                :src="friend.friendAvatar || '/default-avatar.png'"
                style="width: 24px; height: 24px; border-radius: 50%;"
              />
              <span>{{ friend.friendName }}</span>
              <span
                v-if="friend.onlineStatus === 1"
                style="color: #52c41a; font-size: 12px;"
              >
                (在线)
              </span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="分享留言">
        <el-input
          v-model="form.shareMessage"
          type="textarea"
          :rows="3"
          placeholder="可以添加一些说明（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="查询标题">
        <el-input v-model="form.queryTitle" disabled />
      </el-form-item>

      <el-form-item label="数据库">
        <el-input v-model="form.databaseName" disabled />
      </el-form-item>

      <el-form-item label="执行耗时">
        <el-input :value="formatExecutionTime(form.executionTime)" disabled />
      </el-form-item>

      <el-form-item label="SQL语句">
        <el-input
          v-model="form.sqlQuery"
          type="textarea"
          :rows="4"
          disabled
        />
      </el-form-item>

      <el-form-item label="包含数据">
        <el-checkbox v-model="includeTableData" :disabled="!hasTableData">
          表格数据 ({{ tableRowCount }} 行)
        </el-checkbox>
        <el-checkbox v-model="includeChartData" :disabled="!hasChartData">
          图表数据
        </el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          @click="handleShare"
          :loading="sharing"
          :disabled="!form.receiveUserId"
        >
          分享
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { queryShareChatApi, friendRelationApi } from '../services/api.real';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  queryData: {
    type: Object,
    required: true
  },
  userId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['update:modelValue', 'shared']);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const form = ref({
  receiveUserId: null,
  shareMessage: '',
  queryTitle: '',
  sqlQuery: '',
  databaseName: '',
  llmName: '',
  executionTime: 0,
  queryTime: '',
  dbConnectionId: null,
  llmConfigId: null,
  dialogId: ''
});

const friendList = ref([]);
const includeTableData = ref(true);
const includeChartData = ref(true);
const sharing = ref(false);

const hasTableData = computed(() => {
  return props.queryData.tableData && props.queryData.tableData.rows && props.queryData.tableData.rows.length > 0;
});

const hasChartData = computed(() => {
  return props.queryData.chartData != null;
});

const tableRowCount = computed(() => {
  if (!hasTableData.value) return 0;
  return props.queryData.tableData.rows.length;
});

// 监听弹窗打开，加载数据
watch(visible, async (newVal) => {
  if (newVal) {
    await loadFriendList();
    initFormData();
  }
});

// 加载好友列表
async function loadFriendList() {
  try {
    // 使用好友关系API获取好友列表
    const friends = await friendRelationApi.getByUser(props.userId);
    friendList.value = friends.map(friend => {
      // 尝试从friendUser获取用户信息（如果API返回了）
      const friendUser = (friend as any).friendUser;
      return {
        friendId: friend.friendId,
        friendName: friend.remarkName || friendUser?.username || `用户${friend.friendId}`,
        friendAvatar: friendUser?.avatarUrl || null,
        onlineStatus: friend.onlineStatus || 0
      };
    });
  } catch (error) {
    console.error('加载好友列表失败', error);
    ElMessage.error('加载好友列表失败');
  }
}

// 初始化表单数据
function initFormData() {
  form.value = {
    receiveUserId: null,
    shareMessage: '',
    queryTitle: props.queryData.userPrompt || props.queryData.queryTitle || '查询结果',
    sqlQuery: props.queryData.sqlQuery || '',
    databaseName: props.queryData.database || props.queryData.databaseName || '',
    llmName: props.queryData.model || props.queryData.llmName || '',
    executionTime: parseExecutionTime(props.queryData.executionTime),
    queryTime: props.queryData.queryTime || new Date().toISOString(),
    dbConnectionId: props.queryData.dbConnectionId,
    llmConfigId: props.queryData.llmConfigId,
    dialogId: props.queryData.conversationId || props.queryData.dialogId || ''
  };
}

// 解析执行时间
function parseExecutionTime(timeStr) {
  if (typeof timeStr === 'number') return timeStr;
  if (!timeStr) return 0;
  
  // 如果是 "123ms" 或 "1.23s" 格式
  if (timeStr.endsWith('ms')) {
    return parseInt(timeStr);
  }
  if (timeStr.endsWith('s')) {
    return parseFloat(timeStr) * 1000;
  }
  
  return parseInt(timeStr) || 0;
}

// 格式化执行时间
function formatExecutionTime(ms) {
  if (!ms) return '0ms';
  if (ms < 1000) return ms + 'ms';
  return (ms / 1000).toFixed(2) + 's';
}

// 处理分享
async function handleShare() {
  if (!form.value.receiveUserId) {
    ElMessage.warning('请选择要分享的好友');
    return;
  }

  sharing.value = true;

  try {
    const shareData = {
      shareUserId: props.userId,
      receiveUserId: form.value.receiveUserId,
      queryTitle: form.value.queryTitle,
      sqlQuery: form.value.sqlQuery,
      databaseName: form.value.databaseName,
      llmName: form.value.llmName,
      executionTime: form.value.executionTime,
      queryTime: form.value.queryTime,
      dbConnectionId: form.value.dbConnectionId,
      llmConfigId: form.value.llmConfigId,
      dialogId: form.value.dialogId,
      shareMessage: form.value.shareMessage
    };

    // 包含表格数据
    if (includeTableData.value && hasTableData.value) {
      shareData.tableData = {
        headers: props.queryData.tableData.headers,
        rows: props.queryData.tableData.rows
      };
    }

    // 包含图表数据
    if (includeChartData.value && hasChartData.value) {
      // 转换图表数据格式以匹配后端期望的格式
      const chartData = props.queryData.chartData;
      shareData.chartData = {
        type: chartData.type,
        xAxis: chartData.labels || chartData.xAxis,
        yAxis: chartData.datasets?.[0]?.data || chartData.yAxis,
        datasets: chartData.datasets
      };
    }

    const message = await queryShareChatApi.share(shareData);
    ElMessage.success('分享成功');
    emit('shared', message);
    handleClose();
  } catch (error) {
    console.error('分享失败', error);
    ElMessage.error(error instanceof Error ? error.message : '分享失败');
  } finally {
    sharing.value = false;
  }
}

// 关闭弹窗
function handleClose() {
  visible.value = false;
  form.value.receiveUserId = null;
  form.value.shareMessage = '';
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-textarea.is-disabled .el-textarea__inner) {
  background-color: #f5f5f5;
  color: #666;
}

:deep(.el-input.is-disabled .el-input__inner) {
  background-color: #f5f5f5;
  color: #666;
}
</style>

