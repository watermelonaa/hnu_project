<template>
  <div class="query-share-card">
    <div class="share-header">
      <div class="share-info">
        <img :src="shareData.shareUserAvatar || '/default-avatar.png'" class="avatar" />
        <div class="info-text">
          <span class="username">{{ shareData.shareUserName }}</span>
          <span class="share-time">分享于 {{ formatTime(shareData.shareTime) }}</span>
        </div>
      </div>
      <div class="share-actions" v-if="!readonly">
        <button @click="saveQuery" class="btn-save" v-if="shareData.receiveStatus === 0">
          <i class="icon">💾</i> 保存
        </button>
        <button @click="deleteQuery" class="btn-delete" v-if="shareData.receiveStatus === 0">
          <i class="icon">🗑️</i> 删除
        </button>
        <span class="status-badge saved" v-if="shareData.receiveStatus === 1">已保存</span>
        <span class="status-badge deleted" v-if="shareData.receiveStatus === 2">已删除</span>
      </div>
    </div>

    <div class="share-content">
      <!-- 分享留言 -->
      <div class="share-message" v-if="shareData.shareMessage">
        <i class="icon">💬</i>
        <span>{{ shareData.shareMessage }}</span>
      </div>

      <!-- 查询标题 -->
      <div class="query-title">
        <h3>{{ shareData.queryTitle }}</h3>
      </div>

      <!-- 查询元信息 -->
      <div class="query-meta">
        <div class="meta-item">
          <span class="label">数据库：</span>
          <span class="value">{{ shareData.databaseName }}</span>
        </div>
        <div class="meta-item">
          <span class="label">大模型：</span>
          <span class="value">{{ shareData.llmName }}</span>
        </div>
        <div class="meta-item">
          <span class="label">执行耗时：</span>
          <span class="value">{{ shareData.executionTimeText }}</span>
        </div>
        <div class="meta-item">
          <span class="label">查询时间：</span>
          <span class="value">{{ shareData.queryTime }}</span>
        </div>
      </div>

      <!-- SQL语句 -->
      <div class="sql-section">
        <div class="section-header">
          <span class="section-title">SQL语句</span>
          <button @click="copySql" class="btn-copy">
            <i class="icon">📋</i> 复制
          </button>
        </div>
        <pre class="sql-code">{{ shareData.sqlQuery }}</pre>
      </div>

      <!-- 查看结果按钮 -->
      <div class="view-actions">
        <button @click="viewTableData" class="btn-view" v-if="shareData.tableData">
          <i class="icon">📊</i> 查看表格数据
        </button>
        <button @click="viewChartData" class="btn-view" v-if="shareData.chartData">
          <i class="icon">📈</i> 查看图表数据
        </button>
      </div>
    </div>

    <!-- 表格数据弹窗 -->
    <el-dialog v-model="showTableDialog" title="表格数据" width="80%">
      <div class="table-container" v-if="shareData.tableData">
        <div class="table-info">
          共 {{ shareData.tableData.totalRows }} 行数据
        </div>
        <el-table :data="tableRows" border stripe max-height="500">
          <el-table-column
            v-for="(header, index) in shareData.tableData.headers"
            :key="index"
            :prop="'col' + index"
            :label="header"
            min-width="120"
          />
        </el-table>
      </div>
    </el-dialog>

    <!-- 图表数据弹窗 -->
    <el-dialog v-model="showChartDialog" title="图表数据" width="70%">
      <div class="chart-container" v-if="shareData.chartData">
        <div ref="chartRef" style="width: 100%; height: 400px;"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { queryShareChatApi } from '../services/api.real';
import * as echarts from 'echarts';

const props = defineProps({
  shareData: {
    type: Object,
    required: true
  },
  readonly: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['save', 'delete', 'update']);

const showTableDialog = ref(false);
const showChartDialog = ref(false);
const chartRef = ref(null);
let chartInstance = null;

// 转换表格数据为el-table格式
const tableRows = computed(() => {
  if (!props.shareData.tableData || !props.shareData.tableData.rows) {
    return [];
  }
  return props.shareData.tableData.rows.map(row => {
    const obj = {};
    row.forEach((cell, index) => {
      obj['col' + index] = cell;
    });
    return obj;
  });
});

// 格式化时间
function formatTime(timeStr) {
  if (!timeStr) return '';
  const time = new Date(timeStr);
  const now = new Date();
  const diff = now - time;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
  if (diff < 172800000) return '昨天';

  return time.toLocaleDateString('zh-CN');
}

// 复制SQL
function copySql() {
  navigator.clipboard.writeText(props.shareData.sqlQuery).then(() => {
    ElMessage.success('SQL已复制到剪贴板');
  }).catch(() => {
    ElMessage.error('复制失败');
  });
}

// 查看表格数据
function viewTableData() {
  showTableDialog.value = true;
}

// 查看图表数据
function viewChartData() {
  showChartDialog.value = true;
  setTimeout(() => {
    renderChart();
  }, 100);
}

// 渲染图表
function renderChart() {
  if (!chartRef.value || !props.shareData.chartData) return;

  if (chartInstance) {
    chartInstance.dispose();
  }

  chartInstance = echarts.init(chartRef.value);

  const chartData = props.shareData.chartData;
  const option = {
    title: {
      text: props.shareData.queryTitle
    },
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: chartData.xAxis
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      data: chartData.yAxis,
      type: chartData.type || 'bar'
    }]
  };

  chartInstance.setOption(option);
}

// 保存查询
async function saveQuery() {
  try {
    await ElMessageBox.confirm('确定要保存这个查询分享吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    });

    const result = await queryShareChatApi.save(
      props.shareData.shareId,
      props.shareData.receiveUserId
    );

    if (result.success) {
      ElMessage.success(result.message || '保存成功');
      emit('save', props.shareData.shareId);
      emit('update');
    } else {
      ElMessage.error(result.message || '保存失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('保存失败', error);
      ElMessage.error(error instanceof Error ? error.message : '保存失败');
    }
  }
}

// 删除查询
async function deleteQuery() {
  try {
    await ElMessageBox.confirm('确定要删除这个查询分享吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const result = await queryShareChatApi.delete(
      props.shareData.shareId,
      props.shareData.receiveUserId
    );

    if (result.success) {
      ElMessage.success(result.message || '删除成功');
      emit('delete', props.shareData.shareId);
      emit('update');
    } else {
      ElMessage.error(result.message || '删除失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error);
      ElMessage.error(error instanceof Error ? error.message : '删除失败');
    }
  }
}

// 监听弹窗关闭，清理图表实例
watch(showChartDialog, (newVal) => {
  if (!newVal && chartInstance) {
    chartInstance.dispose();
    chartInstance = null;
  }
});
</script>

<style scoped>
.query-share-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  margin: 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.share-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.share-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.info-text {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 500;
  font-size: 14px;
  color: #333;
}

.share-time {
  font-size: 12px;
  color: #999;
}

.share-actions {
  display: flex;
  gap: 8px;
}

.btn-save,
.btn-delete,
.btn-copy,
.btn-view {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-save {
  background: #1890ff;
  color: white;
}

.btn-save:hover {
  background: #40a9ff;
}

.btn-delete {
  background: #ff4d4f;
  color: white;
}

.btn-delete:hover {
  background: #ff7875;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.status-badge.saved {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.status-badge.deleted {
  background: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.share-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.share-message {
  background: #f0f7ff;
  padding: 12px;
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.query-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.query-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
}

.meta-item {
  font-size: 13px;
}

.meta-item .label {
  color: #999;
  margin-right: 4px;
}

.meta-item .value {
  color: #333;
  font-weight: 500;
}

.sql-section {
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #e8e8e8;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: #666;
}

.btn-copy {
  background: white;
  border: 1px solid #d9d9d9;
}

.btn-copy:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.sql-code {
  margin: 0;
  padding: 12px;
  background: #f5f5f5;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #333;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.view-actions {
  display: flex;
  gap: 12px;
}

.btn-view {
  flex: 1;
  background: white;
  border: 1px solid #d9d9d9;
  padding: 10px;
  justify-content: center;
}

.btn-view:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.icon {
  font-style: normal;
}

.table-container {
  max-height: 600px;
  overflow: auto;
}

.table-info {
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;
}

.chart-container {
  padding: 20px;
}

@media (max-width: 768px) {
  .query-meta {
    grid-template-columns: 1fr;
  }

  .view-actions {
    flex-direction: column;
  }
}
</style>

