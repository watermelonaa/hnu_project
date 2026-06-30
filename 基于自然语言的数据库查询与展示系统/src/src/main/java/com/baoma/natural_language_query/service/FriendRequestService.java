package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.FriendRequest;
import java.util.List;

public interface FriendRequestService {

  List<FriendRequest> listByRecipientId(Long recipientId);

  List<FriendRequest> listByRecipientIdAndStatus(Long recipientId, Integer status);

  FriendRequest getById(Long id);

  boolean save(FriendRequest friendRequest);

  boolean updateById(FriendRequest friendRequest);

  boolean removeById(Long id);

  boolean acceptRequest(Long requestId);

  boolean rejectRequest(Long requestId);
}
