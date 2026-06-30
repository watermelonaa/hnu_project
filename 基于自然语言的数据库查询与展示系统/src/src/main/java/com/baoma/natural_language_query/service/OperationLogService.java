package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.OperationLog;
import com.baomidou.mybatisplus.extension.service.IService;
import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogService extends IService<OperationLog> {
  /** 根据用户ID查询操作日志 */
  List<OperationLog> listByUserId(Long userId);

  /** 根据模块查询操作日志 */
  List<OperationLog> listByModule(String module);

  /** 查询失败的操作日志 */
  List<OperationLog> listFailed();

  /** 根据时间范围查询操作日志 */
  List<OperationLog> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

  /** 根据时间范围、用户、状态等条件查询操作日志 */
  List<OperationLog> listByConditions(
      LocalDateTime startTime,
      LocalDateTime endTime,
      Long userId,
      String module,
      Integer result);
}
