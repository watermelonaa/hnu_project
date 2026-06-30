package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mongodb.QueryCollection;
import com.baoma.natural_language_query.repository.QueryCollectionRepository;
import com.baoma.natural_language_query.service.QueryCollectionService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueryCollectionServiceImpl implements QueryCollectionService {

  @Autowired private QueryCollectionRepository queryCollectionRepository;

  @Override
  public List<QueryCollection> listByUserId(Long userId) {
    return queryCollectionRepository.findByUserId(userId);
  }

  @Override
  public QueryCollection getByUserIdAndGroupName(Long userId, String groupName) {
    return queryCollectionRepository.findByUserIdAndGroupName(userId, groupName);
  }

  @Override
  public QueryCollection getById(String id) {
    return queryCollectionRepository.findById(id).orElse(null);
  }

  @Override
  public QueryCollection save(QueryCollection queryCollection) {
    return queryCollectionRepository.save(queryCollection);
  }

  @Override
  public void deleteById(String id) {
    queryCollectionRepository.deleteById(id);
  }
}
