package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.ErrorLog;
import com.baoma.natural_language_query.mapper.ErrorLogMapper;
import com.baoma.natural_language_query.service.ErrorLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ErrorLogServiceImpl extends ServiceImpl<ErrorLogMapper, ErrorLog>
    implements ErrorLogService {

  @Override
  public List<ErrorLog> listByErrorTypeId(Integer errorTypeId) {
    LambdaQueryWrapper<ErrorLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ErrorLog::getErrorTypeId, errorTypeId);
    wrapper.orderByDesc(ErrorLog::getStatTime);
    return list(wrapper);
  }

  @Override
  public List<ErrorLog> listByPeriod(String period) {
    LambdaQueryWrapper<ErrorLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ErrorLog::getPeriod, period);
    wrapper.orderByDesc(ErrorLog::getStatTime);
    return list(wrapper);
  }
}
