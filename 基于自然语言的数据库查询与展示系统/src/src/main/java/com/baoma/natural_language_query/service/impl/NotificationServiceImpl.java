package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.Notification;
import com.baoma.natural_language_query.mapper.NotificationMapper;
import com.baoma.natural_language_query.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现类
 *
 * <p>提供通知的查询、创建、更新、删除等功能。
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
    implements NotificationService {

  /** 所有用户的目标ID */
  private static final int TARGET_ID_ALL_USERS = 1;

  @Override
  public List<Notification> listPublished() {
    LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
    wrapper.isNotNull(Notification::getPublishTime);
    applyPublishedOrder(wrapper);
    return list(wrapper);
  }

  @Override
  public List<Notification> listDrafts() {
    LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
    wrapper.isNull(Notification::getPublishTime);
    wrapper.orderByDesc(Notification::getLatestUpdateTime);
    return list(wrapper);
  }

  @Override
  public List<Notification> listByTargetId(Integer targetId) {
    LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Notification::getTargetId, targetId);
    wrapper.isNotNull(Notification::getPublishTime);
    applyPublishedOrder(wrapper);
    return list(wrapper);
  }

  @Override
  public List<Notification> listByUserRole(Integer roleId) {
    LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
    // 查询目标为"所有用户"或目标为当前角色的通知
    wrapper.and(
        w ->
            w.eq(Notification::getTargetId, TARGET_ID_ALL_USERS)
                .or()
                .eq(Notification::getTargetId, roleId));
    wrapper.isNotNull(Notification::getPublishTime);
    applyPublishedOrder(wrapper);
    return list(wrapper);
  }

  @Override
  public List<Notification> listByPublisherId(Long publisherId) {
    LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Notification::getPublisherId, publisherId);
    // 按最后更新时间降序排序（包括草稿和已发布）
    wrapper.orderByDesc(Notification::getLatestUpdateTime);
    return list(wrapper);
  }

  /**
   * 应用已发布通知的排序规则：置顶优先，然后按发布时间降序
   *
   * @param wrapper 查询包装器
   */
  private void applyPublishedOrder(LambdaQueryWrapper<Notification> wrapper) {
    wrapper.orderByDesc(Notification::getIsTop);
    wrapper.orderByDesc(Notification::getPublishTime);
  }
}
