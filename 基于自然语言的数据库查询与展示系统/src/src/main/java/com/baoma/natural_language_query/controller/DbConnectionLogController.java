package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.DbConnectionLog;
import com.baoma.natural_language_query.service.DbConnectionLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/db-connection-log")
public class DbConnectionLogController {

  @Autowired private DbConnectionLogService dbConnectionLogService;

  @GetMapping("/list")
  public Result<List<DbConnectionLog>> list() {
    return Result.success(dbConnectionLogService.list());
  }

  @GetMapping("/list/connection/{dbConnectionId}")
  public Result<List<DbConnectionLog>> listByDbConnectionId(@PathVariable Long dbConnectionId) {
    return Result.success(dbConnectionLogService.listByDbConnectionId(dbConnectionId));
  }

  @GetMapping("/list/status/{status}")
  public Result<List<DbConnectionLog>> listByStatus(@PathVariable String status) {
    return Result.success(dbConnectionLogService.listByStatus(status));
  }

  @GetMapping("/{id}")
  public Result<DbConnectionLog> getById(@PathVariable Long id) {
    return Result.success(dbConnectionLogService.getById(id));
  }

  @PostMapping
  public Result<DbConnectionLog> save(@RequestBody DbConnectionLog dbConnectionLog) {
    if (dbConnectionLog.getConnectTime() == null) {
      dbConnectionLog.setConnectTime(LocalDateTime.now());
    }
    dbConnectionLogService.save(dbConnectionLog);
    return Result.success(dbConnectionLog);
  }
}
