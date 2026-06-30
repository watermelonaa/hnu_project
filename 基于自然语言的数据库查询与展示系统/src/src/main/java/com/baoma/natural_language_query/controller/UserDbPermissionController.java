package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.entity.mysql.UserDbPermission;
import com.baoma.natural_language_query.service.UserDbPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-db-permission")
public class UserDbPermissionController {

  private static final Logger logger = LoggerFactory.getLogger(UserDbPermissionController.class);

  @Autowired private UserDbPermissionService userDbPermissionService;

  /** 查询所有权限 */
  @GetMapping("/list")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<UserDbPermission>> list() {
    return Result.success(userDbPermissionService.list());
  }

  /** 查询已分配权限的用户 */
  @GetMapping("/list/assigned")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<UserDbPermission>> listAssigned() {
    return Result.success(userDbPermissionService.listAssigned());
  }

  /** 查询未分配权限的用户 */
  @GetMapping("/list/unassigned")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<UserDbPermission>> listUnassigned() {
    return Result.success(userDbPermissionService.listUnassigned());
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<UserDbPermission> getById(@PathVariable Long id) {
    return Result.success(userDbPermissionService.getById(id));
  }

  /** 根据用户ID查询权限 */
  @GetMapping("/user/{userId}")
  @RequirePermission(Role.USER)
  public Result<UserDbPermission> getByUserId(@PathVariable Long userId) {
    return Result.success(userDbPermissionService.getByUserId(userId));
  }

  /** 添加或更新权限（修复版）- 推荐使用这个 */
  @PostMapping
  @RequirePermission(Role.DATA_ADMIN)
  public Result<UserDbPermission> save(@RequestBody UserDbPermission permission, HttpServletRequest request) {
    try {
      // 验证必填字段
      if (permission.getUserId() == null) {
        return Result.error("用户ID不能为空");
      }
      
      // 自动设置授权管理员ID
      Long currentUserId = (Long) request.getAttribute("userId");
      permission.setLastGrantUserId(currentUserId != null ? currentUserId : 1L);

      // 设置时间
      permission.setLastGrantTime(LocalDateTime.now());
      permission.setUpdateTime(LocalDateTime.now());
      if (permission.getIsAssigned() == null) {
        permission.setIsAssigned(1);
      }

      logger.info("开始保存权限，用户ID: {}", permission.getUserId());

      // 检查该用户是否已有权限记录
      UserDbPermission existing = userDbPermissionService.getByUserId(permission.getUserId());

      if (existing != null) {
        logger.info("用户ID {} 已有权限记录，更新现有记录 ID: {}", permission.getUserId(), existing.getId());

        // 保留ID，更新现有记录
        permission.setId(existing.getId());

        boolean success = userDbPermissionService.updateById(permission);
        logger.info("更新权限结果: {}", success);

        if (!success) {
          return Result.error("更新权限失败");
        }
      } else {
        logger.info("用户ID {} 没有权限记录，创建新记录", permission.getUserId());

        // 插入新记录
        boolean success = userDbPermissionService.save(permission);
        logger.info("插入权限结果: {}", success);

        if (!success) {
          return Result.error("保存权限失败");
        }
      }

      // 重新查询以获取完整的记录
      UserDbPermission saved = userDbPermissionService.getByUserId(permission.getUserId());
      if (saved == null) {
        return Result.error("保存成功但查询失败");
      }

      logger.info("保存成功，记录ID: {}", saved.getId());
      return Result.success(saved);
    } catch (Exception e) {
      logger.error("保存权限时发生错误", e);
      return Result.error("保存权限时发生错误: " + e.getMessage());
    }
  }

  /** 简化版保存方法 */
  @PostMapping("/simple")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<UserDbPermission> saveSimple(@RequestBody UserDbPermission permission, HttpServletRequest request) {
    try {
      // 验证必填字段
      if (permission.getUserId() == null) {
        return Result.error("用户ID不能为空");
      }
      
      // 自动设置授权管理员ID
      Long currentUserId = (Long) request.getAttribute("userId");
      permission.setLastGrantUserId(currentUserId != null ? currentUserId : 1L);

      // 设置时间
      permission.setLastGrantTime(LocalDateTime.now());
      permission.setUpdateTime(LocalDateTime.now());
      if (permission.getIsAssigned() == null) {
        permission.setIsAssigned(1);
      }

      logger.info("保存权限（简化版），用户ID: {}", permission.getUserId());

      // 使用saveOrUpdate方法
      boolean success = userDbPermissionService.saveOrUpdate(permission);
      logger.info("保存权限结果: {}", success);

      if (!success) {
        return Result.error("保存权限失败");
      }

      // 重新查询以获取完整的记录
      UserDbPermission saved = userDbPermissionService.getByUserId(permission.getUserId());
      if (saved == null) {
        return Result.error("保存成功但查询失败");
      }

      logger.info("保存成功，记录ID: {}", saved.getId());
      return Result.success(saved);
    } catch (Exception e) {
      logger.error("保存权限时发生错误", e);
      return Result.error("保存权限时发生错误: " + e.getMessage());
    }
  }

  /** 更新权限 */
  @PutMapping
  @RequirePermission(Role.DATA_ADMIN)
  public Result<UserDbPermission> update(@RequestBody UserDbPermission permission) {
    try {
      if (permission.getId() == null) {
        return Result.error("权限ID不能为空");
      }

      permission.setLastGrantTime(LocalDateTime.now());
      permission.setUpdateTime(LocalDateTime.now());

      logger.info("更新权限，ID: {}", permission.getId());
      boolean success = userDbPermissionService.updateById(permission);

      if (!success) {
        return Result.error("更新权限失败");
      }

      UserDbPermission updated = userDbPermissionService.getById(permission.getId());
      return Result.success(updated);
    } catch (Exception e) {
      logger.error("更新权限时发生错误", e);
      return Result.error("更新权限时发生错误: " + e.getMessage());
    }
  }

  /** 删除权限 */
  @DeleteMapping("/{id}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Void> delete(@PathVariable Long id) {
    try {
      logger.info("删除权限，ID: {}", id);
      boolean success = userDbPermissionService.removeById(id);

      if (!success) {
        return Result.error("删除权限失败");
      }

      return Result.success();
    } catch (Exception e) {
      logger.error("删除权限时发生错误", e);
      return Result.error("删除权限时发生错误: " + e.getMessage());
    }
  }

  /** 测试端点 */
  @PostMapping("/test")
  @RequirePermission(Role.ADMIN)
  public Result<UserDbPermission> testSave(HttpServletRequest request) {
    try {
      logger.info("开始测试权限保存功能");

      // 创建测试权限对象
      UserDbPermission testPermission = new UserDbPermission();
      testPermission.setUserId(5L); // 测试用户ID
      
      // 调用save方法
      return this.save(testPermission, request);
    } catch (Exception e) {
      logger.error("测试保存权限时发生错误", e);
      return Result.error("测试失败: " + e.getMessage());
    }
  }
}
