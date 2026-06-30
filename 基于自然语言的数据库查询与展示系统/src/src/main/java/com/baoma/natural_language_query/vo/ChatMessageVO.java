package com.baoma.natural_language_query.vo;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/**
 * 聊天消息视图对象
 *
 * <p>用于返回聊天消息信息给前端
 */
@Data
public class ChatMessageVO {

  /** 消息ID */
  private String id;

  /** 发送者用户ID */
  private Long senderId;

  /** 发送者用户名 */
  private String senderName;

  /** 发送者头像URL */
  private String senderAvatar;

  /** 接收者用户ID */
  private Long receiverId;

  /** 接收者用户名 */
  private String receiverName;

  /** 接收者头像URL */
  private String receiverAvatar;

  /** 消息内容类型 */
  private String contentType;

  /** 消息内容 */
  private Map<String, Object> content;

  /** 发送时间 */
  private LocalDateTime sendTime;

  /** 是否已读 */
  private Boolean isRead;

  /** 扩展信息 */
  private Map<String, Object> extra;
}

