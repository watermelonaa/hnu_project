package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.QueryLog;
import com.baoma.natural_language_query.mapper.QueryLogMapper;
import com.baoma.natural_language_query.service.QueryLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QueryLogServiceImpl extends ServiceImpl<QueryLogMapper, QueryLog>
    implements QueryLogService {

  @Override
  public List<QueryLog> listByUserId(Long userId) {
    LambdaQueryWrapper<QueryLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(QueryLog::getUserId, userId);
    wrapper.orderByDesc(QueryLog::getQueryTime);
    return list(wrapper);
  }

  @Override
  public List<QueryLog> listByDialogId(String dialogId) {
    LambdaQueryWrapper<QueryLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(QueryLog::getDialogId, dialogId);
    wrapper.orderByAsc(QueryLog::getQueryTime);
    return list(wrapper);
  }
}
