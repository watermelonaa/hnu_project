package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.QueryLogDTO;
import com.baoma.natural_language_query.entity.mysql.QueryLog;
import com.baoma.natural_language_query.service.QueryLogService;
import java.time.LocalDate;
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
@RequestMapping("/query-log")
public class QueryLogController {

  @Autowired private QueryLogService queryLogService;

  /** 查询所有查询日志 */
  @GetMapping("/list")
  public Result<List<QueryLog>> list() {
    return Result.success(queryLogService.list());
  }

  /** 根据用户ID查询查询日志 */
  @GetMapping("/list/user/{userId}")
  public Result<List<QueryLog>> listByUser(@PathVariable Long userId) {
    return Result.success(queryLogService.listByUserId(userId));
  }

  /** 根据对话ID查询查询日志 */
  @GetMapping("/list/dialog/{dialogId}")
  public Result<List<QueryLog>> listByDialog(@PathVariable String dialogId) {
    return Result.success(queryLogService.listByDialogId(dialogId));
  }

  /** 根据ID查询查询日志 */
  @GetMapping("/{id}")
  public Result<QueryLog> getById(@PathVariable Long id) {
    return Result.success(queryLogService.getById(id));
  }

  /** 添加查询日志 */
  @PostMapping
  public Result<QueryLog> save(@RequestBody QueryLogDTO dto) {
    try {
      // 打印接收到的数据（用于调试）
      System.out.println("收到 QueryLogDTO: userId=" + dto.getUserId() 
          + ", dialogId=" + dto.getDialogId() 
          + ", dbConnectionId=" + dto.getDbConnectionId()
          + ", queryTimeStr=" + dto.getQueryTimeStr());
      
      // 验证必填字段
      if (dto.getUserId() == null) {
        throw new IllegalArgumentException("userId 不能为空");
      }
      if (dto.getDialogId() == null || dto.getDialogId().trim().isEmpty()) {
        throw new IllegalArgumentException("dialogId 不能为空");
      }
      if (dto.getDbConnectionId() == null || dto.getDbConnectionId() <= 0) {
        throw new IllegalArgumentException("dbConnectionId (dataSourceId) 不能为空且必须大于0，当前值: " + dto.getDbConnectionId());
      }
      
      // 将DTO转换为实体类
      QueryLog queryLog = new QueryLog();
      queryLog.setId(dto.getId());
      queryLog.setUserId(dto.getUserId());
      queryLog.setDialogId(dto.getDialogId());
      
      // 映射 dbConnectionId 到 dataSourceId
      queryLog.setDataSourceId(dto.getDbConnectionId());
      
      // 设置用户原始提问
      if (dto.getUserPrompt() != null && !dto.getUserPrompt().trim().isEmpty()) {
        queryLog.setUserPrompt(dto.getUserPrompt());
      }
      
      // 设置日期和时间
      if (dto.getQueryDate() == null) {
        queryLog.setQueryDate(LocalDate.now());
      } else {
        queryLog.setQueryDate(dto.getQueryDate());
      }
      
      LocalDateTime queryTime = dto.getQueryTime();
      if (queryTime == null) {
        queryLog.setQueryTime(LocalDateTime.now());
      } else {
        queryLog.setQueryTime(queryTime);
      }
      
      // 设置执行结果（如果前端没有提供，默认为成功）
      if (dto.getExecuteResult() != null) {
        queryLog.setExecuteResult(dto.getExecuteResult());
      } else {
        queryLog.setExecuteResult(1); // 默认成功
      }
      
      System.out.println("准备保存 QueryLog: dataSourceId=" + queryLog.getDataSourceId() 
          + ", userId=" + queryLog.getUserId() 
          + ", dialogId=" + queryLog.getDialogId());
      
      queryLogService.save(queryLog);
      System.out.println("保存成功，ID: " + queryLog.getId());
      return Result.success(queryLog);
    } catch (Exception e) {
      System.err.println("保存 QueryLog 失败: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  /** 删除查询日志 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    queryLogService.removeById(id);
    return Result.success();
  }
}
