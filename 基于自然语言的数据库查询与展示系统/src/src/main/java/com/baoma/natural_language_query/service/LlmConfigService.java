package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.LlmConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface LlmConfigService extends IService<LlmConfig> {
  /** 获取所有可用的大模型配置 */
  List<LlmConfig> listAvailable();
}
