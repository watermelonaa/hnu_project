package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.FriendRelation;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.FriendRelationService;
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
@RequestMapping("/friend-relation")
public class FriendRelationController {

  @Autowired private FriendRelationService friendRelationService;

  @GetMapping("/list/{userId}")
  @RequirePermission(Role.USER)
  public Result<List<FriendRelation>> listByUserId(@PathVariable Long userId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能查看自己的好友列表");
    }
    return Result.success(friendRelationService.listByUserId(userId));
  }

  @GetMapping("/{userId}/{friendId}")
  @RequirePermission(Role.USER)
  public Result<FriendRelation> getByUserIdAndFriendId(
      @PathVariable Long userId, @PathVariable Long friendId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权查看他人的好友关系");
    }
    return Result.success(friendRelationService.getByUserIdAndFriendId(userId, friendId));
  }

  @PostMapping
  @RequirePermission(Role.ADMIN) // 创建关系通常由系统触发（如接受请求时），手动调用限管理员
  public Result<FriendRelation> save(@RequestBody FriendRelation friendRelation) {
    friendRelation.setCreateTime(LocalDateTime.now());
    if (friendRelation.getOnlineStatus() == null) {
      friendRelation.setOnlineStatus(0);
    }
    friendRelationService.save(friendRelation);
    return Result.success(friendRelation);
  }

  @PutMapping
  @RequirePermission(Role.USER)
  public Result<FriendRelation> update(@RequestBody FriendRelation friendRelation, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, friendRelation.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权修改他人的好友关系");
    }
    friendRelationService.updateById(friendRelation);
    return Result.success(friendRelation);
  }

  @DeleteMapping("/{id}")
  @RequirePermission(Role.USER)
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    FriendRelation existing = friendRelationService.getById(id);
    if (existing == null) {
      return Result.error("好友关系不存在");
    }
    if (!PermissionUtil.isCurrentUser(request, existing.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权删除他人的好友关系");
    }
    friendRelationService.removeById(id);
    return Result.success();
  }

  @DeleteMapping("/{userId}/{friendId}")
  @RequirePermission(Role.USER)
  public Result<Void> deleteByUserIdAndFriendId(
      @PathVariable Long userId, @PathVariable Long friendId, HttpServletRequest request) {
    if (!PermissionUtil.isCurrentUser(request, userId) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，无权删除他人的好友关系");
    }
    friendRelationService.removeByUserIdAndFriendId(userId, friendId);
    return Result.success();
  }
}
