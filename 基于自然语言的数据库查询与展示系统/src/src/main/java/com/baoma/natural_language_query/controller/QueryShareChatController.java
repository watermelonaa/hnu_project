package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.ShareQueryDTO;
import com.baoma.natural_language_query.service.QueryShareChatService;
import com.baoma.natural_language_query.vo.ChatMessageVO;
import com.baoma.natural_language_query.vo.QueryShareVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查询分享聊天控制器
 *
 * <p>提供好友之间分享查询记录的API接口
 */
@RestController
@RequestMapping("/query-share-chat")
public class QueryShareChatController {

  @Autowired private QueryShareChatService queryShareChatService;

  @Autowired private SimpMessagingTemplate messagingTemplate;

  /**
   * 分享查询记录给好友
   *
   * @param dto 分享查询DTO
   * @return 聊天消息VO
   */
  @PostMapping("/share")
  public Result<ChatMessageVO> shareQueryToFriend(@RequestBody ShareQueryDTO dto) {
    try {
      ChatMessageVO message = queryShareChatService.shareQueryToFriend(dto);

      // 通过WebSocket推送消息给接收者
      messagingTemplate.convertAndSendToUser(
          dto.getReceiveUserId().toString(), "/queue/messages", message);

      // 也发送给发送者（多设备同步）
      messagingTemplate.convertAndSendToUser(
          dto.getShareUserId().toString(), "/queue/messages", message);

      return Result.success(message);
    } catch (Exception e) {
      return Result.error(e.getMessage());
    }
  }

  /**
   * 获取收到的查询分享列表
   *
   * @param userId 用户ID
   * @return 查询分享列表
   */
  @GetMapping("/received/{userId}")
  public Result<List<QueryShareVO>> getReceivedQueryShares(@PathVariable Long userId) {
    List<QueryShareVO> shares = queryShareChatService.getReceivedQueryShares(userId);
    return Result.success(shares);
  }

  /**
   * 获取分享出去的查询列表
   *
   * @param userId 用户ID
   * @return 查询分享列表
   */
  @GetMapping("/shared/{userId}")
  public Result<List<QueryShareVO>> getSharedQueries(@PathVariable Long userId) {
    List<QueryShareVO> shares = queryShareChatService.getSharedQueries(userId);
    return Result.success(shares);
  }

  /**
   * 获取查询分享详情
   *
   * @param shareId 分享ID
   * @return 查询分享详情
   */
  @GetMapping("/detail/{shareId}")
  public Result<QueryShareVO> getQueryShareDetail(@PathVariable String shareId) {
    QueryShareVO share = queryShareChatService.getQueryShareDetail(shareId);
    return Result.success(share);
  }

  /**
   * 保存收到的查询分享
   *
   * @param shareId 分享ID
   * @param userId 用户ID
   * @return 操作结果
   */
  @PutMapping("/save/{shareId}/{userId}")
  public Result<Map<String, Object>> saveSharedQuery(
      @PathVariable String shareId, @PathVariable Long userId) {
    boolean success = queryShareChatService.saveSharedQuery(shareId, userId);
    Map<String, Object> result = new HashMap<>();
    result.put("success", success);
    result.put("message", success ? "保存成功" : "保存失败");
    return Result.success(result);
  }

  /**
   * 删除收到的查询分享
   *
   * @param shareId 分享ID
   * @param userId 用户ID
   * @return 操作结果
   */
  @DeleteMapping("/delete/{shareId}/{userId}")
  public Result<Map<String, Object>> deleteSharedQuery(
      @PathVariable String shareId, @PathVariable Long userId) {
    boolean success = queryShareChatService.deleteSharedQuery(shareId, userId);
    Map<String, Object> result = new HashMap<>();
    result.put("success", success);
    result.put("message", success ? "删除成功" : "删除失败");
    return Result.success(result);
  }
}

