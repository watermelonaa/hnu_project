package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.TableMetadata;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface TableMetadataService extends IService<TableMetadata> {
  /** 根据数据库连接ID查询表元数据列表 */
  List<TableMetadata> listByDbConnectionId(Long dbConnectionId);
}
