package com.baoma.natural_language_query.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baoma.natural_language_query.entity.mysql.TableMetadata;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.entity.mysql.UserDbPermission;
import com.baoma.natural_language_query.service.TableMetadataService;
import com.baoma.natural_language_query.service.TablePermissionService;
import com.baoma.natural_language_query.service.UserDbPermissionService;
import com.baoma.natural_language_query.service.UserService;
import com.baoma.natural_language_query.utils.SqlTableExtractor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 表权限检查服务实现
 * 
 * <p>功能说明：
 * <ul>
 *   <li>检查用户是否有权限访问指定的数据库表</li>
 *   <li>数据管理员（roleId=2）默认拥有所有数据库和表的查询权限</li>
 *   <li>普通用户需要显式分配权限才能访问数据库表</li>
 * </ul>
 * 
 * @author 神奇宝码队
 * @version 1.0
 */
@Service
public class TablePermissionServiceImpl implements TablePermissionService {

  /** 数据管理员角色ID（从数据库schema可知，roleId=2对应数据管理员） */
  private static final Integer DATA_ADMIN_ROLE_ID = 2;

  @Autowired private UserDbPermissionService userDbPermissionService;

  @Autowired private UserService userService;

  @Autowired private TableMetadataService tableMetadataService;

  /**
   * 检查用户是否有权限访问指定的表
   * 
   * <p>权限检查逻辑：
   * <ol>
   *   <li>如果用户是数据管理员（roleId=2），默认允许访问所有表</li>
   *   <li>如果是普通用户，需要检查是否已分配权限，以及是否有权限访问指定的表</li>
   * </ol>
   * 
   * @param userId 用户ID
   * @param dbConnectionId 数据库连接ID
   * @param tableNames 要访问的表名集合
   * @return 权限检查结果
   */
  @Override
  public PermissionCheckResult checkTablePermissions(
      Long userId, Long dbConnectionId, Set<String> tableNames) {
    if (userId == null || dbConnectionId == null || tableNames == null || tableNames.isEmpty()) {
      return new PermissionCheckResult(true, "无需检查权限");
    }

    // 检查用户是否是数据管理员，如果是，默认允许访问所有表
    if (isDataAdmin(userId)) {
      return new PermissionCheckResult(
          true, "数据管理员默认拥有所有数据库和表的查询权限", new HashSet<>(), tableNames);
    }

    // 普通用户需要检查权限配置
    // 获取用户权限配置
    UserDbPermission userPermission = userDbPermissionService.getByUserId(userId);

    // 如果用户没有权限配置或未分配权限，拒绝访问
    if (userPermission == null || userPermission.getIsAssigned() != 1) {
      return new PermissionCheckResult(false, "用户未分配数据库访问权限");
    }

    // 解析权限详情
    Set<String> allowedTables =
        parseAllowedTables(userPermission.getPermissionDetails(), dbConnectionId);

    // 如果没有配置任何表权限，拒绝访问
    if (allowedTables.isEmpty()) {
      return new PermissionCheckResult(false, "用户在此数据库连接下未配置表访问权限");
    }

    // 检查每个表是否在允许列表中
    Set<String> deniedTables = new HashSet<>();
    Set<String> checkedAllowedTables = new HashSet<>();

    for (String tableName : tableNames) {
      String normalizedTableName = tableName.toLowerCase().trim();
      if (allowedTables.contains(normalizedTableName)) {
        checkedAllowedTables.add(normalizedTableName);
      } else {
        deniedTables.add(normalizedTableName);
      }
    }

    if (!deniedTables.isEmpty()) {
      String message =
          String.format(
              "用户无权限访问以下表: %s。允许访问的表: %s",
              String.join(", ", deniedTables), String.join(", ", allowedTables));
      return new PermissionCheckResult(false, message, deniedTables, checkedAllowedTables);
    }

    return new PermissionCheckResult(true, "权限检查通过", deniedTables, checkedAllowedTables);
  }

  /**
   * 检查用户是否有权限执行指定的SQL语句
   * 
   * <p>权限检查逻辑：
   * <ol>
   *   <li>首先检查SQL是否为只读操作（SELECT等），非只读操作一律拒绝</li>
   *   <li>如果用户是数据管理员，允许执行只读SQL</li>
   *   <li>如果是普通用户，需要检查是否有权限访问SQL中涉及的表</li>
   * </ol>
   * 
   * @param userId 用户ID
   * @param dbConnectionId 数据库连接ID
   * @param sql SQL语句
   * @return 权限检查结果
   */
  @Override
  public PermissionCheckResult checkSqlPermissions(Long userId, Long dbConnectionId, String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return new PermissionCheckResult(true, "空SQL语句，无需检查权限");
    }

    // 首先检查是否为只读操作（所有用户都必须遵守此规则）
    if (!SqlTableExtractor.isReadOnlyQuery(sql)) {
      return new PermissionCheckResult(false, "不允许执行非只读操作（INSERT/UPDATE/DELETE/DROP等）");
    }

    // 如果用户是数据管理员，允许执行所有只读SQL
    if (userId != null && isDataAdmin(userId)) {
      return new PermissionCheckResult(true, "数据管理员默认拥有所有数据库和表的查询权限");
    }

    // 提取SQL中的表名
    Set<String> tableNames = SqlTableExtractor.extractTableNames(sql);

    if (tableNames.isEmpty()) {
      return new PermissionCheckResult(true, "SQL语句中未检测到表名，允许执行");
    }

    // 检查表权限（普通用户）
    return checkTablePermissions(userId, dbConnectionId, tableNames);
  }

  /**
   * 获取用户在指定数据库连接下的所有可访问表名
   * 
   * <p>返回逻辑：
   * <ul>
   *   <li>如果用户是数据管理员，返回该数据库连接下的所有表</li>
   *   <li>如果是普通用户，返回已分配权限的表</li>
   * </ul>
   * 
   * @param userId 用户ID
   * @param dbConnectionId 数据库连接ID
   * @return 可访问的表名集合
   */
  @Override
  public Set<String> getUserAccessibleTables(Long userId, Long dbConnectionId) {
    if (userId == null || dbConnectionId == null) {
      return new HashSet<>();
    }

    // 如果用户是数据管理员，返回该数据库连接下的所有表
    if (isDataAdmin(userId)) {
      return getAllTablesByDbConnectionId(dbConnectionId);
    }

    // 普通用户返回已分配权限的表
    UserDbPermission userPermission = userDbPermissionService.getByUserId(userId);

    if (userPermission == null || userPermission.getIsAssigned() != 1) {
      return new HashSet<>();
    }

    return parseAllowedTables(userPermission.getPermissionDetails(), dbConnectionId);
  }

  @Override
  public Set<Long> getUserAccessibleDbIds(Long userId) {
    Set<Long> dbIds = new HashSet<>();
    if (userId == null) return dbIds;

    UserDbPermission userPermission = userDbPermissionService.getByUserId(userId);
    if (userPermission == null || userPermission.getIsAssigned() != 1 || 
        userPermission.getPermissionDetails() == null) {
      return dbIds;
    }

    try {
      JSONArray permissionArray = JSON.parseArray(userPermission.getPermissionDetails());
      for (Object item : permissionArray) {
        if (item instanceof JSONObject) {
          JSONObject permission = (JSONObject) item;
          Long dbId = permission.getLong("db_connection_id");
          if (dbId == null) dbId = permission.getLong("dbConnectionId");
          if (dbId != null) dbIds.add(dbId);
        }
      }
    } catch (Exception e) {
      System.err.println("解析用户权限DbIds失败: " + e.getMessage());
    }
    return dbIds;
  }

  /**
   * 判断用户是否是数据管理员
   * 
   * <p>数据管理员（roleId=2）默认拥有所有数据库和表的查询权限，无需显式分配权限
   * 
   * @param userId 用户ID
   * @return true表示是数据管理员，false表示不是
   */
  private boolean isDataAdmin(Long userId) {
    if (userId == null) {
      return false;
    }

    try {
      User user = userService.getById(userId);
      if (user == null) {
        return false;
      }

      // roleId=2 对应数据管理员角色（从数据库schema可知）
      return DATA_ADMIN_ROLE_ID.equals(user.getRoleId());
    } catch (Exception e) {
      System.err.println("检查用户角色失败: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 获取指定数据库连接下的所有表名
   * 
   * <p>从表元数据中查询该数据库连接下的所有表，用于数据管理员权限
   * 
   * @param dbConnectionId 数据库连接ID
   * @return 表名集合（小写，已规范化）
   */
  private Set<String> getAllTablesByDbConnectionId(Long dbConnectionId) {
    Set<String> allTables = new HashSet<>();

    try {
      // 从表元数据中获取该数据库连接下的所有表
      List<TableMetadata> tableMetadataList =
          tableMetadataService.listByDbConnectionId(dbConnectionId);

      // 提取表名并规范化（转小写、去空格）
      allTables =
          tableMetadataList.stream()
              .map(TableMetadata::getTableName)
              .filter(tableName -> tableName != null && !tableName.trim().isEmpty())
              .map(tableName -> tableName.toLowerCase().trim())
              .collect(Collectors.toSet());

    } catch (Exception e) {
      System.err.println("获取数据库表列表失败: " + e.getMessage());
      e.printStackTrace();
    }

    return allTables;
  }

  /**
   * 解析用户权限详情，提取指定数据库连接下的允许访问表名
   * 
   * <p>权限详情格式: [{"table_ids": [], "table_names": ["film"], "db_connection_id": 1}]
   * 
   * @param permissionDetails 权限详情JSON字符串
   * @param dbConnectionId 数据库连接ID
   * @return 允许访问的表名集合（小写，已规范化）
   */
  private Set<String> parseAllowedTables(String permissionDetails, Long dbConnectionId) {
    Set<String> allowedTables = new HashSet<>();

    if (permissionDetails == null || permissionDetails.trim().isEmpty()) {
      return allowedTables;
    }

    try {
      JSONArray permissionArray = JSON.parseArray(permissionDetails);

      for (Object item : permissionArray) {
        if (item instanceof JSONObject) {
          JSONObject permission = (JSONObject) item;

          // 检查数据库连接ID是否匹配 (增强容错：支持下划线和驼峰)
          Long permDbConnectionId = permission.getLong("db_connection_id");
          if (permDbConnectionId == null) {
            permDbConnectionId = permission.getLong("dbConnectionId");
          }

          if (dbConnectionId != null && dbConnectionId.equals(permDbConnectionId)) {

            // 获取表名列表 (增强容错：支持下划线和驼峰)
            JSONArray tableNamesArray = permission.getJSONArray("table_names");
            if (tableNamesArray == null) {
              tableNamesArray = permission.getJSONArray("tableNames");
            }
            if (tableNamesArray != null) {
              for (Object tableName : tableNamesArray) {
                if (tableName != null) {
                  allowedTables.add(tableName.toString().toLowerCase().trim());
                }
              }
            }

            // 注意：这里暂时忽略table_ids，因为需要额外的表ID到表名的映射
            // 如果需要支持table_ids，需要添加相应的服务来查询表ID对应的表名
          }
        }
      }

    } catch (Exception e) {
      System.err.println("解析用户权限详情失败: " + e.getMessage());
      e.printStackTrace();
    }

    return allowedTables;
  }
}
