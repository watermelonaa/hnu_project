package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.SystemHealth;
import com.baoma.natural_language_query.mapper.SystemHealthMapper;
import com.baoma.natural_language_query.service.SystemHealthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthServiceImpl extends ServiceImpl<SystemHealthMapper, SystemHealth>
    implements SystemHealthService {

  @Override
  public SystemHealth getLatest() {
    LambdaQueryWrapper<SystemHealth> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByDesc(SystemHealth::getCollectTime);
    wrapper.last("LIMIT 1");
    return getOne(wrapper);
  }

  @Override
  public List<SystemHealth> listRecent(int limit) {
    LambdaQueryWrapper<SystemHealth> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByDesc(SystemHealth::getCollectTime);
    wrapper.last("LIMIT " + limit);
    return list(wrapper);
  }
}
