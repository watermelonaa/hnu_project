package com.baoma.natural_language_query.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 *
 * <p>配置WebSocket端点和消息代理，用于实现实时聊天功能
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  /**
   * 配置消息代理
   *
   * @param registry 消息代理注册表
   */
  @Override
  public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
    // 启用简单消息代理，消息目的地前缀为 /topic（广播）和 /queue（点对点）
    registry.enableSimpleBroker("/topic", "/queue");

    // 客户端发送消息的目的地前缀
    registry.setApplicationDestinationPrefixes("/app");

    // 点对点消息的前缀
    registry.setUserDestinationPrefix("/user");
  }

  /**
   * 配置客户端入站通道
   *
   * <p>添加拦截器来设置Principal，以便convertAndSendToUser能正确路由消息
   *
   * @param registration 通道注册表
   */
  @Override
  public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
    registration.interceptors(new WebSocketChannelInterceptor());
  }

  /**
   * 注册STOMP端点
   *
   * @param registry STOMP端点注册表
   */
  @Override
  public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
    // 注册端点 /ws-chat，并允许跨域
    registry
        .addEndpoint("/ws-chat")
        .setAllowedOriginPatterns("*")
        .addInterceptors(new WebSocketAuthInterceptor()) // 添加认证拦截器
        .withSockJS(); // 启用SockJS支持（兼容不支持WebSocket的浏览器）
  }
}

