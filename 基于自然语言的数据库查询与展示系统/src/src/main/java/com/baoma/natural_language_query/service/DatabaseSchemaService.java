package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.DbConnection;
import java.util.List;
import java.util.Map;

/** 数据库表结构服务接口 */
public interface DatabaseSchemaService {
  /**
   * 获取数据库的所有表结构信息
   *
   * @param connection 数据库连接配置
   * @return 表结构信息的格式化字符串
   */
  String getDatabaseSchema(DbConnection connection);

  /**
   * 获取指定表的结构信息
   *
   * @param connection 数据库连接配置
   * @param tableName 表名
   * @return 表结构信息
   */
  Map<String, Object> getTableSchema(DbConnection connection, String tableName);

  /**
   * 获取数据库的所有表名
   *
   * @param connection 数据库连接配置
   * @return 表名列表
   */
  List<String> getTableNames(DbConnection connection);
}
