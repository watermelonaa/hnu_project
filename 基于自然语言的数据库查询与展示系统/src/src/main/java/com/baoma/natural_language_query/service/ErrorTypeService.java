package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.ErrorType;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ErrorTypeService extends IService<ErrorType> {
  /** 根据错误编码查询 */
  ErrorType getByErrorCode(String errorCode);
}
