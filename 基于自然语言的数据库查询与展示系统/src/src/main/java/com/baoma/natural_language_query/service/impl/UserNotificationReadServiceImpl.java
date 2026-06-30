package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.Notification;
import com.baoma.natural_language_query.entity.mysql.UserNotificationRead;
import com.baoma.natural_language_query.mapper.UserNotificationReadMapper;
import com.baoma.natural_language_query.service.NotificationService;
import com.baoma.natural_language_query.service.UserNotificationReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 用户通知已读状态服务实现类
 *
 * <p>提供用户通知已读状态的管理功能，包括标记已读/未读、查询已读状态、统计未读数量等。
 */
@Service
public class UserNotificationReadServiceImpl
    extends ServiceImpl<UserNotificationReadMapper, UserNotificationRead>
    implements UserNotificationReadService {

  /** 已读状态：已读 */
  private static final int READ_STATUS_READ = 1;

  /** 已读状态：未读 */
  private static final int READ_STATUS_UNREAD = 0;

  private final NotificationService notificationService;

  public UserNotificationReadServiceImpl(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Override
  public void markAsRead(Long userId, Long notificationId) {
    UserNotificationRead read = getOrCreateReadRecord(userId, notificationId);
    read.setIsRead(READ_STATUS_READ);
    read.setReadTime(LocalDateTime.now());
    saveOrUpdate(read);
  }

  @Override
  public void markAsUnread(Long userId, Long notificationId) {
    UserNotificationRead read = getReadRecord(userId, notificationId);
    if (read != null) {
      read.setIsRead(READ_STATUS_UNREAD);
      read.setReadTime(null);
      updateById(read);
    }
  }

  @Override
  public boolean isRead(Long userId, Long notificationId) {
    LambdaQueryWrapper<UserNotificationRead> wrapper = buildUserNotificationWrapper(userId, notificationId);
    wrapper.eq(UserNotificationRead::getIsRead, READ_STATUS_READ);
    return count(wrapper) > 0;
  }

  @Override
  public int getUnreadCount(Long userId, Integer roleId) {
    List<Notification> notifications = notificationService.listByUserRole(roleId);
    if (notifications.isEmpty()) {
      return 0;
    }

    Set<Long> readNotificationIds = getReadNotificationIds(userId);
    long unreadCount = 0;
    for (Notification notification : notifications) {
      Long notificationId = notification.getId();
      if (notificationId != null && !readNotificationIds.contains(notificationId)) {
        unreadCount++;
      }
    }
    return (int) unreadCount;
  }

  /**
   * 获取或创建用户通知已读记录
   *
   * @param userId 用户ID
   * @param notificationId 通知ID
   * @return 用户通知已读记录
   */
  private UserNotificationRead getOrCreateReadRecord(Long userId, Long notificationId) {
    UserNotificationRead read = getReadRecord(userId, notificationId);
    if (read == null) {
      read = new UserNotificationRead();
      read.setUserId(userId);
      read.setNotificationId(notificationId);
    }
    return read;
  }

  /**
   * 获取用户通知已读记录
   *
   * @param userId 用户ID
   * @param notificationId 通知ID
   * @return 用户通知已读记录，如果不存在则返回null
   */
  private UserNotificationRead getReadRecord(Long userId, Long notificationId) {
    LambdaQueryWrapper<UserNotificationRead> wrapper =
        buildUserNotificationWrapper(userId, notificationId);
    return getOne(wrapper);
  }

  /**
   * 构建用户通知查询包装器
   *
   * @param userId 用户ID
   * @param notificationId 通知ID
   * @return 查询包装器
   */
  private LambdaQueryWrapper<UserNotificationRead> buildUserNotificationWrapper(
      Long userId, Long notificationId) {
    LambdaQueryWrapper<UserNotificationRead> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(UserNotificationRead::getUserId, userId)
        .eq(UserNotificationRead::getNotificationId, notificationId);
    return wrapper;
  }

  /**
   * 获取用户已读的通知ID集合
   *
   * @param userId 用户ID
   * @return 已读通知ID集合
   */
  private Set<Long> getReadNotificationIds(Long userId) {
    LambdaQueryWrapper<UserNotificationRead> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(UserNotificationRead::getUserId, userId)
        .eq(UserNotificationRead::getIsRead, READ_STATUS_READ);
    List<UserNotificationRead> readList = list(wrapper);
    return readList.stream()
        .map(UserNotificationRead::getNotificationId)
        .collect(Collectors.toSet());
  }

  @Override
  public void resetAllUsersReadStatus(Long notificationId) {
    // 查询该通知的所有已读记录
    LambdaQueryWrapper<UserNotificationRead> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(UserNotificationRead::getNotificationId, notificationId)
        .eq(UserNotificationRead::getIsRead, READ_STATUS_READ);
    
    List<UserNotificationRead> readRecords = list(wrapper);
    
    // 将所有已读记录重置为未读
    for (UserNotificationRead record : readRecords) {
      record.setIsRead(READ_STATUS_UNREAD);
      record.setReadTime(null);
    }
    
    // 批量更新
    if (!readRecords.isEmpty()) {
      updateBatchById(readRecords);
    }
  }
}

