package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mongodb.DialogRecord;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.service.DialogService;
import com.baoma.natural_language_query.utils.PermissionUtil;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/dialog")
public class DialogController {

  @Autowired private DialogService dialogService;

  @GetMapping("/list")
  @RequirePermission(Role.USER)
  public Result<List<DialogRecord>> getUserDialogs(HttpServletRequest request) {
    try {
      Long userId = (Long) request.getAttribute("userId");
      if (userId == null) {
        return Result.error("未登录或登录已过期");
      }
      List<DialogRecord> dialogs = dialogService.getUserDialogs(userId);
      return Result.success(dialogs != null ? dialogs : List.of());
    } catch (Exception e) {
      System.err.println("获取对话列表失败: " + e.getMessage());
      e.printStackTrace();
      return Result.error("获取对话列表失败: " + e.getMessage());
    }
  }

  @GetMapping("/{dialogId}")
  @RequirePermission(Role.USER)
  public Result<DialogRecord> getDialogById(@PathVariable String dialogId, HttpServletRequest request) {
    DialogRecord dialog = dialogService.getDialogById(dialogId);
    if (dialog != null) {
      // 权限检查：只能查看自己的对话
      if (!PermissionUtil.isOwner(request, dialog.getUserId()) && !PermissionUtil.isAdmin(request)) {
        return Result.error("权限不足，只能查看自己的对话");
      }
      return Result.success(dialog);
    }
    return Result.error("对话不存在");
  }

  @PostMapping
  @RequirePermission(Role.USER)
  public Result<DialogRecord> createDialog(
      @RequestBody DialogRecord dialogRecord,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      return Result.error("未登录或登录已过期");
    }
    dialogRecord.setUserId(userId);
    DialogRecord created = dialogService.createDialog(dialogRecord);
    return Result.success(created);
  }

  @DeleteMapping("/{dialogId}")
  @RequirePermission(Role.USER)
  public Result<Void> deleteDialog(
      @PathVariable String dialogId,
      HttpServletRequest request) {
    DialogRecord dialog = dialogService.getDialogById(dialogId);
    if (dialog == null) {
      return Result.error("对话不存在");
    }
    
    // 权限检查：只能删除自己的对话
    if (!PermissionUtil.isOwner(request, dialog.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能删除自己的对话");
    }

    dialogService.deleteDialog(dialogId, dialog.getUserId());
    return Result.success();
  }

  @PutMapping("/{dialogId}")
  @RequirePermission(Role.USER)
  public Result<DialogRecord> updateDialog(
      @PathVariable String dialogId,
      @RequestBody DialogRecord dialogRecord,
      HttpServletRequest request) {
    DialogRecord existing = dialogService.getDialogById(dialogId);
    if (existing == null) {
      return Result.error("对话不存在");
    }
    
    // 权限检查：只能更新自己的对话
    if (!PermissionUtil.isOwner(request, existing.getUserId()) && !PermissionUtil.isAdmin(request)) {
      return Result.error("权限不足，只能更新自己的对话");
    }

    dialogRecord.setDialogId(dialogId);
    dialogRecord.setUserId(existing.getUserId());
    DialogRecord updated = dialogService.updateDialog(dialogRecord);
    return Result.success(updated);
  }
}
