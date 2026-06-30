package com.baoma.natural_language_query.config;

import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.mapper.UserMapper;
import com.baoma.natural_language_query.service.TokenBlacklistService;
import com.baoma.natural_language_query.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT令牌拦截器
 *
 * <p>在请求处理前拦截HTTP请求，验证JWT令牌的有效性。 如果令牌有效，从令牌中提取用户信息并设置到请求属性中，供后续处理使用。
 *
 * <p>认证流程：
 *
 * <ol>
 *   <li>允许OPTIONS请求（CORS预检请求）直接通过
 *   <li>从Authorization请求头中提取Bearer Token
 *   <li>验证Token有效性（包括检查黑名单），如果有效则提取用户信息
 *   <li>检查用户账号状态，如果被禁用则返回403禁止访问
 *   <li>开发环境支持从userId请求头获取用户ID（生产环境应移除）
 *   <li>查询接口允许无Token访问（使用默认userId，生产环境应移除）
 *   <li>其他情况返回401未授权
 * </ol>
 *
 * <p>注意：生产环境应移除开发环境的特殊处理逻辑，严格验证Token。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

  /** JWT工具类，用于验证和解析JWT令牌 */
  @Autowired private JwtUtil jwtUtil;

  /** Token黑名单服务，用于检查token是否已被注销 */
  @Autowired private TokenBlacklistService tokenBlacklistService;

  /** 用户数据访问对象，用于检查用户状态 */
  @Autowired private UserMapper userMapper;

  /**
   * 请求处理前的拦截方法
   *
   * <p>验证JWT令牌，如果有效则提取用户信息并设置到请求属性中。
   *
   * @param request HTTP请求对象
   * @param response HTTP响应对象
   * @param handler 处理器对象
   * @return true表示继续处理请求，false表示中断请求
   * @throws Exception 处理异常
   */
  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler)
      throws Exception {
    // 允许OPTIONS请求（CORS预检请求）直接通过
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    // 从Authorization请求头中提取Bearer Token
    String authHeader = request.getHeader("Authorization");
    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      // 验证Token有效性（包括签名、过期时间等）
      if (jwtUtil.validateToken(token)) {
        // 检查token是否在黑名单中（已被注销或重复登录时被替换）
        if (tokenBlacklistService.isBlacklisted(token)) {
          // Token已被加入黑名单，返回401未授权
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
          return false;
        }
        
        // 从Token中提取用户信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        
        // 检查用户账号状态
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getId, userId);
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
          // 用户不存在，返回401未授权
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write("{\"code\":401,\"message\":\"用户不存在\"}");
          return false;
        }
        
        if (user.getStatus() == 0) {
          // 用户已被禁用，返回403禁止访问
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          response.setContentType("application/json;charset=UTF-8");
          response.getWriter().write("{\"code\":403,\"message\":\"账号已被禁用，请联系管理员\"}");
          return false;
        }
        
        // 设置用户信息到请求属性中
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("roleId", user.getRoleId());
        return true;
      }
    }

    // 允许用户注册（POST /user）免 Token
    if ("/user".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    // 如果没有 token，尝试从 userId header 获取（用于开发环境）
    // 生产环境应该移除这个逻辑
    String userIdHeader = request.getHeader("userId");
    if (StringUtils.hasText(userIdHeader)) {
      try {
        Long userId = Long.parseLong(userIdHeader);
        request.setAttribute("userId", userId);
        
        // 开发模式下，如果请求头带了 userId，尝试获取其角色并设置，方便调试
        User user = userMapper.selectById(userId);
        if (user != null) {
          request.setAttribute("roleId", user.getRoleId());
        }
        
        return true;
      } catch (NumberFormatException e) { // 忽略解析异常
      }
    }

    // 对于查询接口，如果没有 token 也允许通过（使用默认 userId=1）
    // 生产环境应该移除这个逻辑
    if (request.getRequestURI().startsWith("/query/")) {
      request.setAttribute("userId", 1L);
      request.setAttribute("roleId", 3); // 默认普通用户
      return true;
    }

    // 其他情况返回401未授权
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    // 显式禁止浏览器弹出登录框（针对某些特定环境）
    response.setHeader("WWW-Authenticate", "None");
    response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
    return false;
  }
}
