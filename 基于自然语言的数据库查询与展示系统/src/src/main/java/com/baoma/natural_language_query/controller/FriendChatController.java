package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.ChatMessagePageDTO;
import com.baoma.natural_language_query.dto.SendChatMessageDTO;
import com.baoma.natural_language_query.entity.mongodb.FriendChat;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.FriendChatService;
import com.baoma.natural_language_query.utils.PermissionUtil;
import com.baoma.natural_language_query.vo.ChatMessageVO;
import com.baoma.natural_language_query.vo.UnreadCountVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 好友聊天控制器
 */
@RestController
@RequestMapping("/friend-chat")
public class FriendChatController {

  @Autowired private FriendChatService friendChatService;

  @GetMapping("/list/{userId}/{friendId}")
  @RequirePermission(Role.USER)
  public Result<List<FriendChat>> listByUserIdAndFriendId(
      @PathVariable Long userId, @PathVariable Long friendId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权查看他人的聊天记录");
    }
    return Result.success(friendChatService.listByUserIdAndFriendId(userId, friendId));
  }

  @PostMapping("/history/page")
  @RequirePermission(Role.USER)
  public Result<Page<ChatMessageVO>> getChatHistoryPage(@RequestBody ChatMessagePageDTO dto, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, dto.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权查看他人的聊天记录");
    }
    Page<ChatMessageVO> page = friendChatService.getChatHistoryPage(dto);
    return Result.success(page);
  }

  @GetMapping("/unread/{friendId}")
  @RequirePermission(Role.USER)
  public Result<List<FriendChat>> listUnreadByFriendId(@PathVariable Long friendId, HttpServletRequest request) {
    // 这里 friendId 实际上是接收者 ID
    if (!PermissionUtil.isCurrentUser(request, friendId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权查看他人的未读消息");
    }
    return Result.success(friendChatService.listUnreadByFriendId(friendId));
  }

  @GetMapping("/unread/count/{userId}")
  @RequirePermission(Role.USER)
  public Result<List<UnreadCountVO>> getUnreadCountByFriends(@PathVariable Long userId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权查看他人的未读消息统计");
    }
    List<UnreadCountVO> counts = friendChatService.getUnreadCountByFriends(userId);
    return Result.success(counts);
  }

  @GetMapping("/unread/count/{userId}/{friendId}")
  @RequirePermission(Role.USER)
  public Result<Map<String, Object>> getUnreadCountFromFriend(
      @PathVariable Long userId, @PathVariable Long friendId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权查看他人的未读消息统计");
    }
    long count = friendChatService.getUnreadCountFromFriend(userId, friendId);
    Map<String, Object> result = new HashMap<>();
    result.put("userId", userId);
    result.put("friendId", friendId);
    result.put("unreadCount", count);
    return Result.success(result);
  }

  @GetMapping("/recent/{userId}")
  @RequirePermission(Role.USER)
  public Result<List<Map<String, Object>>> getRecentChats(@PathVariable Long userId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权查看他人的最近聊天列表");
    }
    List<Map<String, Object>> recentChats = friendChatService.getRecentChats(userId);
    return Result.success(recentChats);
  }

  @PostMapping("/send")
  @RequirePermission(Role.USER)
  public Result<ChatMessageVO> sendMessage(@RequestBody SendChatMessageDTO dto, HttpServletRequest request) {
    try {
      if (!PermissionUtil.isCurrentUser(request, dto.getSenderId()) && !PermissionUtil.isAdmin(request)) {
        return Result.error("权限不足，不能以他人身份发送消息");
      }
      ChatMessageVO message = friendChatService.sendMessage(dto);
      return Result.success(message);
    } catch (Exception e) {
      return Result.error("发送消息失败: " + e.getMessage());
    }
  }

  @PostMapping
  @RequirePermission(Role.ADMIN) // 直接保存由系统逻辑控制，手动调用限管理员
  public Result<FriendChat> save(@RequestBody FriendChat friendChat) {
    friendChat.setSendTime(LocalDateTime.now());
    if (friendChat.getIsRead() == null) {
      friendChat.setIsRead(false);
    }
    FriendChat saved = friendChatService.save(friendChat);
    return Result.success(saved);
  }

  @PutMapping("/mark-read")
  @RequirePermission(Role.USER)
  public Result<Map<String, Object>> markMessagesAsRead(
      @RequestParam Long userId, @RequestParam Long friendId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权处理他人的消息已读状态");
    }
    int count = friendChatService.markMessagesAsRead(userId, friendId);
    Map<String, Object> result = new HashMap<>();
    result.put("markedCount", count);
    result.put("message", "成功标记 " + count + " 条消息为已读");
    return Result.success(result);
  }

  @PutMapping("/mark-read/{messageId}")
  @RequirePermission(Role.USER)
  public Result<Map<String, Object>> markMessageAsRead(@PathVariable String messageId, HttpServletRequest request) {
    // 逻辑层应该校验消息接收者是否为当前用户
    boolean success = friendChatService.markMessageAsRead(messageId);
    Map<String, Object> result = new HashMap<>();
    result.put("success", success);
    result.put("message", success ? "标记成功" : "消息已读或不存在");
    return Result.success(result);
  }

  @PutMapping
  @RequirePermission(Role.ADMIN)
  public Result<FriendChat> update(@RequestBody FriendChat friendChat) {
    FriendChat saved = friendChatService.save(friendChat);
    return Result.success(saved);
  }

  @DeleteMapping("/{id}")
  @RequirePermission(Role.ADMIN)
  public Result<Void> delete(@PathVariable String id) {
    friendChatService.deleteById(id);
    return Result.success();
  }
}
