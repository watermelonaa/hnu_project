package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.UserNotificationRead;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserNotificationReadService extends IService<UserNotificationRead> {
  /** 标记通知为已读 */
  void markAsRead(Long userId, Long notificationId);

  /** 标记通知为未读 */
  void markAsUnread(Long userId, Long notificationId);

  /** 检查通知是否已读 */
  boolean isRead(Long userId, Long notificationId);

  /** 获取用户未读通知数量 */
  int getUnreadCount(Long userId, Integer roleId);

  /** 重置通知的所有用户已读状态（通知更新后调用） */
  void resetAllUsersReadStatus(Long notificationId);
}

