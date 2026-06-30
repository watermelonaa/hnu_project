package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mongodb.SqlCache;
import com.baoma.natural_language_query.repository.SqlCacheRepository;
import com.baoma.natural_language_query.service.SqlCacheService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SqlCacheServiceImpl implements SqlCacheService {

  @Autowired private SqlCacheRepository sqlCacheRepository;

  @Override
  public SqlCache getByNlHashAndConnectionIdAndTableIds(
      String nlHash, Long connectionId, List<Long> tableIds) {
    return sqlCacheRepository.findByNlHashAndConnectionIdAndTableIds(
        nlHash, connectionId, tableIds);
  }

  @Override
  public List<SqlCache> listByUserId(Long userId) {
    return sqlCacheRepository.findByUserId(userId);
  }

  @Override
  public SqlCache save(SqlCache sqlCache) {
    return sqlCacheRepository.save(sqlCache);
  }

  @Override
  public void deleteById(String id) {
    sqlCacheRepository.deleteById(id);
  }
}
