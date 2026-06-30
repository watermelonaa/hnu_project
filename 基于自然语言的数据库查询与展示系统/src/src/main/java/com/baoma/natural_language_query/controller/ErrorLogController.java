package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.ErrorLog;
import com.baoma.natural_language_query.service.ErrorLogService;
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
@RequestMapping("/error-log")
public class ErrorLogController {

  @Autowired private ErrorLogService errorLogService;

  /** 查询所有错误日志 */
  @GetMapping("/list")
  public Result<List<ErrorLog>> list() {
    return Result.success(errorLogService.list());
  }

  /** 根据错误类型查询 */
  @GetMapping("/list/type/{errorTypeId}")
  public Result<List<ErrorLog>> listByErrorType(@PathVariable Integer errorTypeId) {
    return Result.success(errorLogService.listByErrorTypeId(errorTypeId));
  }

  /** 根据统计周期查询 */
  @GetMapping("/list/period/{period}")
  public Result<List<ErrorLog>> listByPeriod(@PathVariable String period) {
    return Result.success(errorLogService.listByPeriod(period));
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  public Result<ErrorLog> getById(@PathVariable Long id) {
    return Result.success(errorLogService.getById(id));
  }

  /** 添加错误日志 */
  @PostMapping
  public Result<ErrorLog> save(@RequestBody ErrorLog errorLog) {
    if (errorLog.getStatTime() == null) {
      errorLog.setStatTime(LocalDateTime.now());
    }
    errorLogService.save(errorLog);
    return Result.success(errorLog);
  }

  /** 更新错误日志 */
  @PutMapping
  public Result<ErrorLog> update(@RequestBody ErrorLog errorLog) {
    errorLogService.updateById(errorLog);
    return Result.success(errorLog);
  }

  /** 删除错误日志 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    errorLogService.removeById(id);
    return Result.success();
  }
}
