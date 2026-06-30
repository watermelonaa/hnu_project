package com.baoma.natural_language_query.dto;

import lombok.Data;

/**
 * 发送好友申请数据传输对象
 *
 * <p>用于接收用户发送好友申请的请求数据。 用户可以通过邮箱或手机号搜索其他用户并发送好友申请。
 */
@Data
public class SendFriendRequestDTO {

  /** 发送申请的用户ID */
  private Long sendUserId;

  /**
   * 搜索关键字（邮箱或手机号）
   *
   * <p>用于查找要添加的好友
   */
  private String searchKey;

  /**
   * 申请消息
   *
   * <p>用户发送好友申请时附加的消息内容
   */
  private String message;
}
