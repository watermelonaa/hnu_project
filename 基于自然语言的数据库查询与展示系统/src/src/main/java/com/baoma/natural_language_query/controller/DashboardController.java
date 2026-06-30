package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.DashboardService;
import com.baoma.natural_language_query.vo.DashboardStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘统计控制器
 * 
 * 提供系统管理员和数据管理员的仪表盘统计数据
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

  @Autowired
  private DashboardService dashboardService;

  /**
   * 获取系统管理员仪表盘统计数据
   */
  @GetMapping("/sysadmin")
  @RequirePermission(Role.ADMIN)
  public Result<DashboardStatsVO> getSysAdminStats() {
    return Result.success(dashboardService.getSysAdminStats());
  }

  /**
   * 获取数据管理员仪表盘统计数据
   */
  @GetMapping("/dataadmin")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<DashboardStatsVO> getDataAdminStats() {
    return Result.success(dashboardService.getDataAdminStats());
  }
}

