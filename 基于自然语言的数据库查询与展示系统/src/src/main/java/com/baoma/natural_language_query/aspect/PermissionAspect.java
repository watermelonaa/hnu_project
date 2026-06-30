package com.baoma.natural_language_query.aspect;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.exception.BusinessException;
import com.baoma.natural_language_query.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限验证切面
 *
 * <p>拦截所有带{@link RequirePermission}注解的方法，验证用户权限。
 */
@Aspect
@Component
public class PermissionAspect {

  @Autowired private UserMapper userMapper;

  /**
   * 权限验证前置通知
   *
   * @param joinPoint 切入点
   * @param requirePermission 权限注解
   */
  @Before("@annotation(requirePermission)")
  public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
    // 获取当前请求
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      throw new BusinessException(401, "无法获取请求上下文");
    }

    HttpServletRequest request = attributes.getRequest();

    // 从request获取userId(由JwtInterceptor设置)
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(401, "未登录或登录已过期");
    }

    // 查询用户信息
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(401, "用户不存在");
    }

    // 检查用户状态
    if (user.getStatus() == 0) {
      throw new BusinessException(403, "账号已被禁用");
    }

    // 检查角色权限
    // 优先使用 value()，如果使用默认值则尝试 role()
    Role requiredRole = requirePermission.value();
    if (requiredRole == Role.USER && requirePermission.role() != Role.USER) {
      requiredRole = requirePermission.role();
    }
    
    Role userRole = Role.fromRoleId(user.getRoleId());

    // 角色层级：ADMIN(1) > DATA_ADMIN(2) > USER(3)
    // 如果用户角色的roleId <= 要求的角色的roleId，则有权限
    if (userRole.getRoleId() <= requiredRole.getRoleId()) {
      return;
    }

    // 权限不足
    throw new BusinessException(403, "权限不足：需要" + requiredRole.getRoleName() + "权限");
  }
}
