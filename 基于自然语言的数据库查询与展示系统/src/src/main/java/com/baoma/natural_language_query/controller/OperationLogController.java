package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.OperationLog;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.exception.BusinessException;
import com.baoma.natural_language_query.service.OperationLogService;
import com.baoma.natural_language_query.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operation-log")
public class OperationLogController {

  @Autowired private OperationLogService operationLogService;

  @Autowired private UserService userService;

  /**
   * 检查当前用户是否有系统管理员权限
   *
   * <p>系统日志只有系统管理员（roleId=1）可以访问
   * 数据管理员（roleId=2）和普通用户（roleId=3）都不能访问
   *
   * @param request HTTP请求对象
   * @return true表示有权限，false表示无权限
   */
  private boolean hasAdminPermission(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return false;
    }
    try {
      User user = userService.getById(userId);
      if (user == null) {
        return false;
      }
      // 只有系统管理员（roleId=1）可以访问系统日志
      // 数据管理员（roleId=2）和普通用户（roleId=3）都不能访问
      Integer roleId = user.getRoleId();
      return roleId != null && roleId == 1;
    } catch (Exception e) {
      return false;
    }
  }

  /** 查询所有操作日志（需要系统管理员权限） */
  @GetMapping("/list")
  public Result<List<OperationLog>> list(HttpServletRequest request) {
    if (!hasAdminPermission(request)) {
      throw new BusinessException(403, "无权限访问：仅系统管理员可查看系统日志");
    }
    return Result.success(operationLogService.list());
  }

  /** 根据时间范围查询操作日志（需要系统管理员权限） */
  @GetMapping("/list/time-range")
  public Result<List<OperationLog>> listByTimeRange(
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      HttpServletRequest request) {
    if (!hasAdminPermission(request)) {
      throw new BusinessException(403, "无权限访问：仅系统管理员可查看系统日志");
    }
    LocalDateTime start = null;
    LocalDateTime end = null;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    try {
      if (startTime != null && !startTime.isEmpty()) {
        if (startTime.length() == 10) {
          start = LocalDateTime.parse(startTime + " 00:00:00", formatter);
        } else {
          start = LocalDateTime.parse(startTime, formatter);
        }
      }
      if (endTime != null && !endTime.isEmpty()) {
        if (endTime.length() == 10) {
          end = LocalDateTime.parse(endTime + " 23:59:59", formatter);
        } else {
          end = LocalDateTime.parse(endTime, formatter);
        }
      }
    } catch (Exception e) {
      throw new BusinessException(400, "时间格式错误，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss 格式");
    }
    return Result.success(operationLogService.listByTimeRange(start, end));
  }

  /** 根据用户ID查询操作日志（需要系统管理员权限） */
  @GetMapping("/list/user/{userId}")
  public Result<List<OperationLog>> listByUser(
      @PathVariable Long userId, HttpServletRequest request) {
    if (!hasAdminPermission(request)) {
      throw new BusinessException(403, "无权限访问：仅系统管理员可查看系统日志");
    }
    return Result.success(operationLogService.listByUserId(userId));
  }

  /** 根据模块查询操作日志（需要系统管理员权限） */
  @GetMapping("/list/module/{module}")
  public Result<List<OperationLog>> listByModule(
      @PathVariable String module, HttpServletRequest request) {
    if (!hasAdminPermission(request)) {
      throw new BusinessException(403, "无权限访问：仅系统管理员可查看系统日志");
    }
    return Result.success(operationLogService.listByModule(module));
  }

  /** 查询失败的操作日志（需要系统管理员权限） */
  @GetMapping("/list/failed")
  public Result<List<OperationLog>> listFailed(HttpServletRequest request) {
    if (!hasAdminPermission(request)) {
      throw new BusinessException(403, "无权限访问：仅系统管理员可查看系统日志");
    }
    return Result.success(operationLogService.listFailed());
  }

  /** 根据ID查询（需要系统管理员权限） */
  @GetMapping("/{id}")
  public Result<OperationLog> getById(@PathVariable Long id, HttpServletRequest request) {
    if (!hasAdminPermission(request)) {
      throw new BusinessException(403, "无权限访问：仅系统管理员可查看系统日志");
    }
    return Result.success(operationLogService.getById(id));
  }

  /** 添加操作日志 */
  @PostMapping
  public Result<OperationLog> save(
      @RequestBody OperationLog operationLog, HttpServletRequest request) {
    // 设置操作时间
    if (operationLog.getOperateTime() == null) {
      operationLog.setOperateTime(LocalDateTime.now());
    }

    // 从request attribute中获取用户信息（由JwtInterceptor设置）
    Long requestUserId = (Long) request.getAttribute("userId");
    String requestUsername = (String) request.getAttribute("username");

    // 如果operationLog中没有userId，从request中获取
    if (operationLog.getUserId() == null && requestUserId != null) {
      operationLog.setUserId(requestUserId);
    }

    // 如果operationLog中没有username，尝试从request或通过userId查询
    if (operationLog.getUsername() == null || operationLog.getUsername().isEmpty()) {
      if (requestUsername != null && !requestUsername.isEmpty()) {
        operationLog.setUsername(requestUsername);
      } else if (operationLog.getUserId() != null) {
        // 通过userId查询username
        User user = userService.getById(operationLog.getUserId());
        if (user != null) {
          operationLog.setUsername(user.getUsername());
        }
      }
    }

    // 如果仍然没有username，使用默认值
    if (operationLog.getUsername() == null || operationLog.getUsername().isEmpty()) {
      operationLog.setUsername("unknown");
    }

    // 设置module默认值（必需字段）
    if (operationLog.getModule() == null || operationLog.getModule().isEmpty()) {
      operationLog.setModule("未知模块");
    }

    // 设置operation默认值（必需字段）
    if (operationLog.getOperation() == null || operationLog.getOperation().isEmpty()) {
      operationLog.setOperation("未知操作");
    }

    // 设置result默认值
    if (operationLog.getResult() == null) {
      operationLog.setResult(1); // 默认成功
    }

    // 设置ipAddress默认值（如果为null）
    if (operationLog.getIpAddress() == null || operationLog.getIpAddress().isEmpty()) {
      // 尝试从request中获取IP地址
      String ipAddress = getClientIpAddress(request);
      operationLog.setIpAddress(ipAddress != null ? ipAddress : "unknown");
    }

    operationLogService.save(operationLog);
    return Result.success(operationLog);
  }

  /** 删除操作日志（需要系统管理员权限） */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    if (!hasAdminPermission(request)) {
      throw new BusinessException(403, "无权限访问：仅系统管理员可删除系统日志");
    }
    operationLogService.removeById(id);
    return Result.success();
  }

  /** 获取客户端IP地址 */
  private String getClientIpAddress(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    // 如果是多级代理，取第一个IP
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}
