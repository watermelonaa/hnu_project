package com.baoma.natural_language_query.repository;

import com.baoma.natural_language_query.entity.mongodb.SqlCache;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SqlCacheRepository extends MongoRepository<SqlCache, String> {

  SqlCache findByNlHashAndConnectionIdAndTableIds(
      String nlHash, Long connectionId, List<Long> tableIds);

  List<SqlCache> findByUserId(Long userId);
}
