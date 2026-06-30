package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mongodb.SqlCache;
import com.baoma.natural_language_query.service.SqlCacheService;
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
@RequestMapping("/sql-cache")
public class SqlCacheController {

  @Autowired private SqlCacheService sqlCacheService;

  @GetMapping("/list/{userId}")
  public Result<List<SqlCache>> listByUserId(@PathVariable Long userId) {
    return Result.success(sqlCacheService.listByUserId(userId));
  }

  @PostMapping("/query")
  public Result<SqlCache> getCache(@RequestBody SqlCache request) {
    SqlCache cache =
        sqlCacheService.getByNlHashAndConnectionIdAndTableIds(
            request.getNlHash(), request.getConnectionId(), request.getTableIds());
    return Result.success(cache);
  }

  @PostMapping
  public Result<SqlCache> save(@RequestBody SqlCache sqlCache) {
    SqlCache saved = sqlCacheService.save(sqlCache);
    return Result.success(saved);
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable String id) {
    sqlCacheService.deleteById(id);
    return Result.success();
  }
}
