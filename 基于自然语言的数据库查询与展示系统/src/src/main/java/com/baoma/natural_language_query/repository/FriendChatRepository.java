package com.baoma.natural_language_query.repository;

import com.baoma.natural_language_query.entity.mongodb.FriendChat;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendChatRepository extends MongoRepository<FriendChat, String> {

  List<FriendChat> findByUserIdAndFriendIdOrderBySendTimeDesc(Long userId, Long friendId);

  List<FriendChat> findByFriendIdAndIsRead(Long friendId, Boolean isRead);

  /**
   * 查询两个用户之间的聊天记录（双向）
   *
   * @param userId1 用户1的ID
   * @param userId2 用户2的ID
   * @param pageable 分页参数
   * @return 分页聊天记录
   */
  @Query(
      "{ $or: [ { 'userId': ?0, 'friendId': ?1 }, { 'userId': ?1, 'friendId': ?0 } ] }")
  Page<FriendChat> findChatsBetweenUsers(Long userId1, Long userId2, Pageable pageable);

  /**
   * 查询两个用户之间指定时间之前的聊天记录
   *
   * @param userId1 用户1的ID
   * @param userId2 用户2的ID
   * @param beforeTime 指定时间
   * @param pageable 分页参数
   * @return 分页聊天记录
   */
  @Query(
      "{ $or: [ { 'userId': ?0, 'friendId': ?1 }, { 'userId': ?1, 'friendId': ?0 } ], 'sendTime': { $lt: ?2 } }")
  Page<FriendChat> findChatsBetweenUsersBeforeTime(
      Long userId1, Long userId2, LocalDateTime beforeTime, Pageable pageable);

  /**
   * 统计某用户收到的未读消息数量
   *
   * @param friendId 接收者用户ID
   * @param isRead 是否已读
   * @return 未读消息数量
   */
  long countByFriendIdAndIsRead(Long friendId, Boolean isRead);

  /**
   * 统计来自指定用户的未读消息数量
   *
   * @param userId 发送者用户ID
   * @param friendId 接收者用户ID
   * @param isRead 是否已读
   * @return 未读消息数量
   */
  long countByUserIdAndFriendIdAndIsRead(Long userId, Long friendId, Boolean isRead);

  /**
   * 查询用户收到的所有未读消息
   *
   * @param friendId 接收者用户ID
   * @param isRead 是否已读
   * @return 未读消息列表
   */
  List<FriendChat> findByFriendIdAndIsReadOrderBySendTimeDesc(Long friendId, Boolean isRead);

  /**
   * 查询指定发送者发给指定接收者的未读消息
   *
   * @param userId 发送者用户ID
   * @param friendId 接收者用户ID
   * @param isRead 是否已读
   * @return 未读消息列表
   */
  List<FriendChat> findByUserIdAndFriendIdAndIsRead(Long userId, Long friendId, Boolean isRead);

  /**
   * 查询用户收到的所有消息（按发送时间倒序）
   *
   * @param friendId 接收者用户ID
   * @return 消息列表
   */
  List<FriendChat> findByFriendIdOrderBySendTimeDesc(Long friendId);

  /**
   * 查询用户发送的所有消息（按发送时间倒序）
   * 
   * <p>用于查询用户分享出去的查询记录
   *
   * @param userId 发送者用户ID
   * @return 消息列表
   */
  List<FriendChat> findByUserIdOrderBySendTimeDesc(Long userId);
}
