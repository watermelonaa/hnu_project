package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mongodb.SqlCache;
import java.util.List;

public interface SqlCacheService {

  SqlCache getByNlHashAndConnectionIdAndTableIds(
      String nlHash, Long connectionId, List<Long> tableIds);

  List<SqlCache> listByUserId(Long userId);

  SqlCache save(SqlCache sqlCache);

  void deleteById(String id);
}
