package com.baoma.natural_language_query.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息数量视图对象
 *
 * <p>用于返回未读消息统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {

  /** 好友用户ID */
  private Long friendId;

  /** 好友用户名 */
  private String friendName;

  /** 好友头像URL */
  private String friendAvatar;

  /** 未读消息数量 */
  private Long unreadCount;

  /** 最后一条消息内容预览 */
  private String lastMessagePreview;

  /** 最后一条消息时间 */
  private String lastMessageTime;
}

