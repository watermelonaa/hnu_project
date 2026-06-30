package com.baoma.natural_language_query.dto;

import java.util.Map;
import lombok.Data;

/**
 * 发送聊天消息数据传输对象
 *
 * <p>用于接收用户发送聊天消息的请求数据
 */
@Data
public class SendChatMessageDTO {

  /** 发送者用户ID */
  private Long senderId;

  /** 接收者用户ID */
  private Long receiverId;

  /**
   * 消息内容类型
   *
   * <p>支持类型：text-文本消息, image-图片消息, file-文件消息, query_share-查询分享
   */
  private String contentType;

  /** 消息内容（根据contentType不同，存储不同格式的数据） */
  private Map<String, Object> content;

  /** 扩展信息（可选） */
  private Map<String, Object> extra;
}

