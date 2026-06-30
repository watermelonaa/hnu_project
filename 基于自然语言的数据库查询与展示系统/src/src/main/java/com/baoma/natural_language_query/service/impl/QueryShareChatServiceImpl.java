package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.dto.ShareQueryDTO;
import com.baoma.natural_language_query.entity.mongodb.FriendChat;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.exception.BusinessException;
import com.baoma.natural_language_query.mapper.UserMapper;
import com.baoma.natural_language_query.repository.FriendChatRepository;
import com.baoma.natural_language_query.service.QueryShareChatService;
import com.baoma.natural_language_query.vo.ChatMessageVO;
import com.baoma.natural_language_query.vo.QueryShareVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryShareChatServiceImpl implements QueryShareChatService {

  @Autowired private FriendChatRepository friendChatRepository;

  @Autowired private UserMapper userMapper;

  // 改为独立的删除标记，而不是统一状态位
  private static final String EXTRA_DELETED_BY_SENDER = "deletedBySender";
  private static final String EXTRA_DELETED_BY_RECEIVER = "deletedByReceiver";
  private static final String EXTRA_SAVED_BY_RECEIVER = "savedByReceiver";
  
  // 保留原有的状态位兼容性，但不再主要使用
  private static final int RECEIVE_STATUS_UNHANDLED = 0; // 未处理
  private static final int RECEIVE_STATUS_SAVED = 1;    // 已保存
  private static final int RECEIVE_STATUS_DELETED_BY_RECEIVER = 2; // 接收者删除
  private static final int RECEIVE_STATUS_DELETED_BY_SENDER = 3;   // 发送者删除
  
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  @Transactional
  public ChatMessageVO shareQueryToFriend(ShareQueryDTO dto) {
    // 验证用户
    User shareUser = userMapper.selectById(dto.getShareUserId());
    User receiveUser = userMapper.selectById(dto.getReceiveUserId());

    if (shareUser == null || receiveUser == null) {
      throw new BusinessException("分享者或接收者不存在");
    }

    // 构建查询分享内容
    Map<String, Object> content = new HashMap<>();
    content.put("queryTitle", dto.getQueryTitle());
    content.put("sqlQuery", dto.getSqlQuery());
    content.put("databaseName", dto.getDatabaseName());
    content.put("dbConnectionId", dto.getDbConnectionId());
    content.put("llmName", dto.getLlmName());
    content.put("llmConfigId", dto.getLlmConfigId());
    content.put("executionTime", dto.getExecutionTime());
    content.put("executionTimeText", formatExecutionTime(dto.getExecutionTime()));
    content.put("queryTime", dto.getQueryTime());
    content.put("dialogId", dto.getDialogId());

    // 添加表格数据
    if (dto.getTableData() != null) {
      Map<String, Object> tableData = new HashMap<>();
      tableData.put("headers", dto.getTableData().getHeaders());
      tableData.put("rows", dto.getTableData().getRows());
      tableData.put("totalRows", dto.getTableData().getRows() != null ? dto.getTableData().getRows().size() : 0);
      content.put("tableData", tableData);
    }

    // 添加图表数据
    if (dto.getChartData() != null) {
      Map<String, Object> chartData = new HashMap<>();
      chartData.put("type", dto.getChartData().getType());
      chartData.put("xAxis", dto.getChartData().getXAxis());
      chartData.put("yAxis", dto.getChartData().getYAxis());
      chartData.put("datasets", dto.getChartData().getDatasets());
      content.put("chartData", chartData);
    }

    // 添加分享留言
    if (dto.getShareMessage() != null && !dto.getShareMessage().isEmpty()) {
      content.put("shareMessage", dto.getShareMessage());
    }

    // 创建聊天消息
    FriendChat friendChat = new FriendChat();
    friendChat.setUserId(dto.getShareUserId());
    friendChat.setFriendId(dto.getReceiveUserId());
    friendChat.setContentType("query_share");
    friendChat.setContent(content);
    friendChat.setSendTime(LocalDateTime.now());
    friendChat.setIsRead(false);

    Map<String, Object> extra = new HashMap<>();
    extra.put("receiveStatus", 0); // 0-未处理
    friendChat.setExtra(extra);

    // 保存消息
    FriendChat saved = friendChatRepository.save(friendChat);

    // 转换为VO返回
    return convertToVO(saved, shareUser, receiveUser);
  }

  @Override
  public List<QueryShareVO> getReceivedQueryShares(Long userId) {
    // 查询接收到的所有查询分享消息（按发送时间倒序）
    List<FriendChat> chats = friendChatRepository.findByFriendIdOrderBySendTimeDesc(userId);

    return chats.stream()
        .filter(chat -> "query_share".equals(chat.getContentType()))
        // 过滤掉接收者自己删除的记录（独立标记）
        .filter(chat -> !isDeletedByReceiver(chat))
        .map(this::convertToQueryShareVO)
        .collect(Collectors.toList());
  }

  /**
   * 获取用户分享出去的查询列表
   * 
   * <p>功能说明：
   * <ul>
   *   <li>查询当前用户作为发送者的所有查询分享消息</li>
   *   <li>过滤出contentType为"query_share"的消息</li>
   *   <li>按发送时间倒序排列</li>
   * </ul>
   * 
   * @param userId 用户ID（分享者）
   * @return 查询分享列表
   */
  @Override
  public List<QueryShareVO> getSharedQueries(Long userId) {
    // 查询用户发送的所有消息（按发送时间倒序）
    List<FriendChat> chats = friendChatRepository.findByUserIdOrderBySendTimeDesc(userId);

    // 过滤出查询分享类型的消息并转换为VO
    return chats.stream()
        .filter(chat -> "query_share".equals(chat.getContentType()))
        // 过滤掉发送者自己删除的记录（独立标记）
        .filter(chat -> !isDeletedBySender(chat))
        .map(this::convertToQueryShareVO)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public boolean saveSharedQuery(String shareId, Long userId) {
    FriendChat chat =
        friendChatRepository
            .findById(shareId)
            .orElseThrow(() -> new BusinessException("分享不存在"));

    // 验证接收者
    if (!chat.getFriendId().equals(userId)) {
      throw new BusinessException("无权操作此分享");
    }

    // 检查是否已被接收者删除
    if (isDeletedByReceiver(chat)) {
      throw new BusinessException("该分享已被删除，无法保存");
    }

    // 更新保存标记
    Map<String, Object> extra = chat.getExtra();
    if (extra == null) {
      extra = new HashMap<>();
    }
    // 使用独立标记
    extra.put(EXTRA_SAVED_BY_RECEIVER, true);
    extra.put(EXTRA_SAVED_BY_RECEIVER + "_time", LocalDateTime.now().format(DATE_TIME_FORMATTER));
    // 保留兼容性也设置状态位
    extra.put("receiveStatus", RECEIVE_STATUS_SAVED);
    
    chat.setExtra(extra);
    chat.setIsRead(true);

    friendChatRepository.save(chat);
    return true;
  }

  @Override
  @Transactional
  public boolean deleteSharedQuery(String shareId, Long userId) {
    FriendChat chat =
        friendChatRepository
            .findById(shareId)
            .orElseThrow(() -> new BusinessException("分享不存在"));

    // 允许发送者和接收者删除
    boolean isSender = chat.getUserId().equals(userId);
    boolean isReceiver = chat.getFriendId().equals(userId);
    
    if (!isSender && !isReceiver) {
      throw new BusinessException("无权操作此分享");
    }

    // 更新删除标记（独立标记，不覆盖）
    Map<String, Object> extra = chat.getExtra();
    if (extra == null) {
      extra = new HashMap<>();
    }
    
    if (isSender) {
      // 标记发送者删除
      extra.put(EXTRA_DELETED_BY_SENDER, true);
      extra.put(EXTRA_DELETED_BY_SENDER + "_time", LocalDateTime.now().format(DATE_TIME_FORMATTER));
      // 保留兼容性也设置状态位
      extra.put("receiveStatus", RECEIVE_STATUS_DELETED_BY_SENDER);
    } else {
      // 标记接收者删除
      extra.put(EXTRA_DELETED_BY_RECEIVER, true);
      extra.put(EXTRA_DELETED_BY_RECEIVER + "_time", LocalDateTime.now().format(DATE_TIME_FORMATTER));
      // 保留兼容性也设置状态位
      extra.put("receiveStatus", RECEIVE_STATUS_DELETED_BY_RECEIVER);
    }
    
    chat.setExtra(extra);
    chat.setIsRead(true);

    friendChatRepository.save(chat);
    return true;
  }

  // 检查是否被接收者删除（基于独立标记）
  private boolean isDeletedByReceiver(FriendChat chat) {
    Map<String, Object> extra = chat.getExtra();
    if (extra == null) {
      return false;
    }
    // 优先使用新标记
    Object deletedFlag = extra.get(EXTRA_DELETED_BY_RECEIVER);
    if (deletedFlag instanceof Boolean) {
      return (Boolean) deletedFlag;
    }
    // 兼容旧的状态位
    Object statusObj = extra.get("receiveStatus");
    if (statusObj instanceof Integer) {
      int status = (Integer) statusObj;
      return status == RECEIVE_STATUS_DELETED_BY_RECEIVER;
    }
    return false;
  }

  // 检查是否被发送者删除（基于独立标记）
  private boolean isDeletedBySender(FriendChat chat) {
    Map<String, Object> extra = chat.getExtra();
    if (extra == null) {
      return false;
    }
    // 优先使用新标记
    Object deletedFlag = extra.get(EXTRA_DELETED_BY_SENDER);
    if (deletedFlag instanceof Boolean) {
      return (Boolean) deletedFlag;
    }
    // 兼容旧的状态位
    Object statusObj = extra.get("receiveStatus");
    if (statusObj instanceof Integer) {
      int status = (Integer) statusObj;
      return status == RECEIVE_STATUS_DELETED_BY_SENDER;
    }
    return false;
  }

  // 检查是否被接收者保存
  private boolean isSavedByReceiver(FriendChat chat) {
    Map<String, Object> extra = chat.getExtra();
    if (extra == null) {
      return false;
    }
    Object savedFlag = extra.get(EXTRA_SAVED_BY_RECEIVER);
    if (savedFlag instanceof Boolean) {
      return (Boolean) savedFlag;
    }
    // 兼容旧的状态位
    Object statusObj = extra.get("receiveStatus");
    if (statusObj instanceof Integer) {
      int status = (Integer) statusObj;
      return status == RECEIVE_STATUS_SAVED;
    }
    return false;
  }

  @Override
  public QueryShareVO getQueryShareDetail(String shareId) {
    FriendChat chat =
        friendChatRepository
            .findById(shareId)
            .orElseThrow(() -> new BusinessException("分享不存在"));

    if (!"query_share".equals(chat.getContentType())) {
      throw new BusinessException("不是查询分享消息");
    }

    return convertToQueryShareVO(chat);
  }

  /**
   * 将FriendChat转换为ChatMessageVO
   *
   * @param chat 聊天消息
   * @param sender 发送者
   * @param receiver 接收者
   * @return 聊天消息VO
   */
  private ChatMessageVO convertToVO(FriendChat chat, User sender, User receiver) {
    ChatMessageVO vo = new ChatMessageVO();
    vo.setId(chat.getId());
    vo.setSenderId(chat.getUserId());
    vo.setSenderName(sender != null ? sender.getUsername() : "未知用户");
    vo.setSenderAvatar(sender != null ? sender.getAvatarUrl() : null);
    vo.setReceiverId(chat.getFriendId());
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
   * 将FriendChat转换为QueryShareVO
   *
   * @param chat 聊天消息
   * @return 查询分享VO
   */
  private QueryShareVO convertToQueryShareVO(FriendChat chat) {
    QueryShareVO vo = new QueryShareVO();
    vo.setShareId(chat.getId());
    vo.setShareUserId(chat.getUserId());
    vo.setReceiveUserId(chat.getFriendId());
    vo.setShareTime(chat.getSendTime());

    // 获取用户信息
    User shareUser = userMapper.selectById(chat.getUserId());
    if (shareUser != null) {
      vo.setShareUserName(shareUser.getUsername());
      vo.setShareUserAvatar(shareUser.getAvatarUrl());
    }

    User receiveUser = userMapper.selectById(chat.getFriendId());
    if (receiveUser != null) {
      vo.setReceiveUserName(receiveUser.getUsername());
    }

    // 提取内容
    Map<String, Object> content = chat.getContent();
    if (content != null) {
      vo.setQueryTitle((String) content.get("queryTitle"));
      vo.setSqlQuery((String) content.get("sqlQuery"));
      vo.setDatabaseName((String) content.get("databaseName"));
      vo.setDbConnectionId(getLongValue(content.get("dbConnectionId")));
      vo.setLlmName((String) content.get("llmName"));
      vo.setLlmConfigId(getLongValue(content.get("llmConfigId")));
      vo.setExecutionTime(getLongValue(content.get("executionTime")));
      vo.setExecutionTimeText((String) content.get("executionTimeText"));
      vo.setQueryTime((String) content.get("queryTime"));
      vo.setDialogId((String) content.get("dialogId"));
      vo.setShareMessage((String) content.get("shareMessage"));

      // 提取表格数据
      Object tableDataObj = content.get("tableData");
      if (tableDataObj instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> tableDataMap = (Map<String, Object>) tableDataObj;
        QueryShareVO.TableDataVO tableData = new QueryShareVO.TableDataVO();
        
        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) tableDataMap.get("headers");
        tableData.setHeaders(headers);
        
        @SuppressWarnings("unchecked")
        List<List<Object>> rows = (List<List<Object>>) tableDataMap.get("rows");
        tableData.setRows(rows);
        tableData.setTotalRows(rows != null ? rows.size() : 0);
        
        vo.setTableData(tableData);
      }

      // 提取图表数据
      Object chartDataObj = content.get("chartData");
      if (chartDataObj instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> chartDataMap = (Map<String, Object>) chartDataObj;
        QueryShareVO.ChartDataVO chartData = new QueryShareVO.ChartDataVO();
        chartData.setType((String) chartDataMap.get("type"));
        
        @SuppressWarnings("unchecked")
        List<String> xAxis = (List<String>) chartDataMap.get("xAxis");
        chartData.setXAxis(xAxis);
        
        @SuppressWarnings("unchecked")
        List<Object> yAxis = (List<Object>) chartDataMap.get("yAxis");
        chartData.setYAxis(yAxis);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> datasets = (List<Map<String, Object>>) chartDataMap.get("datasets");
        chartData.setDatasets(datasets);
        
        vo.setChartData(chartData);
      }
    }

    // 提取接收状态
    Map<String, Object> extra = chat.getExtra();
    if (extra != null && extra.containsKey("receiveStatus")) {
      vo.setReceiveStatus(getIntValue(extra.get("receiveStatus")));
    } else {
      vo.setReceiveStatus(0);
    }

    return vo;
  }

  /**
   * 格式化执行时间
   *
   * @param executionTime 执行时间（毫秒）
   * @return 格式化字符串
   */
  private String formatExecutionTime(Long executionTime) {
    if (executionTime == null) {
      return "0ms";
    }
    if (executionTime < 1000) {
      return executionTime + "ms";
    }
    double seconds = executionTime / 1000.0;
    return String.format("%.2fs", seconds);
  }

  /**
   * 安全地将Object转换为Long
   */
  private Long getLongValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Long) {
      return (Long) value;
    }
    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    }
    if (value instanceof String) {
      try {
        return Long.parseLong((String) value);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * 安全地将Object转换为Integer
   */
  private Integer getIntValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof Long) {
      return ((Long) value).intValue();
    }
    if (value instanceof String) {
      try {
        return Integer.parseInt((String) value);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
}

