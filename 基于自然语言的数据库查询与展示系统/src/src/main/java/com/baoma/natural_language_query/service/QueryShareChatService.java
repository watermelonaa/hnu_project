package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.dto.ShareQueryDTO;
import com.baoma.natural_language_query.vo.ChatMessageVO;
import com.baoma.natural_language_query.vo.QueryShareVO;
import java.util.List;

/**
 * 查询分享聊天服务接口
 *
 * <p>处理好友之间分享查询记录的业务逻辑
 */
public interface QueryShareChatService {

  /**
   * 分享查询记录给好友（通过聊天）
   *
   * @param dto 分享查询DTO
   * @return 聊天消息VO
   */
  ChatMessageVO shareQueryToFriend(ShareQueryDTO dto);

  /**
   * 获取用户收到的查询分享列表
   *
   * @param userId 用户ID
   * @return 查询分享列表
   */
  List<QueryShareVO> getReceivedQueryShares(Long userId);

  /**
   * 获取用户分享出去的查询列表
   *
   * @param userId 用户ID
   * @return 查询分享列表
   */
  List<QueryShareVO> getSharedQueries(Long userId);

  /**
   * 保存收到的查询分享
   *
   * @param shareId 分享ID
   * @param userId 用户ID
   * @return 是否成功
   */
  boolean saveSharedQuery(String shareId, Long userId);

  /**
   * 删除收到的查询分享
   *
   * @param shareId 分享ID
   * @param userId 用户ID
   * @return 是否成功
   */
  boolean deleteSharedQuery(String shareId, Long userId);

  /**
   * 获取查询分享详情
   *
   * @param shareId 分享ID
   * @return 查询分享详情
   */
  QueryShareVO getQueryShareDetail(String shareId);
}

