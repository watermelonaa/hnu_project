package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.DbConnectionLog;
import com.baoma.natural_language_query.mapper.DbConnectionLogMapper;
import com.baoma.natural_language_query.service.DbConnectionLogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DbConnectionLogServiceImpl implements DbConnectionLogService {

  @Autowired private DbConnectionLogMapper dbConnectionLogMapper;

  @Override
  public List<DbConnectionLog> list() {
    return dbConnectionLogMapper.selectList(null);
  }

  @Override
  public List<DbConnectionLog> listByDbConnectionId(Long dbConnectionId) {
    QueryWrapper<DbConnectionLog> wrapper = new QueryWrapper<>();
    wrapper.eq("db_connection_id", dbConnectionId);
    return dbConnectionLogMapper.selectList(wrapper);
  }

  @Override
  public List<DbConnectionLog> listByStatus(String status) {
    QueryWrapper<DbConnectionLog> wrapper = new QueryWrapper<>();
    wrapper.eq("status", status);
    return dbConnectionLogMapper.selectList(wrapper);
  }

  @Override
  public DbConnectionLog getById(Long id) {
    return dbConnectionLogMapper.selectById(id);
  }

  @Override
  public boolean save(DbConnectionLog dbConnectionLog) {
    return dbConnectionLogMapper.insert(dbConnectionLog) > 0;
  }
}
