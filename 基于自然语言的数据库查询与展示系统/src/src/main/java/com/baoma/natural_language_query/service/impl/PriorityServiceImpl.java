package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.Priority;
import com.baoma.natural_language_query.mapper.PriorityMapper;
import com.baoma.natural_language_query.service.PriorityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PriorityServiceImpl extends ServiceImpl<PriorityMapper, Priority>
    implements PriorityService {

  @Override
  public Priority getByPriorityCode(String priorityCode) {
    LambdaQueryWrapper<Priority> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Priority::getPriorityCode, priorityCode);
    return getOne(wrapper);
  }

  @Override
  public List<Priority> listOrderBySort() {
    LambdaQueryWrapper<Priority> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByAsc(Priority::getSort);
    return list(wrapper);
  }
}
