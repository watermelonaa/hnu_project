package com.baoma.natural_language_query.dto;

import lombok.Data;

/**
 * 聊天消息分页查询数据传输对象
 *
 * <p>用于分页查询聊天记录
 */
@Data
public class ChatMessagePageDTO {

  /** 当前用户ID */
  private Long userId;

  /** 好友用户ID */
  private Long friendId;

  /** 页码（从1开始） */
  private Integer page = 1;

  /** 每页大小 */
  private Integer pageSize = 20;

  /** 最后一条消息的时间戳（用于滚动加载） */
  private String lastMessageTime;
}

