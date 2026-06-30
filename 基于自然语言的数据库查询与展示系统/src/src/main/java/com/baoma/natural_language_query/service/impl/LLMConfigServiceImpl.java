package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.LlmConfig;
import com.baoma.natural_language_query.mapper.LLMConfigMapper;
import com.baoma.natural_language_query.service.LlmConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LLMConfigServiceImpl extends ServiceImpl<LLMConfigMapper, LlmConfig>
    implements LlmConfigService {

  @Override
  public List<LlmConfig> listAvailable() {
    LambdaQueryWrapper<LlmConfig> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(LlmConfig::getIsDisabled, 0);
    wrapper.orderByDesc(LlmConfig::getCreateTime);
    return list(wrapper);
  }
}
