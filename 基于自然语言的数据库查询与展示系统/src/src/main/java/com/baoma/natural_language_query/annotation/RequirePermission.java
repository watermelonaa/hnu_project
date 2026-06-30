package com.baoma.natural_language_query.annotation;

import com.baoma.natural_language_query.enums.Role;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限验证注解
 *
 * <p>用于Controller方法上，标识该接口需要的最低权限级别。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

  /**
   * 需要的角色级别，默认为普通用户
   */
  Role value() default Role.USER;

  /**
   * 需要的角色级别（别名），默认为普通用户
   */
  Role role() default Role.USER;
}
