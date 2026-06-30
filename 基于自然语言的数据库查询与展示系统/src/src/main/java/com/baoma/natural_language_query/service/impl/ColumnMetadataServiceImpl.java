package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.ColumnMetadata;
import com.baoma.natural_language_query.mapper.ColumnMetadataMapper;
import com.baoma.natural_language_query.service.ColumnMetadataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ColumnMetadataServiceImpl extends ServiceImpl<ColumnMetadataMapper, ColumnMetadata>
    implements ColumnMetadataService {

  @Override
  public List<ColumnMetadata> listByTableId(Long tableId) {
    LambdaQueryWrapper<ColumnMetadata> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ColumnMetadata::getTableId, tableId);
    // 主键字段优先
    wrapper.orderByDesc(ColumnMetadata::getIsPrimary);
    wrapper.orderByAsc(ColumnMetadata::getColumnName);
    return list(wrapper);
  }
}
