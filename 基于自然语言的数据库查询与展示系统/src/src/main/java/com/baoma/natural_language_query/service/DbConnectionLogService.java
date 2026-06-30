package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.DbConnectionLog;
import java.util.List;

public interface DbConnectionLogService {

  List<DbConnectionLog> list();

  List<DbConnectionLog> listByDbConnectionId(Long dbConnectionId);

  List<DbConnectionLog> listByStatus(String status);

  DbConnectionLog getById(Long id);

  boolean save(DbConnectionLog dbConnectionLog);
}
