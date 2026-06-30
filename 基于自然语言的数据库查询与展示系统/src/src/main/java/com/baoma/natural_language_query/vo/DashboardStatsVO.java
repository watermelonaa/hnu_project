package com.baoma.natural_language_query.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计数据VO
 */
@Data
public class DashboardStatsVO {
  
  // 系统管理员统计
  private Long totalUsers;           // 总用户数
  private Long totalDataSources;    // 数据源总数
  private Long todayQueries;        // 今日查询数
  private Long todayTokenUsage;     // 今日Token消耗
  private Long errorLogs;            // 异常日志数
  
  // 数据管理员统计
  private Long datasourceCount;     // 数据源总数
  private Long connectedCount;       // 当前连接数
  private Long errorCount;           // 连接错误数
  private Long pendingPermissions;  // 待处理权限请求数
  
  // 图表数据
  private List<Map<String, Object>> queryVolumeData;      // 查询量数据（24小时）
  private List<Map<String, Object>> responseTimeData;    // 响应时间数据（近7日）
  private List<Map<String, Object>> errorChartData;      // 错误类型分布
  private List<Map<String, Object>> costChartData;       // 成本分布
  private List<Map<String, Object>> healthStatusData;    // 健康状态分布
  private List<Map<String, Object>> queryLoadData;        // 查询负载数据
  
  // 列表数据
  private List<Map<String, Object>> recentFailures;      // 最近失败记录
  private List<Map<String, Object>> recentPermissionLogs; // 最近权限日志
}

