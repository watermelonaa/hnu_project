package com.baoma.natural_language_query.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 *
 * <p>配置Spring MVC的相关设置，包括：
 *
 * <ul>
 *   <li>注册拦截器（JWT认证拦截器）
 *   <li>配置拦截路径和排除路径
 * </ul>
 *
 * <p>拦截器配置说明：
 *
 * <ul>
 *   <li>默认拦截所有路径（/**）
 *   <li>排除路径：登录、注册、健康检查等公开接口
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /** JWT认证拦截器 */
  @Autowired @NonNull private JwtInterceptor jwtInterceptor;

  /**
   * 添加拦截器到拦截器注册表
   *
   * <p>配置JWT拦截器的拦截路径和排除路径：
   *
   * <ul>
   *   <li>拦截所有路径（/**）
   *   <li>排除路径：
   *       <ul>
   *         <li>/auth/** - 登录认证接口
   *         <li>/actuator/** - Spring Boot Actuator健康检查接口
   *         <li>/role - 角色相关接口（注册时）
   *         <li>/user - 用户注册接口
   *         <li>/query/** - 查询接口（开发环境，生产环境应移除）
   *         <li>/error - 错误处理接口
   *       </ul>
   * </ul>
   *
   * @param registry 拦截器注册表
   */
  @Override
  @SuppressWarnings("null")
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry
        .addInterceptor(jwtInterceptor)
        // 拦截所有路径
        .addPathPatterns("/**")
        // 排除不需要认证的路径
        .excludePathPatterns(
            "/auth/**", // 登录接口免认证
            "/actuator/**", // 健康检查接口免认证（开发环境）
            "/role", // 角色列表免认证
            "/error", // 错误处理接口
            "/test-exception/**" // 测试异常接口（仅用于测试）
            );
  }
}
