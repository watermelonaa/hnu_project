package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.SystemHealth;
import com.baoma.natural_language_query.service.SystemHealthService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system-health")
public class SystemHealthController {

  @Autowired private SystemHealthService systemHealthService;

  /** 查询所有健康记录 */
  @GetMapping("/list")
  public Result<List<SystemHealth>> list() {
    return Result.success(systemHealthService.list());
  }

  /** 查询最新的健康记录 */
  @GetMapping("/latest")
  public Result<SystemHealth> getLatest() {
    return Result.success(systemHealthService.getLatest());
  }

  /** 查询最近N条健康记录 */
  @GetMapping("/recent/{limit}")
  public Result<List<SystemHealth>> listRecent(@PathVariable int limit) {
    return Result.success(systemHealthService.listRecent(limit));
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  public Result<SystemHealth> getById(@PathVariable Long id) {
    return Result.success(systemHealthService.getById(id));
  }

  /** 添加健康记录 */
  @PostMapping
  public Result<SystemHealth> save(@RequestBody SystemHealth systemHealth) {
    if (systemHealth.getCollectTime() == null) {
      systemHealth.setCollectTime(LocalDateTime.now());
    }
    systemHealthService.save(systemHealth);
    return Result.success(systemHealth);
  }

  /** 删除健康记录 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    systemHealthService.removeById(id);
    return Result.success();
  }
}
