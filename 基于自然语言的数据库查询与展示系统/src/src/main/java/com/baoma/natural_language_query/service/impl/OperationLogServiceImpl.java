package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.OperationLog;
import com.baoma.natural_language_query.mapper.OperationLogMapper;
import com.baoma.natural_language_query.service.OperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
    implements OperationLogService {

  @Override
  public List<OperationLog> listByUserId(Long userId) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(OperationLog::getUserId, userId);
    wrapper.orderByDesc(OperationLog::getOperateTime);
    return list(wrapper);
  }

  @Override
  public List<OperationLog> listByModule(String module) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(OperationLog::getModule, module);
    wrapper.orderByDesc(OperationLog::getOperateTime);
    return list(wrapper);
  }

  @Override
  public List<OperationLog> listFailed() {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(OperationLog::getResult, 0);
    wrapper.orderByDesc(OperationLog::getOperateTime);
    return list(wrapper);
  }

  @Override
  public List<OperationLog> listByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    if (startTime != null) {
      wrapper.ge(OperationLog::getOperateTime, startTime);
    }
    if (endTime != null) {
      wrapper.le(OperationLog::getOperateTime, endTime);
    }
    wrapper.orderByDesc(OperationLog::getOperateTime);
    return list(wrapper);
  }

  @Override
  public List<OperationLog> listByConditions(
      LocalDateTime startTime,
      LocalDateTime endTime,
      Long userId,
      String module,
      Integer result) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    if (startTime != null) {
      wrapper.ge(OperationLog::getOperateTime, startTime);
    }
    if (endTime != null) {
      wrapper.le(OperationLog::getOperateTime, endTime);
    }
    if (userId != null) {
      wrapper.eq(OperationLog::getUserId, userId);
    }
    if (module != null && !module.isEmpty()) {
      wrapper.eq(OperationLog::getModule, module);
    }
    if (result != null) {
      wrapper.eq(OperationLog::getResult, result);
    }
    wrapper.orderByDesc(OperationLog::getOperateTime);
    return list(wrapper);
  }

  @Override
  public List<OperationLog> list() {
    // 重写list方法，确保默认按时间倒序排序
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.orderByDesc(OperationLog::getOperateTime);
    return list(wrapper);
  }
}
