package com.baoma.natural_language_query.repository;

import com.baoma.natural_language_query.entity.mongodb.QueryCollection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryCollectionRepository extends MongoRepository<QueryCollection, String> {

  List<QueryCollection> findByUserId(Long userId);

  QueryCollection findByUserIdAndGroupName(Long userId, String groupName);
}
