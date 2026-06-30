package com.baoma.natural_language_query.utils;

import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.exception.BusinessException;
import com.baoma.natural_language_query.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 权限验证工具类
 *
 * <p>提供常用的权限验证方法
 */
@Component
public class PermissionUtil {

  @Autowired private UserMapper userMapper;

  /**
   * 从请求中获取当前用户ID
   */
  public static Long getCurrentUserId(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(401, "未登录或登录已过期");
    }
    return userId;
  }

  /**
   * 检查是否是指定用户
   */
  public static boolean isCurrentUser(HttpServletRequest request, Long userId) {
    if (userId == null) return false;
    try {
      Long currentUserId = getCurrentUserId(request);
      return userId.equals(currentUserId);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 检查是否是资源所有者 (别名)
   */
  public static boolean isOwner(HttpServletRequest request, Long ownerId) {
    return isCurrentUser(request, ownerId);
  }

  /**
   * 检查是否是系统管理员
   */
  public static boolean isAdmin(HttpServletRequest request) {
    Integer roleId = (Integer) request.getAttribute("roleId");
    // 如果属性中没有，可能需要查库，但通常拦截器会设置
    return roleId != null && roleId == 1;
  }

  /**
   * 检查是否是数据管理员或更高
   */
  public static boolean isDataAdminOrAbove(HttpServletRequest request) {
    Integer roleId = (Integer) request.getAttribute("roleId");
    return roleId != null && roleId <= 2;
  }

  /**
   * 获取当前用户信息（非静态，需要查库）
   */
  public User getCurrentUser(HttpServletRequest request) {
    Long userId = getCurrentUserId(request);
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(401, "用户不存在");
    }
    return user;
  }
}
