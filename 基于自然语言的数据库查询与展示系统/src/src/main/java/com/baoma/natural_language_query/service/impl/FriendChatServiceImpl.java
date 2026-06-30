package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.dto.ChatMessagePageDTO;
import com.baoma.natural_language_query.dto.SendChatMessageDTO;
import com.baoma.natural_language_query.entity.mongodb.FriendChat;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.exception.BusinessException;
import com.baoma.natural_language_query.mapper.UserMapper;
import com.baoma.natural_language_query.repository.FriendChatRepository;
import com.baoma.natural_language_query.service.FriendChatService;
import com.baoma.natural_language_query.vo.ChatMessageVO;
import com.baoma.natural_language_query.vo.UnreadCountVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendChatServiceImpl implements FriendChatService {

  @Autowired private FriendChatRepository friendChatRepository;

  @Autowired private UserMapper userMapper;

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public List<FriendChat> listByUserIdAndFriendId(Long userId, Long friendId) {
    return friendChatRepository.findByUserIdAndFriendIdOrderBySendTimeDesc(userId, friendId);
  }

  @Override
  public List<FriendChat> listUnreadByFriendId(Long friendId) {
    return friendChatRepository.findByFriendIdAndIsReadOrderBySendTimeDesc(friendId, false);
  }

  @Override
  @Transactional
  public FriendChat save(FriendChat friendChat) {
    return friendChatRepository.save(friendChat);
  }

  @Override
  public FriendChat getById(String id) {
    return friendChatRepository.findById(id).orElse(null);
  }

  @Override
  @Transactional
  public ChatMessageVO sendMessage(SendChatMessageDTO dto) {
    // 验证发送者和接收者
    User sender = userMapper.selectById(dto.getSenderId());
    User receiver = userMapper.selectById(dto.getReceiverId());

    if (sender == null || receiver == null) {
      throw new BusinessException("发送者或接收者不存在");
    }

    // 创建聊天消息
    FriendChat friendChat = new FriendChat();
    friendChat.setUserId(dto.getSenderId());
    friendChat.setFriendId(dto.getReceiverId());
    friendChat.setContentType(dto.getContentType());
    friendChat.setContent(dto.getContent());
    friendChat.setSendTime(LocalDateTime.now());
    friendChat.setIsRead(false);
    friendChat.setExtra(dto.getExtra());

    // 保存消息
    FriendChat saved = friendChatRepository.save(friendChat);

    // 转换为VO返回
    return convertToVO(saved, sender, receiver);
  }

  @Override
  public void deleteById(String id) {
    friendChatRepository.deleteById(id);
  }

  @Override
  public Page<ChatMessageVO> getChatHistoryPage(ChatMessagePageDTO dto) {
    // 构建分页参数（按时间倒序）
    Pageable pageable =
        PageRequest.of(dto.getPage() - 1, dto.getPageSize(), Sort.by(Sort.Direction.DESC, "sendTime"));

    Page<FriendChat> chatPage;

    // 如果提供了最后一条消息时间，则查询该时间之前的消息
    if (dto.getLastMessageTime() != null && !dto.getLastMessageTime().isEmpty()) {
      LocalDateTime beforeTime = LocalDateTime.parse(dto.getLastMessageTime(), DATE_TIME_FORMATTER);
      chatPage =
          friendChatRepository.findChatsBetweenUsersBeforeTime(
              dto.getUserId(), dto.getFriendId(), beforeTime, pageable);
    } else {
      chatPage =
          friendChatRepository.findChatsBetweenUsers(dto.getUserId(), dto.getFriendId(), pageable);
    }

    // 获取用户信息
    User user = userMapper.selectById(dto.getUserId());
    User friend = userMapper.selectById(dto.getFriendId());

    // 转换为VO
    return chatPage.map(
        chat -> {
          // userId是发送者ID，friendId是接收者ID
          // 如果chat.getUserId()等于dto.getUserId()，说明是当前用户发送的消息
          boolean isSentByCurrentUser = chat.getUserId().equals(dto.getUserId());
          User sender = isSentByCurrentUser ? user : friend;
          User receiver = isSentByCurrentUser ? friend : user;
          return convertToVO(chat, sender, receiver);
        });
  }

  @Override
  @Transactional
  public int markMessagesAsRead(Long userId, Long friendId) {
    // 查询未读消息
    List<FriendChat> unreadMessages =
        friendChatRepository.findByUserIdAndFriendIdAndIsRead(friendId, userId, false);

    // 批量标记为已读
    for (FriendChat message : unreadMessages) {
      message.setIsRead(true);
    }

    if (!unreadMessages.isEmpty()) {
      friendChatRepository.saveAll(unreadMessages);
    }

    return unreadMessages.size();
  }

  @Override
  @Transactional
  public boolean markMessageAsRead(String messageId) {
    FriendChat message =
        friendChatRepository
            .findById(messageId)
            .orElseThrow(() -> new BusinessException("消息不存在"));

    if (!message.getIsRead()) {
      message.setIsRead(true);
      friendChatRepository.save(message);
      return true;
    }

    return false;
  }

  @Override
  public List<UnreadCountVO> getUnreadCountByFriends(Long userId) {
    // 获取所有未读消息
    List<FriendChat> unreadMessages =
        friendChatRepository.findByFriendIdAndIsReadOrderBySendTimeDesc(userId, false);

    // 按发送者分组统计
    Map<Long, List<FriendChat>> groupedMessages =
        unreadMessages.stream().collect(Collectors.groupingBy(FriendChat::getUserId));

    // 构建统计结果
    List<UnreadCountVO> result = new ArrayList<>();
    for (Map.Entry<Long, List<FriendChat>> entry : groupedMessages.entrySet()) {
      Long friendId = entry.getKey();
      List<FriendChat> messages = entry.getValue();

      User friend = userMapper.selectById(friendId);
      if (friend != null) {
        // 获取最后一条消息
        FriendChat lastMessage = messages.get(0); // 已按时间倒序排列

        String preview = getMessagePreview(lastMessage);
        String lastTime = lastMessage.getSendTime().format(DATE_TIME_FORMATTER);

        UnreadCountVO vo =
            new UnreadCountVO(
                friendId,
                friend.getUsername(),
                friend.getAvatarUrl(),
                (long) messages.size(),
                preview,
                lastTime);

        result.add(vo);
      }
    }

    // 按最后消息时间倒序排列
    result.sort(
        (a, b) ->
            LocalDateTime.parse(b.getLastMessageTime(), DATE_TIME_FORMATTER)
                .compareTo(LocalDateTime.parse(a.getLastMessageTime(), DATE_TIME_FORMATTER)));

    return result;
  }

  @Override
  public long getUnreadCountFromFriend(Long userId, Long friendId) {
    return friendChatRepository.countByUserIdAndFriendIdAndIsRead(friendId, userId, false);
  }

  @Override
  public List<Map<String, Object>> getRecentChats(Long userId) {
    // 查询所有与该用户相关的聊天记录（作为发送者或接收者）
    // 这里需要自定义查询，先获取所有消息，然后处理
    List<FriendChat> allMessages = friendChatRepository.findAll();

    // 筛选与当前用户相关的消息
    List<FriendChat> userMessages =
        allMessages.stream()
            .filter(msg -> msg.getUserId().equals(userId) || msg.getFriendId().equals(userId))
            .collect(Collectors.toList());

    // 按好友分组，获取最后一条消息
    Map<Long, FriendChat> latestMessageMap = new HashMap<>();
    for (FriendChat message : userMessages) {
      Long friendId = message.getUserId().equals(userId) ? message.getFriendId() : message.getUserId();

      FriendChat existingMessage = latestMessageMap.get(friendId);
      if (existingMessage == null
          || message.getSendTime().isAfter(existingMessage.getSendTime())) {
        latestMessageMap.put(friendId, message);
      }
    }

    // 构建返回结果
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map.Entry<Long, FriendChat> entry : latestMessageMap.entrySet()) {
      Long friendId = entry.getKey();
      FriendChat lastMessage = entry.getValue();

      User friend = userMapper.selectById(friendId);
      if (friend != null) {
        Map<String, Object> chatInfo = new HashMap<>();
        chatInfo.put("friendId", friendId);
        chatInfo.put("friendName", friend.getUsername());
        chatInfo.put("friendAvatar", friend.getAvatarUrl());
        chatInfo.put("onlineStatus", friend.getOnlineStatus());
        chatInfo.put("lastMessagePreview", getMessagePreview(lastMessage));
        chatInfo.put("lastMessageTime", lastMessage.getSendTime().format(DATE_TIME_FORMATTER));

        // 获取未读消息数量
        long unreadCount = getUnreadCountFromFriend(userId, friendId);
        chatInfo.put("unreadCount", unreadCount);

        result.add(chatInfo);
      }
    }

    // 按最后消息时间倒序排列
    result.sort(
        Comparator.comparing(
            (Map<String, Object> o) ->
                LocalDateTime.parse((String) o.get("lastMessageTime"), DATE_TIME_FORMATTER),
            Comparator.reverseOrder()));

    return result;
  }

  /**
   * 将FriendChat转换为ChatMessageVO
   *
   * @param chat 聊天消息实体
   * @param sender 发送者
   * @param receiver 接收者
   * @return 聊天消息VO
   */
  private ChatMessageVO convertToVO(FriendChat chat, User sender, User receiver) {
    ChatMessageVO vo = new ChatMessageVO();
    vo.setId(chat.getId());
    // 使用sender和receiver的ID，而不是chat的userId和friendId
    // 因为chat的userId和friendId是存储时的原始值，而sender和receiver是根据查询上下文确定的
    vo.setSenderId(sender != null ? sender.getId() : chat.getUserId());
    vo.setSenderName(sender != null ? sender.getUsername() : "未知用户");
    vo.setSenderAvatar(sender != null ? sender.getAvatarUrl() : null);
    vo.setReceiverId(receiver != null ? receiver.getId() : chat.getFriendId());
    vo.setReceiverName(receiver != null ? receiver.getUsername() : "未知用户");
    vo.setReceiverAvatar(receiver != null ? receiver.getAvatarUrl() : null);
    vo.setContentType(chat.getContentType());
    vo.setContent(chat.getContent());
    vo.setSendTime(chat.getSendTime());
    vo.setIsRead(chat.getIsRead());
    vo.setExtra(chat.getExtra());
    return vo;
  }

  /**
   * 获取消息预览文本
   *
   * @param message 消息
   * @return 预览文本
   */
  private String getMessagePreview(FriendChat message) {
    if (message.getContent() == null) {
      return "";
    }

    String contentType = message.getContentType();
    Map<String, Object> content = message.getContent();

    switch (contentType) {
      case "text":
        String text = (String) content.get("text");
        return text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text;
      case "image":
        return "[图片]";
      case "file":
        return "[文件]";
      case "query_share":
        return "[查询分享]";
      default:
        return "[消息]";
    }
  }
}
