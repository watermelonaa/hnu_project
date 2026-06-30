package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.DbType;
import com.baoma.natural_language_query.mapper.DbTypeMapper;
import com.baoma.natural_language_query.service.DbTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class DbTypeServiceImpl extends ServiceImpl<DbTypeMapper, DbType> implements DbTypeService {

  @Override
  public DbType getByTypeCode(String typeCode) {
    LambdaQueryWrapper<DbType> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(DbType::getTypeCode, typeCode);
    return getOne(wrapper);
  }
}
