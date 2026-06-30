package com.baoma.natural_language_query.config;

import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket事件监听器
 *
 * <p>监听WebSocket连接和断开事件，更新用户在线状态
 */
@Component
public class WebSocketEventListener {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

  @Autowired private SimpMessagingTemplate messagingTemplate;

  @Autowired private UserMapper userMapper;

  /**
   * 处理WebSocket连接事件
   *
   * @param event 连接事件
   */
  @EventListener
  public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = headerAccessor.getSessionId();

    logger.info("WebSocket连接建立，SessionId: {}", sessionId);

    // 这里可以获取用户信息并更新在线状态
    // 注意：需要在客户端连接时传递用户ID
    String userId = headerAccessor.getFirstNativeHeader("userId");
    if (userId != null && !userId.isEmpty()) {
      try {
        Long uid = Long.parseLong(userId);
        updateUserOnlineStatus(uid, 1);

        // 广播用户上线消息
        messagingTemplate.convertAndSend("/topic/online", uid);
        logger.info("用户 {} 上线", uid);
      } catch (NumberFormatException e) {
        logger.error("用户ID格式错误: {}", userId);
      }
    }
  }

  /**
   * 处理WebSocket断开事件
   *
   * @param event 断开事件
   */
  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = headerAccessor.getSessionId();

    logger.info("WebSocket连接断开，SessionId: {}", sessionId);

    // 获取用户信息并更新离线状态
    String userId = headerAccessor.getFirstNativeHeader("userId");
    if (userId != null && !userId.isEmpty()) {
      try {
        Long uid = Long.parseLong(userId);
        updateUserOnlineStatus(uid, 0);

        // 广播用户离线消息
        messagingTemplate.convertAndSend("/topic/offline", uid);
        logger.info("用户 {} 离线", uid);
      } catch (NumberFormatException e) {
        logger.error("用户ID格式错误: {}", userId);
      }
    }
  }

  /**
   * 更新用户在线状态
   *
   * @param userId 用户ID
   * @param status 状态（0-离线，1-在线）
   */
  private void updateUserOnlineStatus(Long userId, Integer status) {
    try {
      User user = userMapper.selectById(userId);
      if (user != null) {
        user.setOnlineStatus(status);
        userMapper.updateById(user);
      }
    } catch (Exception e) {
      logger.error("更新用户在线状态失败，userId: {}, status: {}", userId, status, e);
    }
  }
}

