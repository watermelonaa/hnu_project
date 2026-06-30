package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.ErrorType;
import com.baoma.natural_language_query.mapper.ErrorTypeMapper;
import com.baoma.natural_language_query.service.ErrorTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ErrorTypeServiceImpl extends ServiceImpl<ErrorTypeMapper, ErrorType>
    implements ErrorTypeService {

  @Override
  public ErrorType getByErrorCode(String errorCode) {
    LambdaQueryWrapper<ErrorType> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ErrorType::getErrorCode, errorCode);
    return getOne(wrapper);
  }
}
