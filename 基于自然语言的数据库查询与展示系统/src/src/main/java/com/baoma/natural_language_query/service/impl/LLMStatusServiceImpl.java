package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.LlmStatus;
import com.baoma.natural_language_query.mapper.LLMStatusMapper;
import com.baoma.natural_language_query.service.LLMStatusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class LLMStatusServiceImpl extends ServiceImpl<LLMStatusMapper, LlmStatus>
    implements LLMStatusService {

  @Override
  public LlmStatus getByStatusCode(String statusCode) {
    LambdaQueryWrapper<LlmStatus> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(LlmStatus::getStatusCode, statusCode);
    return getOne(wrapper);
  }
}
