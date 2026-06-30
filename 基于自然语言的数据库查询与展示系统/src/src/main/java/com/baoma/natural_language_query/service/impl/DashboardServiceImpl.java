package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.DbConnection;
import com.baoma.natural_language_query.entity.mysql.DbConnectionLog;
import com.baoma.natural_language_query.entity.mysql.ErrorLog;
import com.baoma.natural_language_query.entity.mysql.OperationLog;
import com.baoma.natural_language_query.entity.mysql.QueryLog;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.entity.mysql.UserDbPermission;
import com.baoma.natural_language_query.mapper.DbConnectionLogMapper;
import com.baoma.natural_language_query.mapper.DbConnectionMapper;
import com.baoma.natural_language_query.mapper.ErrorLogMapper;
import com.baoma.natural_language_query.mapper.OperationLogMapper;
import com.baoma.natural_language_query.mapper.QueryLogMapper;
import com.baoma.natural_language_query.mapper.TokenConsumeMapper;
import com.baoma.natural_language_query.mapper.UserDbPermissionMapper;
import com.baoma.natural_language_query.mapper.UserMapper;
import com.baoma.natural_language_query.service.DashboardService;
import com.baoma.natural_language_query.vo.DashboardStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

  @Autowired
  private UserMapper userMapper;
  
  @Autowired
  private QueryLogMapper queryLogMapper;
  
  @Autowired
  private DbConnectionMapper dbConnectionMapper;
  
  @Autowired
  private DbConnectionLogMapper dbConnectionLogMapper;
  
  @Autowired
  private ErrorLogMapper errorLogMapper;
  
  @Autowired
  private TokenConsumeMapper tokenConsumeMapper;
  
  @Autowired
  private UserDbPermissionMapper userDbPermissionMapper;
  
  @Autowired
  private OperationLogMapper operationLogMapper;

  @Override
  public DashboardStatsVO getSysAdminStats() {
    DashboardStatsVO stats = new DashboardStatsVO();
    
    // 总用户数
    stats.setTotalUsers((long) userMapper.selectCount(null));
    
    // 数据源总数
    stats.setTotalDataSources((long) dbConnectionMapper.selectCount(null));
    
    // 今日查询数
    LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
    LambdaQueryWrapper<QueryLog> todayQueryWrapper = new LambdaQueryWrapper<>();
    todayQueryWrapper.ge(QueryLog::getQueryTime, todayStart);
    stats.setTodayQueries((long) queryLogMapper.selectCount(todayQueryWrapper));
    
    // 今日Token消耗
    stats.setTodayTokenUsage(getTodayTokenUsage());
    
    // 异常日志数
    stats.setErrorLogs((long) errorLogMapper.selectCount(null));
    
    // 查询量数据（24小时，每小时）
    stats.setQueryVolumeData(getQueryVolumeData24h());
    
    // 响应时间数据（近7日）
    stats.setResponseTimeData(getResponseTimeData7d());
    
    // 错误类型分布
    stats.setErrorChartData(getErrorChartData());
    
    // 成本分布（从数据库获取）
    stats.setCostChartData(getCostChartData());
    
    return stats;
  }

  @Override
  public DashboardStatsVO getDataAdminStats() {
    DashboardStatsVO stats = new DashboardStatsVO();
    
    // 数据源总数
    stats.setDatasourceCount((long) dbConnectionMapper.selectCount(null));
    
    // 当前连接数（状态为connected的数据源）
    LambdaQueryWrapper<DbConnection> connectedWrapper = new LambdaQueryWrapper<>();
    connectedWrapper.eq(DbConnection::getStatus, "connected");
    stats.setConnectedCount((long) dbConnectionMapper.selectCount(connectedWrapper));
    
    // 连接错误数（统计当前状态为error的数据源数量，而不是连接日志中的错误数）
    LambdaQueryWrapper<DbConnection> errorWrapper = new LambdaQueryWrapper<>();
    errorWrapper.eq(DbConnection::getStatus, "error");
    long errorCount = dbConnectionMapper.selectCount(errorWrapper);
    // 确保错误数不为负数
    stats.setErrorCount(errorCount >= 0 ? errorCount : 0L);
    
    /**
     * 待处理权限请求数（统计没有权限记录的用户数量，即待分配权限用户数）
     * 
     * <p>计算逻辑：
     * <ol>
     *   <li>统计所有普通用户（roleId = 3），排除系统管理员（roleId = 1）和数据管理员（roleId = 2）</li>
     *   <li>统计这些用户中已分配权限的用户数量（isAssigned=1）</li>
     *   <li>待分配权限用户数 = 普通用户总数 - 已分配权限用户数</li>
     * </ol>
     * 
     * <p>注意：数据管理员（roleId=2）默认拥有所有数据库和表的查询权限，不需要分配权限，
     * 因此不应该出现在"待分配权限用户"列表中，也不应该计入"待处理权限请求"数量。
     */
    // 普通用户角色ID（roleId=3）
    final Integer NORMAL_USER_ROLE_ID = 3;
    
    // 1. 统计所有普通用户（roleId = 3），排除系统管理员和数据管理员
    LambdaQueryWrapper<User> normalUserWrapper = new LambdaQueryWrapper<>();
    normalUserWrapper.eq(User::getRoleId, NORMAL_USER_ROLE_ID);
    List<User> normalUsers = userMapper.selectList(normalUserWrapper);
    long normalUserCount = normalUsers.size();
    
    // 2. 获取所有普通用户的userId列表
    List<Long> normalUserIds = normalUsers.stream()
        .map(User::getId)
        .collect(Collectors.toList());
    
    // 3. 统计普通用户中已分配权限的用户数量（isAssigned=1且userId在普通用户列表中）
    long assignedUserCount = 0L;
    if (!normalUserIds.isEmpty()) {
      LambdaQueryWrapper<UserDbPermission> assignedWrapper = new LambdaQueryWrapper<>();
      assignedWrapper.eq(UserDbPermission::getIsAssigned, 1);
      assignedWrapper.in(UserDbPermission::getUserId, normalUserIds);
      assignedUserCount = userDbPermissionMapper.selectCount(assignedWrapper);
    }
    
    // 4. 待分配权限用户数 = 普通用户总数 - 已分配权限用户数
    long pendingPermissions = normalUserCount - assignedUserCount;
    stats.setPendingPermissions(pendingPermissions >= 0 ? pendingPermissions : 0L);
    
    // 调试日志
    System.out.println("待处理权限请求统计: 普通用户数=" + normalUserCount + ", 已分配权限用户数=" + assignedUserCount + ", 待分配权限用户数=" + pendingPermissions);
    
    // 健康状态分布
    stats.setHealthStatusData(getHealthStatusData());
    
    // 查询负载数据
    stats.setQueryLoadData(getQueryLoadData());
    
    // 最近失败记录
    stats.setRecentFailures(getRecentFailures());
    
    // 最近权限日志（查询权限管理模块的最近操作记录）
    stats.setRecentPermissionLogs(getRecentPermissionLogs());
    
    return stats;
  }

  /**
   * 获取24小时查询量数据
   */
  private List<Map<String, Object>> getQueryVolumeData24h() {
    List<Map<String, Object>> result = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    
    for (int i = 23; i >= 0; i--) {
      LocalDateTime hourStart = now.minusHours(i).withMinute(0).withSecond(0).withNano(0);
      LocalDateTime hourEnd = hourStart.plusHours(1);
      
      LambdaQueryWrapper<QueryLog> wrapper = new LambdaQueryWrapper<>();
      wrapper.ge(QueryLog::getQueryTime, hourStart);
      wrapper.lt(QueryLog::getQueryTime, hourEnd);
      
      long count = queryLogMapper.selectCount(wrapper);
      
      Map<String, Object> data = new HashMap<>();
      data.put("label", hourStart.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
      data.put("value", count);
      result.add(data);
    }
    
    return result;
  }

  /**
   * 获取近7日响应时间数据
   */
  private List<Map<String, Object>> getResponseTimeData7d() {
    List<Map<String, Object>> result = new ArrayList<>();
    LocalDate today = LocalDate.now();
    
    for (int i = 6; i >= 0; i--) {
      LocalDate date = today.minusDays(i);
      LocalDateTime dayStart = date.atStartOfDay();
      LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
      
      LambdaQueryWrapper<QueryLog> wrapper = new LambdaQueryWrapper<>();
      wrapper.ge(QueryLog::getQueryTime, dayStart);
      wrapper.le(QueryLog::getQueryTime, dayEnd);
      
      List<QueryLog> logs = queryLogMapper.selectList(wrapper);
      
      // 计算平均响应时间（暂时使用固定值，因为QueryLog没有executionTime字段）
      // 实际应该从QueryResponseVO或DialogDetail中获取
      double avgTime = 0.0;
      if (!logs.isEmpty()) {
        // 暂时返回一个估算值，实际应该从查询结果中获取
        avgTime = 0.9; // 默认0.9秒
      }
      
      Map<String, Object> data = new HashMap<>();
      data.put("label", i == 0 ? "今天" : (i == 1 ? "昨天" : (6 - i + 1) + "天前"));
      data.put("value", Math.round(avgTime * 1000)); // 转换为毫秒
      result.add(data);
    }
    
    return result;
  }

  /**
   * 获取错误类型分布数据
   */
  private List<Map<String, Object>> getErrorChartData() {
    List<ErrorLog> errorLogs = errorLogMapper.selectList(null);
    
    // 按错误类型ID分组统计
    Map<Integer, Long> errorTypeCounts = errorLogs.stream()
        .collect(Collectors.groupingBy(
            log -> log.getErrorTypeId() != null ? log.getErrorTypeId() : 0,
            Collectors.counting()
        ));
    
    // 错误类型ID映射（可以根据实际情况调整）
    Map<Integer, String> errorTypeNames = new HashMap<>();
    errorTypeNames.put(1, "模型调用超时");
    errorTypeNames.put(2, "数据库连接错误");
    errorTypeNames.put(3, "用户认证失败");
    errorTypeNames.put(0, "其他");
    
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map.Entry<Integer, Long> entry : errorTypeCounts.entrySet()) {
      Map<String, Object> data = new HashMap<>();
      data.put("label", errorTypeNames.getOrDefault(entry.getKey(), "未知类型"));
      data.put("value", entry.getValue());
      result.add(data);
    }
    
    return result;
  }

  /**
   * 获取健康状态分布数据
   */
  private List<Map<String, Object>> getHealthStatusData() {
    List<DbConnection> connections = dbConnectionMapper.selectList(null);
    
    Map<String, Long> statusCounts = connections.stream()
        .collect(Collectors.groupingBy(
            conn -> {
              String status = conn.getStatus();
              if (status == null) return "未知";
              switch (status) {
                case "connected": return "已连接";
                case "disconnected": return "未连接";
                case "error": return "错误";
                case "testing": return "测试中";
                case "disabled": return "已禁用";
                default: return status;
              }
            },
            Collectors.counting()
        ));
    
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
      Map<String, Object> data = new HashMap<>();
      data.put("label", entry.getKey());
      data.put("value", entry.getValue());
      result.add(data);
    }
    
    return result;
  }

  /**
   * 获取查询负载数据
   */
  private List<Map<String, Object>> getQueryLoadData() {
    // 按数据源统计查询量
    List<QueryLog> queryLogs = queryLogMapper.selectList(null);
    
    Map<Long, Long> dbQueryCounts = queryLogs.stream()
        .filter(log -> log.getDataSourceId() != null)
        .collect(Collectors.groupingBy(
            QueryLog::getDataSourceId,
            Collectors.counting()
        ));
    
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map.Entry<Long, Long> entry : dbQueryCounts.entrySet()) {
      DbConnection db = dbConnectionMapper.selectById(entry.getKey());
      Map<String, Object> data = new HashMap<>();
      data.put("label", db != null ? db.getName() : "未知数据源");
      data.put("value", entry.getValue());
      result.add(data);
    }
    
    // 按查询量排序，取前10个
    result.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
    return result.stream().limit(10).collect(Collectors.toList());
  }

  /**
   * 获取最近失败记录
   */
  private List<Map<String, Object>> getRecentFailures() {
    LambdaQueryWrapper<DbConnectionLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(DbConnectionLog::getStatus, "error");
    wrapper.orderByDesc(DbConnectionLog::getConnectTime);
    wrapper.last("LIMIT 5");
    
    List<DbConnectionLog> logs = dbConnectionLogMapper.selectList(wrapper);
    
    return logs.stream().map(log -> {
      Map<String, Object> data = new HashMap<>();
      DbConnection db = dbConnectionMapper.selectById(log.getDbConnectionId());
      data.put("id", log.getId());
      data.put("datasource", db != null ? db.getName() : "未知数据源");
      data.put("time", log.getConnectTime());
      data.put("status", "失败");
      data.put("remark", log.getRemark());
      return data;
    }).collect(Collectors.toList());
  }
  
  /**
   * 获取今日Token消耗总量
   */
  private Long getTodayTokenUsage() {
    try {
      LocalDate today = LocalDate.now();
      com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.baoma.natural_language_query.entity.mysql.TokenConsume> wrapper = 
          new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
      wrapper.eq("consume_date", today);
      
      List<com.baoma.natural_language_query.entity.mysql.TokenConsume> todayConsumes = 
          tokenConsumeMapper.selectList(wrapper);
      
      if (todayConsumes == null || todayConsumes.isEmpty()) {
        return 0L;
      }
      
      // 累加所有LLM的今日token消耗
      return todayConsumes.stream()
          .mapToLong(consume -> consume.getTotalTokens() != null ? consume.getTotalTokens() : 0)
          .sum();
    } catch (Exception e) {
      System.err.println("获取今日Token消耗失败: " + e.getMessage());
      e.printStackTrace();
      return 0L;
    }
  }
  
  /**
   * 获取模型成本分布数据（今日）
   */
  private List<Map<String, Object>> getCostChartData() {
    try {
      LocalDate today = LocalDate.now();
      com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.baoma.natural_language_query.entity.mysql.TokenConsume> wrapper = 
          new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
      wrapper.eq("consume_date", today);
      
      List<com.baoma.natural_language_query.entity.mysql.TokenConsume> todayConsumes = 
          tokenConsumeMapper.selectList(wrapper);
      
      if (todayConsumes == null || todayConsumes.isEmpty()) {
        return new ArrayList<>();
      }
      
      // 按模型名称分组，累加每个模型的token消耗
      Map<String, Integer> modelPromptTokens = new HashMap<>();
      Map<String, Integer> modelCompletionTokens = new HashMap<>();
      
      for (com.baoma.natural_language_query.entity.mysql.TokenConsume consume : todayConsumes) {
        String llmName = consume.getLlmName();
        if (llmName == null || llmName.isEmpty()) {
          continue;
        }
        
        // 累加该模型的token
        int promptTokens = consume.getPromptTokens() != null ? consume.getPromptTokens() : 0;
        int completionTokens = consume.getCompletionTokens() != null ? consume.getCompletionTokens() : 0;
        
        modelPromptTokens.put(llmName, 
            modelPromptTokens.getOrDefault(llmName, 0) + promptTokens);
        modelCompletionTokens.put(llmName, 
            modelCompletionTokens.getOrDefault(llmName, 0) + completionTokens);
      }
      
      // 计算每个模型的成本并构建返回数据
      List<Map<String, Object>> result = new ArrayList<>();
      for (String llmName : modelPromptTokens.keySet()) {
        // 获取模型价格（每1000 tokens的USD价格）
        double inputPricePer1k = getInputPricePer1kTokens(llmName);
        double outputPricePer1k = getOutputPricePer1kTokens(llmName);
        
        // 获取该模型的token消耗
        int promptTokens = modelPromptTokens.getOrDefault(llmName, 0);
        int completionTokens = modelCompletionTokens.getOrDefault(llmName, 0);
        
        // 计算成本：成本 = (输入token数 / 1000) * 输入价格 + (输出token数 / 1000) * 输出价格
        double cost = (promptTokens / 1000.0) * inputPricePer1k + (completionTokens / 1000.0) * outputPricePer1k;
        
        // 只显示有成本的模型（成本大于0）
        if (cost > 0) {
          Map<String, Object> data = new HashMap<>();
          data.put("label", llmName);
          data.put("value", Math.round(cost * 100.0) / 100.0); // 保留两位小数
          result.add(data);
        }
      }
      
      // 按成本降序排序
      result.sort((a, b) -> Double.compare((Double) b.get("value"), (Double) a.get("value")));
      
      return result;
    } catch (Exception e) {
      System.err.println("获取模型成本数据失败: " + e.getMessage());
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
  
  /**
   * 获取模型的输入token价格（每1000 tokens的USD价格）
   */
  private double getInputPricePer1kTokens(String llmName) {
    if (llmName == null) {
      return 0.0;
    }
    
    String nameLower = llmName.toLowerCase();
    
    // 常见模型价格映射（每1000 tokens，USD）
    if (nameLower.contains("gpt-4")) {
      return 0.03; // GPT-4: $0.03 per 1K input tokens
    } else if (nameLower.contains("gpt-3.5") || nameLower.contains("gpt-3")) {
      return 0.0015; // GPT-3.5-turbo: $0.0015 per 1K input tokens
    } else if (nameLower.contains("gemini")) {
      return 0.00025; // Gemini: $0.00025 per 1K input tokens
    } else if (nameLower.contains("glm")) {
      return 0.001; // GLM-4: $0.001 per 1K input tokens
    } else if (nameLower.contains("qwen")) {
      return 0.0008; // Qwen: $0.0008 per 1K input tokens
    } else if (nameLower.contains("claude")) {
      return 0.008; // Claude: $0.008 per 1K input tokens
    }
    
    // 默认价格（如果模型名称不匹配）
    return 0.001;
  }
  
  /**
   * 获取模型的输出token价格（每1000 tokens的USD价格）
   */
  private double getOutputPricePer1kTokens(String llmName) {
    if (llmName == null) {
      return 0.0;
    }
    
    String nameLower = llmName.toLowerCase();
    
    // 常见模型价格映射（每1000 tokens，USD）
    if (nameLower.contains("gpt-4")) {
      return 0.06; // GPT-4: $0.06 per 1K output tokens
    } else if (nameLower.contains("gpt-3.5") || nameLower.contains("gpt-3")) {
      return 0.002; // GPT-3.5-turbo: $0.002 per 1K output tokens
    } else if (nameLower.contains("gemini")) {
      return 0.0005; // Gemini: $0.0005 per 1K output tokens
    } else if (nameLower.contains("glm")) {
      return 0.001; // GLM-4: $0.001 per 1K output tokens
    } else if (nameLower.contains("qwen")) {
      return 0.0008; // Qwen: $0.0008 per 1K output tokens
    } else if (nameLower.contains("claude")) {
      return 0.024; // Claude: $0.024 per 1K output tokens
    }
    
    // 默认价格（如果模型名称不匹配）
    return 0.001;
  }
  
  /**
   * 获取最近权限变更动态
   */
  private List<Map<String, Object>> getRecentPermissionLogs() {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(OperationLog::getModule, "权限管理");
    wrapper.orderByDesc(OperationLog::getOperateTime);
    wrapper.last("LIMIT 10");
    
    List<OperationLog> logs = operationLogMapper.selectList(wrapper);
    
    if (logs == null || logs.isEmpty()) {
      return new ArrayList<>();
    }
    
    return logs.stream().map(log -> {
      Map<String, Object> data = new HashMap<>();
      data.put("id", log.getId());
      // 构建显示文本，包含用户名和操作类型
      String username = log.getUsername() != null ? log.getUsername() : "未知用户";
      String operation = log.getOperation() != null ? log.getOperation() : "未知操作";
      // 构建友好的操作描述
      String actionText = operation;
      if (log.getResult() != null && log.getResult() == 0) {
        // 如果操作失败，显示失败信息
        actionText = operation + "（失败）";
      }
      data.put("text", String.format(
        "<span class='font-medium'>%s</span> 执行了 %s",
        username,
        actionText
      ));
      data.put("timestamp", log.getOperateTime() != null ? log.getOperateTime().toString() : "");
      return data;
    }).collect(Collectors.toList());
  }
}

