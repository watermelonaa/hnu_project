package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.QueryShare;
import com.baoma.natural_language_query.service.QueryShareService;
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
@RequestMapping("/query-share")
public class QueryShareController {

  @Autowired private QueryShareService queryShareService;

  @GetMapping("/list/receive/{receiveUserId}")
  public Result<List<QueryShare>> listByReceiveUserId(@PathVariable Long receiveUserId) {
    return Result.success(queryShareService.listByReceiveUserId(receiveUserId));
  }

  @GetMapping("/list/receive/{receiveUserId}/{receiveStatus}")
  public Result<List<QueryShare>> listByReceiveUserIdAndStatus(
      @PathVariable Long receiveUserId, @PathVariable Integer receiveStatus) {
    return Result.success(
        queryShareService.listByReceiveUserIdAndStatus(receiveUserId, receiveStatus));
  }

  @GetMapping("/list/share/{shareUserId}")
  public Result<List<QueryShare>> listByShareUserId(@PathVariable Long shareUserId) {
    return Result.success(queryShareService.listByShareUserId(shareUserId));
  }

  @GetMapping("/{id}")
  public Result<QueryShare> getById(@PathVariable Long id) {
    return Result.success(queryShareService.getById(id));
  }

  @PostMapping
  public Result<QueryShare> save(@RequestBody QueryShare queryShare) {
    queryShare.setShareTime(LocalDateTime.now());
    if (queryShare.getReceiveStatus() == null) {
      queryShare.setReceiveStatus(0);
    }
    queryShareService.save(queryShare);
    return Result.success(queryShare);
  }

  @PutMapping
  public Result<QueryShare> update(@RequestBody QueryShare queryShare) {
    queryShareService.updateById(queryShare);
    return Result.success(queryShare);
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    queryShareService.removeById(id);
    return Result.success();
  }
}
