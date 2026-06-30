package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.vo.DashboardStatsVO;

/**
 * 仪表盘统计服务接口
 */
public interface DashboardService {
  
  /**
   * 获取系统管理员仪表盘统计数据
   */
  DashboardStatsVO getSysAdminStats();
  
  /**
   * 获取数据管理员仪表盘统计数据
   */
  DashboardStatsVO getDataAdminStats();
}

