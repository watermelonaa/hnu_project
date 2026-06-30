package com.baoma.natural_language_query.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 通知视图对象（带已读状态）
 *
 * <p>用于返回用户可见的通知信息，包含通知基本信息和用户的已读状态。
 */
@Data
public class NotificationVO {

  /** 通知ID */
  private Long id;

  /** 通知标题 */
  private String title;

  /** 通知内容 */
  private String content;

  /** 目标ID */
  private Integer targetId;

  /** 优先级ID */
  private Integer priorityId;

  /** 发布者ID */
  private Long publisherId;

  /** 是否置顶：0-否，1-是 */
  private Integer isTop;

  /** 发布时间 */
  private LocalDateTime publishTime;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 最后更新时间 */
  private LocalDateTime latestUpdateTime;

  /** 是否已读：0-未读，1-已读 */
  private Integer isRead;
}

