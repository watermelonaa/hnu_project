package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.NotificationTarget;
import com.baomidou.mybatisplus.extension.service.IService;

public interface NotificationTargetService extends IService<NotificationTarget> {
  /** 根据目标编码查询 */
  NotificationTarget getByTargetCode(String targetCode);
}
