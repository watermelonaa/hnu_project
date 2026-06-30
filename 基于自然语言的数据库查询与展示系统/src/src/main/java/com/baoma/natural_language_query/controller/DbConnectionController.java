package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.DbConnection;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.DatabaseSchemaService;
import com.baoma.natural_language_query.service.DbConnectionService;
import com.baoma.natural_language_query.utils.PermissionUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baoma.natural_language_query.service.TablePermissionService;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/db-connection")
public class DbConnectionController {

  @Autowired private DbConnectionService dbConnectionService;
  @Autowired private TablePermissionService tablePermissionService;

  /** 查询所有数据库连接 */
  @GetMapping("/list")
  @RequirePermission(Role.USER)
  public Result<List<DbConnection>> list(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    Integer roleId = (Integer) request.getAttribute("roleId");

    // 管理员或数据管理员：直接返回所有
    if (roleId != null && (roleId == 1 || roleId == 2)) {
      return Result.success(dbConnectionService.list());
    }

    // 普通用户：只返回有权限的数据库
    Set<Long> allowedIds = tablePermissionService.getUserAccessibleDbIds(userId);
    List<DbConnection> filtered = dbConnectionService.list().stream()
        .filter(conn -> allowedIds.contains(conn.getId()))
        .collect(Collectors.toList());

    return Result.success(filtered);
  }

  /** 根据创建者ID查询数据库连接 */
  @GetMapping("/list/{createUserId}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<DbConnection>> listByUser(@PathVariable Long createUserId) {
    return Result.success(dbConnectionService.listByCreateUserId(createUserId));
  }

  /** 根据ID查询数据库连接 */
  @GetMapping("/{id}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<DbConnection> getById(@PathVariable Long id) {
    return Result.success(dbConnectionService.getById(id));
  }

  /** 添加数据库连接 */
  @PostMapping
  @RequirePermission(Role.DATA_ADMIN)
  public Result<DbConnection> save(
      @RequestBody DbConnection dbConnection, HttpServletRequest request) {
    // 验证必填字段
    if (dbConnection.getName() == null || dbConnection.getName().trim().isEmpty()) {
      return Result.error("连接名称不能为空");
    }
    if (dbConnection.getDbTypeId() == null) {
      return Result.error("数据库类型不能为空");
    }
    if (dbConnection.getUrl() == null || dbConnection.getUrl().trim().isEmpty()) {
      return Result.error("连接地址不能为空");
    }
    if (dbConnection.getUsername() == null || dbConnection.getUsername().trim().isEmpty()) {
      return Result.error("数据库账号不能为空");
    }
    if (dbConnection.getPassword() == null || dbConnection.getPassword().trim().isEmpty()) {
      return Result.error("数据库密码不能为空");
    }

    // 设置默认值
    dbConnection.setCreateTime(LocalDateTime.now());
    dbConnection.setUpdateTime(LocalDateTime.now());
    if (dbConnection.getStatus() == null) {
      dbConnection.setStatus("disconnected");
    }
    
    // 从请求属性中获取当前用户ID
    Long userId = (Long) request.getAttribute("userId");
    dbConnection.setCreateUserId(userId != null ? userId : 1L);

    try {
      dbConnectionService.save(dbConnection);
      return Result.success(dbConnection);
    } catch (DuplicateKeyException e) {
      return Result.error("连接名称 '" + dbConnection.getName() + "' 已存在，请换一个名称");
    } catch (Exception e) {
      return Result.error("保存失败: " + e.getMessage());
    }
  }

  /** 更新数据库连接 */
  @PutMapping
  @RequirePermission(Role.DATA_ADMIN)
  public Result<DbConnection> update(@RequestBody DbConnection dbConnection, HttpServletRequest request) {
    DbConnection existing = dbConnectionService.getById(dbConnection.getId());
    if (existing == null) {
      return Result.error("连接不存在");
    }
    
    // 权限检查：只有创建者或系统管理员可以修改
    if (!PermissionUtil.isOwner(request, existing.getCreateUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能修改自己创建的连接");
    }

    dbConnection.setUpdateTime(LocalDateTime.now());
    dbConnectionService.updateById(dbConnection);
    return Result.success(dbConnection);
  }

  /** 删除数据库连接 */
  @DeleteMapping("/{id}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    DbConnection existing = dbConnectionService.getById(id);
    if (existing == null) {
      return Result.error("连接不存在");
    }
    
    // 权限检查
    if (!PermissionUtil.isOwner(request, existing.getCreateUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能删除自己创建的连接");
    }

    dbConnectionService.removeById(id);
    return Result.success();
  }

  /** 测试数据库连接 */
  @GetMapping("/test/{id}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Boolean> testConnection(@PathVariable Long id, HttpServletRequest request) {
    DbConnection existing = dbConnectionService.getById(id);
    if (existing == null) {
      return Result.error("连接不存在");
    }
    
    // 权限检查
    if (!PermissionUtil.isOwner(request, existing.getCreateUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能测试自己创建的连接");
    }

    boolean result = dbConnectionService.testConnection(id);
    return Result.success(result);
  }

  @Autowired private DatabaseSchemaService databaseSchemaService;

  /** 获取数据源的所有表名 */
  @GetMapping("/{id}/tables")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<String>> getTables(@PathVariable Long id, HttpServletRequest request) {
    try {
      DbConnection connection = dbConnectionService.getById(id);
      if (connection == null) {
        return Result.error("数据源不存在");
      }
      
      // 权限检查
      if (!PermissionUtil.isOwner(request, connection.getCreateUserId()) && !PermissionUtil.isAdmin(request)) {
        return Result.error("权限不足，只能查看自己创建的数据源表信息");
      }
      
      // 验证连接配置
      if (connection.getUrl() == null || connection.getUrl().trim().isEmpty()) {
        return Result.error("数据源连接URL为空，请检查数据源配置");
      }
      if (connection.getUsername() == null || connection.getUsername().trim().isEmpty()) {
        return Result.error("数据源用户名为空，请检查数据源配置");
      }
      if (connection.getDbTypeId() == null) {
        return Result.error("数据源类型未设置，请检查数据源配置");
      }
      
      List<String> tables = databaseSchemaService.getTableNames(connection);
      return Result.success(tables);
    } catch (RuntimeException e) {
      // 捕获服务层抛出的异常，返回详细的错误信息
      return Result.error("加载表列表失败: " + e.getMessage());
    } catch (Exception e) {
      // 捕获其他未知异常
      return Result.error("加载表列表失败: " + e.getMessage());
    }
  }
}
