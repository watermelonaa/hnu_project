package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.NotificationTarget;
import com.baoma.natural_language_query.service.NotificationTargetService;
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
@RequestMapping("/notification-target")
public class NotificationTargetController {

  @Autowired private NotificationTargetService notificationTargetService;

  /** 查询所有通知目标 */
  @GetMapping("/list")
  public Result<List<NotificationTarget>> list() {
    return Result.success(notificationTargetService.list());
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  public Result<NotificationTarget> getById(@PathVariable Integer id) {
    return Result.success(notificationTargetService.getById(id));
  }

  /** 根据目标编码查询 */
  @GetMapping("/code/{targetCode}")
  public Result<NotificationTarget> getByTargetCode(@PathVariable String targetCode) {
    return Result.success(notificationTargetService.getByTargetCode(targetCode));
  }

  /** 添加通知目标 */
  @PostMapping
  public Result<NotificationTarget> save(@RequestBody NotificationTarget notificationTarget) {
    notificationTargetService.save(notificationTarget);
    return Result.success(notificationTarget);
  }

  /** 更新通知目标 */
  @PutMapping
  public Result<NotificationTarget> update(@RequestBody NotificationTarget notificationTarget) {
    notificationTargetService.updateById(notificationTarget);
    return Result.success(notificationTarget);
  }

  /** 删除通知目标 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Integer id) {
    notificationTargetService.removeById(id);
    return Result.success();
  }
}
