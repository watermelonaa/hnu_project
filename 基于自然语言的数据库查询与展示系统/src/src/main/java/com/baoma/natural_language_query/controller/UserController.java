package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.ChangePasswordDTO;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.service.UserService;
import com.baoma.natural_language_query.utils.PermissionUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 *
 * <p>提供用户管理的RESTful API接口，包括：
 *
 * <ul>
 *   <li>用户CRUD操作（创建、查询、更新、删除）
 *   <li>用户搜索（按用户名、邮箱、手机号）
 *   <li>用户密码修改
 *   <li>分页查询用户列表
 * </ul>
 *
 * <p>权限说明：
 *
 * <ul>
 *   <li>普通用户：只能查看和修改自己的信息
 *   <li>管理员：可以管理所有用户
 * </ul>
 */
@RestController
@RequestMapping("/user")
public class UserController {

  @Autowired private UserService userService;

  @GetMapping("/{id}")
  @RequirePermission(Role.USER)
  public Result<User> getById(@PathVariable Long id, HttpServletRequest request) {
    User user = userService.getById(id);
    if (user != null) {
      // 敏感信息处理：如果不是本人且不是管理员，屏蔽敏感字段
      if (!PermissionUtil.isCurrentUser(request, id) && !PermissionUtil.isAdmin(request)) {
        user.setPassword(null);
        // 保留用户名和头像，这些是社交功能必需的
      }
      return Result.success(user);
    }
    return Result.error("用户不存在");
  }

  @GetMapping("/list")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<User>> list() {
    List<User> users = userService.list();
    return Result.success(users);
  }

  @GetMapping("/page")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Page<User>> page(
      @RequestParam(defaultValue = "1") int current, @RequestParam(defaultValue = "10") int size) {
    Page<User> page = userService.page(current, size);
    return Result.success(page);
  }

  @PostMapping
  @RequirePermission(Role.ADMIN)
  public Result<String> save(@RequestBody User user) {
    try {
      // 验证邮箱格式
      if (user.getEmail() != null && !user.getEmail().isEmpty()) {
        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
          return Result.error("邮箱格式不正确");
        }
      }
      
      // 检查用户名是否重复
      User existingUser = userService.getByUsername(user.getUsername());
      if (existingUser != null) {
        return Result.error("用户名已存在，请使用其他用户名");
      }
      
      // 检查邮箱是否重复
      if (user.getEmail() != null && !user.getEmail().isEmpty()) {
        List<User> emailUsers = userService.searchByEmail(user.getEmail());
        if (emailUsers != null && !emailUsers.isEmpty()) {
          return Result.error("该邮箱已被使用，请使用其他邮箱");
        }
      }
      
      // 检查手机号是否重复
      if (user.getPhonenumber() != null && !user.getPhonenumber().isEmpty()) {
        User phoneUser = userService.searchByPhoneNumber(user.getPhonenumber());
        if (phoneUser != null) {
          return Result.error("该手机号已被使用，请使用其他手机号");
        }
      }
      
      boolean success = userService.save(user);
      if (success) {
        return Result.success("添加成功");
      }
      return Result.error("添加失败");
    } catch (Exception e) {
      return Result.error("添加失败: " + e.getMessage());
    }
  }

  @PutMapping
  @RequirePermission(Role.USER)
  public Result<String> update(@RequestBody User user, HttpServletRequest request) {
    // 普通用户只能更新自己的信息，管理员可以更新所有人
    if (!PermissionUtil.isCurrentUser(request, user.getId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能修改自己的信息");
    }

    try {
      // 验证邮箱格式
      if (user.getEmail() != null && !user.getEmail().isEmpty()) {
        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
          return Result.error("邮箱格式不正确");
        }
      }
      
      // 检查邮箱是否被其他用户使用
      if (user.getEmail() != null && !user.getEmail().isEmpty()) {
        List<User> emailUsers = userService.searchByEmail(user.getEmail());
        if (emailUsers != null) {
          for (User emailUser : emailUsers) {
            if (!emailUser.getId().equals(user.getId())) {
              return Result.error("该邮箱已被其他用户使用");
            }
          }
        }
      }
      
      // 检查手机号是否被其他用户使用
      if (user.getPhonenumber() != null && !user.getPhonenumber().isEmpty()) {
        User phoneUser = userService.searchByPhoneNumber(user.getPhonenumber());
        if (phoneUser != null && !phoneUser.getId().equals(user.getId())) {
          return Result.error("该手机号已被其他用户使用");
        }
      }
      
      boolean success = userService.updateById(user);
      if (success) {
        return Result.success("更新成功");
      }
      return Result.error("更新失败");
    } catch (Exception e) {
      return Result.error("更新失败: " + e.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  @RequirePermission(Role.ADMIN)
  public Result<String> delete(@PathVariable Long id) {
    boolean success = userService.removeById(id);
    if (success) {
      return Result.success("删除成功");
    }
    return Result.error("删除失败");
  }

  @GetMapping("/username/{username}")
  @RequirePermission(Role.USER)
  public Result<User> getByUsername(@PathVariable String username) {
    User user = userService.getByUsername(username);
    if (user != null) {
      return Result.success(user);
    }
    return Result.error("用户不存在");
  }

  @GetMapping("/search/email")
  @RequirePermission(Role.USER)
  public Result<List<User>> searchByEmail(@RequestParam String email) {
    List<User> users = userService.searchByEmail(email);
    return Result.success(users);
  }

  @GetMapping("/search/phone")
  @RequirePermission(Role.USER)
  public Result<User> searchByPhoneNumber(@RequestParam String phoneNumber) {
    User user = userService.searchByPhoneNumber(phoneNumber);
    if (user != null) {
      return Result.success(user);
    }
    return Result.error("未找到该用户");
  }

  @PostMapping("/change-password")
  @RequirePermission(Role.USER)
  public Result<String> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO, HttpServletRequest request) {
    // 普通用户只能修改自己的密码，管理员可以修改任何人的密码
    if (!PermissionUtil.isCurrentUser(request, changePasswordDTO.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能修改自己的密码");
    }

    try {
      boolean success =
          userService.changePassword(
              changePasswordDTO.getUserId(),
              changePasswordDTO.getOldPassword(),
              changePasswordDTO.getNewPassword());
      if (success) {
        return Result.success("密码修改成功");
      }
      return Result.error("密码修改失败");
    } catch (RuntimeException e) {
      return Result.error(e.getMessage());
    }
  }

  /**
   * 系统管理员直接重置用户密码
   */
  @PostMapping("/reset-password/{id}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<String> resetPassword(@PathVariable Long id) {
    System.out.println("收到重置密码请求，用户ID: " + id);
    try {
      // 默认重置密码为 123456
      boolean success = userService.changePassword(id, null, "123456");
      System.out.println("重置密码结果: " + success);
      if (success) {
        return Result.success("密码已成功重置为：123456");
      }
      return Result.error("重置密码失败");
    } catch (Exception e) {
      System.err.println("重置密码异常: " + e.getMessage());
      e.printStackTrace();
      return Result.error("重置密码失败: " + e.getMessage());
    }
  }
}
