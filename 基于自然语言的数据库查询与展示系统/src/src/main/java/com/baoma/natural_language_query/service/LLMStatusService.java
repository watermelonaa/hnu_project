package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.LlmStatus;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LLMStatusService extends IService<LlmStatus> {
  /** 根据状态编码查询 */
  LlmStatus getByStatusCode(String statusCode);
}
