package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.ColumnMetadata;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ColumnMetadataService extends IService<ColumnMetadata> {
  /** 根据表ID查询字段元数据列表 */
  List<ColumnMetadata> listByTableId(Long tableId);
}
