package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mongodb.QueryCollection;
import java.util.List;

public interface QueryCollectionService {

  List<QueryCollection> listByUserId(Long userId);

  QueryCollection getByUserIdAndGroupName(Long userId, String groupName);

  QueryCollection getById(String id);

  QueryCollection save(QueryCollection queryCollection);

  void deleteById(String id);
}
