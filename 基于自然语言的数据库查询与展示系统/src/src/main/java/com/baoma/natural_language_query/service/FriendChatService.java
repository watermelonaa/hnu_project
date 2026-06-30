package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.dto.ChatMessagePageDTO;
import com.baoma.natural_language_query.dto.SendChatMessageDTO;
import com.baoma.natural_language_query.entity.mongodb.FriendChat;
import com.baoma.natural_language_query.vo.ChatMessageVO;
import com.baoma.natural_language_query.vo.UnreadCountVO;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public interface FriendChatService {

  /**
   * 查询两个用户之间的聊天记录
   *
   * @param userId 用户ID
   * @param friendId 好友ID
   * @return 聊天记录列表
   */
  List<FriendChat> listByUserIdAndFriendId(Long userId, Long friendId);

  /**
   * 查询用户收到的所有未读消息
   *
   * @param friendId 用户ID
   * @return 未读消息列表
   */
  List<FriendChat> listUnreadByFriendId(Long friendId);

  /**
   * 保存聊天消息
   *
   * @param friendChat 聊天消息
   * @return 保存后的消息
   */
  FriendChat save(FriendChat friendChat);

  /**
   * 根据ID查询聊天消息
   *
   * @param id 消息ID
   * @return 聊天消息
   */
  FriendChat getById(String id);

  /**
   * 发送聊天消息
   *
   * @param dto 发送消息DTO
   * @return 保存后的消息VO
   */
  ChatMessageVO sendMessage(SendChatMessageDTO dto);

  /**
   * 删除聊天消息
   *
   * @param id 消息ID
   */
  void deleteById(String id);

  /**
   * 分页查询聊天记录
   *
   * @param dto 分页查询参数
   * @return 分页聊天记录VO
   */
  Page<ChatMessageVO> getChatHistoryPage(ChatMessagePageDTO dto);

  /**
   * 批量标记消息为已读
   *
   * @param userId 当前用户ID
   * @param friendId 好友ID
   * @return 标记数量
   */
  int markMessagesAsRead(Long userId, Long friendId);

  /**
   * 标记单条消息为已读
   *
   * @param messageId 消息ID
   * @return 是否成功
   */
  boolean markMessageAsRead(String messageId);

  /**
   * 获取用户的未读消息统计（按好友分组）
   *
   * @param userId 用户ID
   * @return 未读消息统计列表
   */
  List<UnreadCountVO> getUnreadCountByFriends(Long userId);

  /**
   * 获取来自指定好友的未读消息数量
   *
   * @param userId 当前用户ID
   * @param friendId 好友ID
   * @return 未读消息数量
   */
  long getUnreadCountFromFriend(Long userId, Long friendId);

  /**
   * 获取用户的最近聊天列表（包含最后一条消息）
   *
   * @param userId 用户ID
   * @return 最近聊天列表
   */
  List<Map<String, Object>> getRecentChats(Long userId);
}
