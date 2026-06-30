package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.QueryShare;
import java.util.List;

public interface QueryShareService {

  List<QueryShare> listByReceiveUserId(Long receiveUserId);

  List<QueryShare> listByReceiveUserIdAndStatus(Long receiveUserId, Integer receiveStatus);

  List<QueryShare> listByShareUserId(Long shareUserId);

  QueryShare getById(Long id);

  boolean save(QueryShare queryShare);

  boolean updateById(QueryShare queryShare);

  boolean removeById(Long id);
}
