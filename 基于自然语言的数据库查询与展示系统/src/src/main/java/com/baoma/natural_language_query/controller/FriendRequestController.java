package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.FriendRequest;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.FriendRequestService;
import com.baoma.natural_language_query.utils.PermissionUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friend-request")
public class FriendRequestController {

  @Autowired private FriendRequestService friendRequestService;

  @GetMapping("/list/{recipientId}")
  @RequirePermission(Role.USER)
  public Result<List<FriendRequest>> listByRecipientId(@PathVariable Long recipientId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, recipientId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能查看发送给自己的好友请求");
    }
    return Result.success(friendRequestService.listByRecipientId(recipientId));
  }

  @GetMapping("/list/{recipientId}/{status}")
  @RequirePermission(Role.USER)
  public Result<List<FriendRequest>> listByRecipientIdAndStatus(
      @PathVariable Long recipientId, @PathVariable Integer status, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, recipientId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能查看发送给自己的好友请求");
    }
    return Result.success(friendRequestService.listByRecipientIdAndStatus(recipientId, status));
  }

  @GetMapping("/{id}")
  @RequirePermission(Role.USER)
  public Result<FriendRequest> getById(@PathVariable Long id, HttpServletRequest request) {
    FriendRequest friendRequest = friendRequestService.getById(id);
    if (friendRequest != null) {
      if (!PermissionUtil.isCurrentUser(request, friendRequest.getApplicantId()) && 
          !PermissionUtil.isCurrentUser(request, friendRequest.getRecipientId()) && 
          !PermissionUtil.isAdmin(request)) {
        return Result.error("权限不足，无权查看此好友请求");
      }
      return Result.success(friendRequest);
    }
    return Result.error("好友请求不存在");
  }

  @PostMapping
  @RequirePermission(Role.USER)
  public Result<FriendRequest> save(@RequestBody FriendRequest friendRequest, HttpServletRequest request) {
    try {
      Long currentUserId = (Long) request.getAttribute("userId");
      if (currentUserId == null) {
        return Result.error("未登录或登录已过期");
      }
      
      friendRequest.setApplicantId(currentUserId); // 强制申请人为当前登录用户
      friendRequest.setCreateTime(LocalDateTime.now());
      if (friendRequest.getStatus() == null) {
        friendRequest.setStatus(0);
      }
      friendRequestService.save(friendRequest);
      return Result.success(friendRequest);
    } catch (RuntimeException e) {
      System.out.println("发送好友请求失败: " + e.getMessage());
      return Result.error(e.getMessage());
    }
  }

  @PutMapping
  @RequirePermission(Role.USER)
  public Result<FriendRequest> update(@RequestBody FriendRequest friendRequest, HttpServletRequest request) {
    FriendRequest existing = friendRequestService.getById(friendRequest.getId());
    if (existing == null) {
      return Result.error("好友请求不存在");
    }
    
    // 只有接收人或管理员可以修改请求状态
    if (!PermissionUtil.isCurrentUser(request, existing.getRecipientId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权处理此好友请求");
    }

    if (friendRequest.getStatus() != null && friendRequest.getStatus() != 0) {
      friendRequest.setHandleTime(LocalDateTime.now());
    }
    friendRequestService.updateById(friendRequest);
    return Result.success(friendRequest);
  }

  @DeleteMapping("/{id}")
  @RequirePermission(Role.USER)
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    FriendRequest existing = friendRequestService.getById(id);
    if (existing == null) {
      return Result.error("好友请求不存在");
    }
    
    // 只有申请人、接收人或管理员可以删除
    if (!PermissionUtil.isCurrentUser(request, existing.getApplicantId()) && 
        !PermissionUtil.isCurrentUser(request, existing.getRecipientId()) && 
        !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权删除此好友请求");
    }

    friendRequestService.removeById(id);
    return Result.success();
  }

  @PostMapping("/{id}/accept")
  @RequirePermission(Role.USER)
  public Result<String> acceptRequest(@PathVariable Long id, HttpServletRequest request) {
    try {
      FriendRequest existing = friendRequestService.getById(id);
      if (existing == null) {
        return Result.error("好友请求不存在");
      }
      
      if (!PermissionUtil.isCurrentUser(request, existing.getRecipientId()) && !PermissionUtil.isAdmin(request)) {
        return Result.error("权限不足，无权接受此好友请求");
      }

      boolean success = friendRequestService.acceptRequest(id);
      if (success) {
        return Result.success("已接受好友请求");
      }
      return Result.error("接受好友请求失败");
    } catch (RuntimeException e) {
      return Result.error(e.getMessage());
    }
  }

  @PostMapping("/{id}/reject")
  @RequirePermission(Role.USER)
  public Result<String> rejectRequest(@PathVariable Long id, HttpServletRequest request) {
    try {
      FriendRequest existing = friendRequestService.getById(id);
      if (existing == null) {
        return Result.error("好友请求不存在");
      }
      
      if (!PermissionUtil.isCurrentUser(request, existing.getRecipientId()) && !PermissionUtil.isAdmin(request)) {
        return Result.error("权限不足，无权拒绝此好友请求");
      }

      boolean success = friendRequestService.rejectRequest(id);
      if (success) {
        return Result.success("已拒绝好友请求");
      }
      return Result.error("拒绝好友请求失败");
    } catch (RuntimeException e) {
      return Result.error(e.getMessage());
    }
  }
}
