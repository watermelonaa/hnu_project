package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.NotificationTarget;
import com.baoma.natural_language_query.mapper.NotificationTargetMapper;
import com.baoma.natural_language_query.service.NotificationTargetService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class NotificationTargetServiceImpl
    extends ServiceImpl<NotificationTargetMapper, NotificationTarget>
    implements NotificationTargetService {

  @Override
  public NotificationTarget getByTargetCode(String targetCode) {
    LambdaQueryWrapper<NotificationTarget> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(NotificationTarget::getTargetCode, targetCode);
    return getOne(wrapper);
  }
}
