package com.baoma.natural_language_query.config;

import com.baoma.natural_language_query.service.impl.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security安全配置类
 *
 * <p>配置系统的安全策略，包括：
 *
 * <ul>
 *   <li>密码加密方式（BCrypt）
 *   <li>用户认证提供者
 *   <li>认证管理器
 *   <li>安全过滤器链（禁用CSRF、无状态会话、权限控制）
 * </ul>
 *
 * <p>当前配置说明：
 *
 * <ul>
 *   <li>禁用CSRF保护（因为使用JWT无状态认证）
 *   <li>使用无状态会话（STATELESS），不创建HttpSession
 *   <li>所有请求默认允许访问（实际权限控制由JWT拦截器处理）
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** 自定义用户详情服务（用于加载用户信息进行认证） */
  private final CustomUserDetailsService customUserDetailsService;

  /**
   * 构造函数注入（消除字段注入警告）
   *
   * @param customUserDetailsService 自定义用户详情服务
   */
  public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
    this.customUserDetailsService = customUserDetailsService;
  }

  /**
   * 配置密码编码器
   *
   * <p>使用BCrypt算法对密码进行加密，提供安全的密码存储。 BCrypt是单向哈希算法，每次加密结果不同，但可以验证密码正确性。
   *
   * @return BCrypt密码编码器实例
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * 配置认证提供者
   *
   * <p>设置用户详情服务和密码编码器，用于Spring Security的认证流程。
   *
   * @return 数据访问对象认证提供者
   */
  @Bean
  @SuppressWarnings("deprecation")
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(customUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  /**
   * 配置认证管理器
   *
   * <p>用于处理用户认证请求，由Spring Security自动配置。
   *
   * @param authConfig 认证配置对象
   * @return 认证管理器实例
   * @throws Exception 配置异常
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  /**
   * 配置安全过滤器链
   *
   * <p>定义系统的安全策略：
   *
   * <ul>
   *   <li>禁用CSRF保护（使用JWT无状态认证）
   *   <li>使用无状态会话管理（不创建HttpSession）
   *   <li>所有请求默认允许访问（实际权限由JWT拦截器控制）
   *   <li>注册认证提供者
   * </ul>
   *
   * @param http HttpSecurity配置对象
   * @return 安全过滤器链
   * @throws Exception 配置异常
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 禁用CSRF保护
        .csrf(CsrfConfigurer::disable)
        // 禁用HTTP Basic认证，防止浏览器弹出登录框
        .httpBasic(basic -> basic.disable())
        // 禁用表单登录
        .formLogin(form -> form.disable())
        // 配置会话管理为无状态
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 配置请求授权
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll() // 显式允许认证接口
            .requestMatchers("/**").permitAll())
        // 注册认证提供者
        .authenticationProvider(authenticationProvider());

    return http.build();
  }
}
