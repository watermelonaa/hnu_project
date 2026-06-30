package com.baoma.natural_language_query.config;

import java.security.Principal;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * WebSocket通道拦截器（简化版）
 *
 * <p>从STOMP CONNECT帧中提取userId并设置Principal，以便convertAndSendToUser能正确路由
 */
public class WebSocketChannelInterceptor implements ChannelInterceptor {

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    
    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      // 从CONNECT帧的header中获取userId
      String userId = accessor.getFirstNativeHeader("userId");
      
      // 如果header中没有，尝试从会话属性获取（由WebSocketAuthInterceptor设置）
      if ((userId == null || userId.isEmpty()) && accessor.getSessionAttributes() != null) {
        Object userIdObj = accessor.getSessionAttributes().get("userId");
        if (userIdObj != null) {
          userId = userIdObj.toString();
        }
      }
      
      if (userId != null && !userId.isEmpty()) {
        Principal principal = new WebSocketAuthInterceptor.SimplePrincipal(userId);
        accessor.setUser(principal);
      }
    }
    
    return message;
  }
}

