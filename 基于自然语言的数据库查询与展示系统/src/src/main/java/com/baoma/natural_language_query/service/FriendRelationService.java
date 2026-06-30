package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.FriendRelation;
import java.util.List;

public interface FriendRelationService {

  List<FriendRelation> listByUserId(Long userId);

  FriendRelation getById(Long id);

  FriendRelation getByUserIdAndFriendId(Long userId, Long friendId);

  boolean save(FriendRelation friendRelation);

  boolean updateById(FriendRelation friendRelation);

  boolean removeById(Long id);

  boolean removeByUserIdAndFriendId(Long userId, Long friendId);
}
