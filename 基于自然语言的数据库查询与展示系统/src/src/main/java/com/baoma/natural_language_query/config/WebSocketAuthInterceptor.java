package com.baoma.natural_language_query.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket认证拦截器（简化版）
 *
 * <p>在WebSocket握手时从URL参数或请求头中提取用户ID
 */
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

  @Override
  public boolean beforeHandshake(
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response,
      @NonNull WebSocketHandler wsHandler,
      @NonNull Map<String, Object> attributes)
      throws Exception {
    if (request instanceof ServletServerHttpRequest) {
      ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
      // 优先从URL参数获取userId
      String userId = servletRequest.getServletRequest().getParameter("userId");
      
      // 如果URL参数中没有，尝试从请求头获取
      if (userId == null || userId.isEmpty()) {
        userId = servletRequest.getServletRequest().getHeader("userId");
      }
      
      if (userId != null && !userId.isEmpty()) {
        attributes.put("userId", userId);
      }
    }
    
    return true;
  }

  @Override
  public void afterHandshake(
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response,
      @NonNull WebSocketHandler wsHandler,
      Exception exception) {
    // 握手后处理
  }

  /**
   * 简单的Principal实现
   */
  public static class SimplePrincipal implements Principal {
    private final String name;

    public SimplePrincipal(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}

