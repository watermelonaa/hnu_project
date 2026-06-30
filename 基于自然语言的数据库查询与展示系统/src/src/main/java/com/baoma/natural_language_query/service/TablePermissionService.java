package com.baoma.natural_language_query.service;

import java.util.Set;

/** 表权限检查服务接口 */
public interface TablePermissionService {

  /**
   * 检查用户是否有权限访问指定的表
   *
   * @param userId 用户ID
   * @param dbConnectionId 数据库连接ID
   * @param tableNames 要访问的表名集合
   * @return 权限检查结果
   */
  PermissionCheckResult checkTablePermissions(
      Long userId, Long dbConnectionId, Set<String> tableNames);

  /**
   * 检查用户是否有权限执行指定的SQL语句
   *
   * @param userId 用户ID
   * @param dbConnectionId 数据库连接ID
   * @param sql SQL语句
   * @return 权限检查结果
   */
  PermissionCheckResult checkSqlPermissions(Long userId, Long dbConnectionId, String sql);

  /**
   * 获取用户在指定数据库连接下的所有可访问表名
   * 
   * @param userId 用户ID
   * @param dbConnectionId 数据库连接ID
   * @return 可访问的表名集合
   */
  Set<String> getUserAccessibleTables(Long userId, Long dbConnectionId);

  /**
   * 获取用户有权访问的所有数据库连接ID
   * 
   * @param userId 用户ID
   * @return 数据库连接ID集合
   */
  Set<Long> getUserAccessibleDbIds(Long userId);

  /** 权限检查结果类 */
  public static class PermissionCheckResult {
    private boolean allowed;
    private String message;
    private Set<String> deniedTables;
    private Set<String> allowedTables;

    public PermissionCheckResult(boolean allowed, String message) {
      this.allowed = allowed;
      this.message = message;
    }

    public PermissionCheckResult(
        boolean allowed, String message, Set<String> deniedTables, Set<String> allowedTables) {
      this.allowed = allowed;
      this.message = message;
      this.deniedTables = deniedTables;
      this.allowedTables = allowedTables;
    }

    // Getters and Setters
    public boolean isAllowed() {
      return allowed;
    }

    public void setAllowed(boolean allowed) {
      this.allowed = allowed;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public Set<String> getDeniedTables() {
      return deniedTables;
    }

    public void setDeniedTables(Set<String> deniedTables) {
      this.deniedTables = deniedTables;
    }

    public Set<String> getAllowedTables() {
      return allowedTables;
    }

    public void setAllowedTables(Set<String> allowedTables) {
      this.allowedTables = allowedTables;
    }
  }
}
