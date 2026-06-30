package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.dto.SendChatMessageDTO;
import com.baoma.natural_language_query.dto.ShareQueryDTO;
import com.baoma.natural_language_query.service.FriendChatService;
import com.baoma.natural_language_query.service.QueryShareChatService;
import com.baoma.natural_language_query.vo.ChatMessageVO;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket聊天控制器
 *
 * <p>处理WebSocket实时聊天消息
 */
@Controller
public class ChatWebSocketController {

  @Autowired private FriendChatService friendChatService;

  @Autowired private QueryShareChatService queryShareChatService;

  @Autowired private SimpMessagingTemplate messagingTemplate;

  /**
   * 处理私聊消息
   *
   * <p>客户端发送消息到 /app/chat/private
   *
   * @param dto 聊天消息DTO
   * @param principal 当前用户信息
   */
  @MessageMapping("/chat/private")
  public void sendPrivateMessage(@Payload SendChatMessageDTO dto, Principal principal) {
    try {
      System.out.println("========================================");
      System.out.println("[WebSocket] 收到私聊消息");
      System.out.println("   senderId=" + dto.getSenderId() + ", receiverId=" + dto.getReceiverId());
      System.out.println("   contentType=" + dto.getContentType());
      System.out.println("   content=" + dto.getContent());
      System.out.println("   Principal=" + (principal != null ? principal.getName() : "null"));
      
      // 保存消息到数据库
      ChatMessageVO message = friendChatService.sendMessage(dto);
      System.out.println("[WebSocket] 消息已保存到数据库，ID: " + message.getId());

      // 发送消息给接收者（点对点）
      // convertAndSendToUser 使用 Principal 的 name 来路由消息
      // 所以 userId 必须与 Principal 的 name 匹配
      String receiverUserId = dto.getReceiverId().toString();
      String senderUserId = dto.getSenderId().toString();
      
      System.out.println("[WebSocket] 准备发送消息:");
      System.out.println("   接收者路径: /user/" + receiverUserId + "/queue/messages");
      System.out.println("   发送者路径: /user/" + senderUserId + "/queue/messages");
      System.out.println("   消息内容: " + message);
      
      // 发送给接收者
      // 注意：convertAndSendToUser 使用 Principal 的 name 来匹配用户
      // 所以 receiverUserId 必须与接收者连接时的 Principal.name 完全匹配
      System.out.println("[WebSocket] 发送消息给接收者，userId=" + receiverUserId);
      messagingTemplate.convertAndSendToUser(
          receiverUserId, "/queue/messages", message);
      System.out.println("[WebSocket] 消息已发送给接收者: " + receiverUserId);

      // 也发送给发送者自己（用于多设备同步）
      System.out.println("[WebSocket] 发送消息给发送者，userId=" + senderUserId);
      messagingTemplate.convertAndSendToUser(
          senderUserId, "/queue/messages", message);
      System.out.println("[WebSocket] 消息已发送给发送者: " + senderUserId);
      
      System.out.println("[WebSocket] 消息已通过WebSocket发送完成");
      System.out.println("========================================");

    } catch (Exception e) {
      // 错误处理
      System.err.println("[WebSocket] 发送消息失败: " + e.getMessage());
      e.printStackTrace();
      // 可以发送错误消息给发送者
      try {
        messagingTemplate.convertAndSendToUser(
            dto.getSenderId().toString(),
            "/queue/errors",
            "消息发送失败: " + e.getMessage());
        System.out.println("[WebSocket] 错误消息已发送给发送者");
      } catch (Exception ex) {
        System.err.println("[WebSocket] 发送错误消息失败: " + ex.getMessage());
      }
    }
  }

  /**
   * 通知用户在线状态变化
   *
   * <p>客户端发送消息到 /app/chat/online
   *
   * @param userId 用户ID
   */
  @MessageMapping("/chat/online")
  public void userOnline(@Payload Long userId) {
    // 广播用户上线消息
    messagingTemplate.convertAndSend("/topic/online", userId);
  }

  /**
   * 通知用户离线状态变化
   *
   * <p>客户端发送消息到 /app/chat/offline
   *
   * @param userId 用户ID
   */
  @MessageMapping("/chat/offline")
  public void userOffline(@Payload Long userId) {
    // 广播用户离线消息
    messagingTemplate.convertAndSend("/topic/offline", userId);
  }

  /**
   * 发送打字状态通知
   *
   * <p>客户端发送消息到 /app/chat/typing
   *
   * @param dto 包含发送者和接收者信息的对象
   */
  @MessageMapping("/chat/typing")
  public void sendTypingStatus(@Payload SendChatMessageDTO dto) {
    // 通知接收者发送者正在输入
    messagingTemplate.convertAndSendToUser(
        dto.getReceiverId().toString(), "/queue/typing", dto.getSenderId());
  }

  /**
   * 通过WebSocket分享查询记录
   *
   * <p>客户端发送消息到 /app/chat/shareQuery
   *
   * @param dto 查询分享DTO
   */
  @MessageMapping("/chat/shareQuery")
  public void shareQuery(@Payload ShareQueryDTO dto) {
    try {
      // 保存查询分享消息
      ChatMessageVO message = queryShareChatService.shareQueryToFriend(dto);

      // 发送消息给接收者
      messagingTemplate.convertAndSendToUser(
          dto.getReceiveUserId().toString(), "/queue/messages", message);

      // 也发送给发送者自己（多设备同步）
      messagingTemplate.convertAndSendToUser(
          dto.getShareUserId().toString(), "/queue/messages", message);

    } catch (Exception e) {
      e.printStackTrace();
      // 发送错误消息给发送者
      messagingTemplate.convertAndSendToUser(
          dto.getShareUserId().toString(),
          "/queue/errors",
          "查询分享失败: " + e.getMessage());
    }
  }
}

