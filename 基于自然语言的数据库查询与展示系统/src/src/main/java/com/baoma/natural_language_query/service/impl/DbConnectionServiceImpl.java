package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.DbConnection;
import com.baoma.natural_language_query.entity.mysql.DbConnectionLog;
import com.baoma.natural_language_query.mapper.DbConnectionMapper;
import com.baoma.natural_language_query.service.DbConnectionLogService;
import com.baoma.natural_language_query.service.DbConnectionService;
import com.baoma.natural_language_query.utils.DynamicDatabaseExecutor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbConnectionServiceImpl extends ServiceImpl<DbConnectionMapper, DbConnection> implements DbConnectionService {

  @Autowired private DynamicDatabaseExecutor databaseExecutor;

  @Autowired private DbConnectionLogService dbConnectionLogService;

  @Override
  public List<DbConnection> listByCreateUserId(Long createUserId) {
    LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(DbConnection::getCreateUserId, createUserId);
    wrapper.orderByDesc(DbConnection::getCreateTime);
    return list(wrapper);
  }

  @Override
  public boolean testConnection(Long id) {
    DbConnection connection = getById(id);
    if (connection == null) {
      System.err.println("数据库连接不存在: ID=" + id);
      // 记录失败日志
      DbConnectionLog log = new DbConnectionLog();
      log.setDbConnectionId(id);
      log.setDbName("未知数据源");
      log.setConnectTime(LocalDateTime.now());
      log.setStatus("error");
      log.setRemark("连接不存在");
      dbConnectionLogService.save(log);
      return false;
    }

    try {
      // 使用动态数据库执行器测试连接
      boolean result = databaseExecutor.testConnection(connection);
      System.out.println("数据库连接测试结果: " + (result ? "成功" : "失败") + " - " + connection.getName());

      // 更新数据库连接状态
      connection.setStatus(result ? "connected" : "error");
      connection.setUpdateTime(LocalDateTime.now());
      updateById(connection);

      // 记录连接日志
      DbConnectionLog log = new DbConnectionLog();
      log.setDbConnectionId(connection.getId());
      log.setDbName(connection.getName());
      log.setConnectTime(LocalDateTime.now());
      log.setStatus(result ? "connected" : "error");
      log.setRemark(result ? null : "连接测试失败");
      dbConnectionLogService.save(log);

      return result;
    } catch (Exception e) {
      System.err.println("数据库连接测试异常: " + e.getMessage());
      e.printStackTrace();

      // 更新数据库连接状态为错误
      connection.setStatus("error");
      connection.setUpdateTime(LocalDateTime.now());
      updateById(connection);

      // 记录异常日志
      DbConnectionLog log = new DbConnectionLog();
      log.setDbConnectionId(connection.getId());
      log.setDbName(connection.getName());
      log.setConnectTime(LocalDateTime.now());
      log.setStatus("error");
      log.setRemark(e.getMessage());
      dbConnectionLogService.save(log);
      return false;
    }
  }

  @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {

      return lambdaUpdate()
        .set(DbConnection::getDeleted, 1)
        .eq(DbConnection::getId, id)
        .eq(DbConnection::getDeleted, 0)   // 防止重复更新
        .update();
    } 

  @Override
    public List<DbConnection> list() {
        // 只查 deleted = 0
        return lambdaQuery()
                .eq(DbConnection::getDeleted, 0)
                .list();
    }
}
