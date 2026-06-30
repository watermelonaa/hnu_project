package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.Notification;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.service.NotificationService;
import com.baoma.natural_language_query.service.UserNotificationReadService;
import com.baoma.natural_language_query.service.UserService;
import com.baoma.natural_language_query.utils.PermissionUtil;
import com.baoma.natural_language_query.vo.NotificationVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知管理控制器
 *
 * <p>提供通知的CRUD操作和用户通知相关的API接口。
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

  /** 置顶状态：是 */
  private static final int TOP_STATUS_YES = 1;

  /** 置顶状态：否 */
  private static final int TOP_STATUS_NO = 0;

  private final NotificationService notificationService;
  private final UserNotificationReadService userNotificationReadService;
  private final UserService userService;

  @Autowired
  public NotificationController(
      NotificationService notificationService,
      UserNotificationReadService userNotificationReadService,
      UserService userService) {
    this.notificationService = notificationService;
    this.userNotificationReadService = userNotificationReadService;
    this.userService = userService;
  }

  /** 查询所有通知 */
  @GetMapping("/list")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<Notification>> list(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    
    // 系统管理员可以查看所有通知，数据管理员只能查看自己发布的
    if (PermissionUtil.isAdmin(request)) {
      return Result.success(notificationService.list());
    } else {
      // 数据管理员只能看到自己发布的通知
      return Result.success(notificationService.listByPublisherId(userId));
    }
  }

  /** 查询已发布的通知 */
  @GetMapping("/list/published")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<Notification>> listPublished() {
    return Result.success(notificationService.listPublished());
  }

  /** 查询草稿 */
  @GetMapping("/list/drafts")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<Notification>> listDrafts() {
    return Result.success(notificationService.listDrafts());
  }

  /** 根据目标ID查询通知 */
  @GetMapping("/list/target/{targetId}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<List<Notification>> listByTarget(@PathVariable Integer targetId) {
    return Result.success(notificationService.listByTargetId(targetId));
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  @RequirePermission(Role.USER)
  public Result<Notification> getById(@PathVariable Long id) {
    return Result.success(notificationService.getById(id));
  }

  /** 添加通知（草稿） */
  @PostMapping
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Notification> save(@RequestBody Notification notification, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return Result.error("未登录或登录已过期");
    }
    
    LocalDateTime now = LocalDateTime.now();
    notification.setCreateTime(now);
    notification.setLatestUpdateTime(now);
    notification.setPublisherId(userId); // 强制绑定当前登录用户为发布者
    
    if (notification.getIsTop() == null) {
      notification.setIsTop(TOP_STATUS_NO);
    }
    notificationService.save(notification);
    return Result.success(notification);
  }

  /** 更新通知 */
  @PutMapping
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Notification> update(@RequestBody Notification notification, HttpServletRequest request) {
    Notification existing = notificationService.getById(notification.getId());
    if (existing == null) {
      return Result.error("通知不存在");
    }
    
    // 权限校验：只有发布者本人或系统管理员可以修改
    if (!PermissionUtil.isOwner(request, existing.getPublisherId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能修改自己发布的通知");
    }

    notification.setLatestUpdateTime(LocalDateTime.now());
    notification.setPublisherId(existing.getPublisherId()); // 保护发布者字段不被随意串改
    notificationService.updateById(notification);
    
    // 通知更新后，重置所有用户对该通知的已读状态
    if (notification.getId() != null) {
      userNotificationReadService.resetAllUsersReadStatus(notification.getId());
    }
    
    return Result.success(notification);
  }

  /** 发布通知 */
  @PutMapping("/{id}/publish")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Void> publish(@PathVariable Long id, HttpServletRequest request) {
    Notification notification = notificationService.getById(id);
    if (notification == null) {
      return Result.error("通知不存在");
    }
    
    // 权限校验：只有发布者本人或系统管理员可以发布
    if (!PermissionUtil.isOwner(request, notification.getPublisherId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能发布自己创建的通知");
    }

    LocalDateTime now = LocalDateTime.now();
    notification.setPublishTime(now);
    notification.setLatestUpdateTime(now);
    notificationService.updateById(notification);
    return Result.success();
  }

  /** 置顶/取消置顶 */
  @PutMapping("/{id}/toggle-top")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Void> toggleTop(@PathVariable Long id, HttpServletRequest request) {
    Notification notification = notificationService.getById(id);
    if (notification == null) {
      return Result.error("通知不存在");
    }
    
    // 权限校验：只有发布者本人或系统管理员可以置顶
    if (!PermissionUtil.isOwner(request, notification.getPublisherId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能对自己的通知执行置顶操作");
    }

    int currentTopStatus = notification.getIsTop() == null ? TOP_STATUS_NO : notification.getIsTop();
    notification.setIsTop(currentTopStatus == TOP_STATUS_NO ? TOP_STATUS_YES : TOP_STATUS_NO);
    notification.setLatestUpdateTime(LocalDateTime.now());
    notificationService.updateById(notification);
    return Result.success();
  }

  /** 删除通知 */
  @DeleteMapping("/{id}")
  @RequirePermission(Role.DATA_ADMIN)
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    Notification existing = notificationService.getById(id);
    if (existing == null) {
      return Result.error("通知不存在");
    }
    
    // 权限校验：只有发布者本人或系统管理员可以删除
    if (!PermissionUtil.isOwner(request, existing.getPublisherId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能删除自己发布的通知");
    }

    notificationService.removeById(id);
    return Result.success();
  }

  // ==================== 用户相关API ====================

  /** 根据用户ID获取可见的通知列表（带已读状态） */
  @GetMapping("/user/list")
  @RequirePermission(Role.USER)
  public Result<List<NotificationVO>> getUserNotifications(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return Result.error("未登录或登录已过期");
    }

    User user = getUserById(userId);
    if (user == null) {
      return Result.error("用户不存在");
    }

    List<Notification> notifications = notificationService.listByUserRole(user.getRoleId());
    List<NotificationVO> result =
        notifications.stream()
            .map(notification -> convertToVO(notification, userId))
            .collect(Collectors.toList());

    return Result.success(result);
  }

  /** 获取用户未读通知数量 */
  @GetMapping("/user/unread-count")
  @RequirePermission(Role.USER)
  public Result<Integer> getUnreadCount(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return Result.error("未登录或登录已过期");
    }

    User user = getUserById(userId);
    if (user == null) {
      return Result.error("用户不存在");
    }
    int count = userNotificationReadService.getUnreadCount(userId, user.getRoleId());
    return Result.success(count);
  }

  /** 标记通知为已读 */
  @PutMapping("/user/{notificationId}/read")
  @RequirePermission(Role.USER)
  public Result<Void> markAsRead(@PathVariable Long notificationId, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return Result.error("未登录或登录已过期");
    }
    userNotificationReadService.markAsRead(userId, notificationId);
    return Result.success();
  }

  /** 标记通知为未读 */
  @PutMapping("/user/{notificationId}/unread")
  @RequirePermission(Role.USER)
  public Result<Void> markAsUnread(@PathVariable Long notificationId, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return Result.error("未登录或登录已过期");
    }
    userNotificationReadService.markAsUnread(userId, notificationId);
    return Result.success();
  }

  /** 用户删除通知（只能删除非置顶通知） */
  @DeleteMapping("/user/{id}")
  @RequirePermission(Role.USER)
  public Result<Void> deleteByUser(@PathVariable Long id, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return Result.error("未登录或登录已过期");
    }

    Notification notification = notificationService.getById(id);
    if (notification == null) {
      return Result.error("通知不存在");
    }

    if (notification.getIsTop() != null && notification.getIsTop() == TOP_STATUS_YES) {
      return Result.error("不能删除置顶通知");
    }

    notificationService.removeById(id);
    return Result.success();
  }

  /**
   * 根据用户ID获取用户信息
   *
   * @param userId 用户ID
   * @return 用户信息，如果不存在则返回null
   */
  private User getUserById(Long userId) {
    return userService.getById(userId);
  }

  /**
   * 将通知实体转换为视图对象
   *
   * @param notification 通知实体
   * @param userId 用户ID
   * @return 通知视图对象
   */
  private NotificationVO convertToVO(Notification notification, Long userId) {
    NotificationVO vo = new NotificationVO();
    vo.setId(notification.getId());
    vo.setTitle(notification.getTitle());
    vo.setContent(notification.getContent());
    vo.setTargetId(notification.getTargetId());
    vo.setPriorityId(notification.getPriorityId());
    vo.setPublisherId(notification.getPublisherId());
    vo.setIsTop(notification.getIsTop());
    vo.setPublishTime(notification.getPublishTime());
    vo.setCreateTime(notification.getCreateTime());
    vo.setLatestUpdateTime(notification.getLatestUpdateTime());

    boolean isRead = userNotificationReadService.isRead(userId, notification.getId());
    vo.setIsRead(isRead ? 1 : 0);

    return vo;
  }
}
