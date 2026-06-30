package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.Notification;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface NotificationService extends IService<Notification> {
  /** 查询所有已发布的通知（置顶优先，按时间排序） */
  List<Notification> listPublished();

  /** 查询所有草稿 */
  List<Notification> listDrafts();

  /** 根据目标ID查询通知 */
  List<Notification> listByTargetId(Integer targetId);

  /** 根据用户角色查询可见的通知（已发布，置顶优先） */
  List<Notification> listByUserRole(Integer roleId);

  /** 根据发布者ID查询通知（数据管理员查看自己发布的通知） */
  List<Notification> listByPublisherId(Long publisherId);
}
