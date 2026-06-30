package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.TableMetadata;
import com.baoma.natural_language_query.mapper.TableMetadataMapper;
import com.baoma.natural_language_query.service.TableMetadataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TableMetadataServiceImpl extends ServiceImpl<TableMetadataMapper, TableMetadata>
    implements TableMetadataService {

  @Override
  public List<TableMetadata> listByDbConnectionId(Long dbConnectionId) {
    LambdaQueryWrapper<TableMetadata> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(TableMetadata::getDbConnectionId, dbConnectionId);
    wrapper.orderByAsc(TableMetadata::getTableName);
    return list(wrapper);
  }
}
