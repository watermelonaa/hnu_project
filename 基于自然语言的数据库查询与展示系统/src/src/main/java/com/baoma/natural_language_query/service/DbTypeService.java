package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.DbType;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DbTypeService extends IService<DbType> {
  /** 根据类型编码查询 */
  DbType getByTypeCode(String typeCode);
}
