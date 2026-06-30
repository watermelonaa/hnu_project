package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.DbConnection;
import com.baoma.natural_language_query.service.DatabaseSchemaService;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** 数据库表结构服务实现 */
@Service
public class DatabaseSchemaServiceImpl implements DatabaseSchemaService {

  /** 根据数据库类型ID获取 JDBC URL 前缀 */
  private String getJdbcUrlPrefix(Integer dbTypeId) {
    if (dbTypeId == null) {
      throw new IllegalArgumentException("数据库类型ID不能为空");
    }

    switch (dbTypeId) {
      case 1: // MySQL
        return "jdbc:mysql://";
      case 2: // PostgreSQL
        return "jdbc:postgresql://";
      case 3: // Oracle
        return "jdbc:oracle:thin:@";
      case 4: // SQL Server
        return "jdbc:sqlserver://";
      default:
        throw new IllegalArgumentException("不支持的数据库类型ID: " + dbTypeId);
    }
  }

  /** 根据数据库类型ID获取 JDBC 驱动类名 */
  private String getDriverClassName(Integer dbTypeId) {
    if (dbTypeId == null) {
      throw new IllegalArgumentException("数据库类型ID不能为空");
    }

    switch (dbTypeId) {
      case 1: // MySQL
        return "com.mysql.cj.jdbc.Driver";
      case 2: // PostgreSQL
        return "org.postgresql.Driver";
      case 3: // Oracle
        return "oracle.jdbc.driver.OracleDriver";
      case 4: // SQL Server
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
      default:
        throw new IllegalArgumentException("不支持的数据库类型ID: " + dbTypeId);
    }
  }

  /** 构建完整的 JDBC URL （使用 UTF-8 支持中文）*/
  private String buildJdbcUrl(DbConnection connection) {
    String prefix = getJdbcUrlPrefix(connection.getDbTypeId());
    String url = connection.getUrl();

    // 对于 MySQL，添加额外参数
    if (connection.getDbTypeId() != null && connection.getDbTypeId() == 1) {
      String mysqlParams =
          "useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci"
              + "&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
      if (url.contains("?")) {
        return prefix + url + "&" + mysqlParams;
      } else {
        return prefix + url + "?" + mysqlParams;
      }
    }

    return prefix + url;
  }

  @Override
  public String getDatabaseSchema(DbConnection connection) {
    try {
      List<String> tableNames = getTableNames(connection);
      StringBuilder schema = new StringBuilder();

      schema.append("数据库表结构信息：\n\n");

      for (String tableName : tableNames) {
        Map<String, Object> tableSchema = getTableSchema(connection, tableName);
        schema.append(formatTableSchema(tableName, tableSchema));
        schema.append("\n");
      }

      return schema.toString();
    } catch (Exception e) {
      System.err.println("获取数据库表结构失败: " + e.getMessage());
      e.printStackTrace();
      return "无法获取表结构信息";
    }
  }

  @Override
  public List<String> getTableNames(DbConnection connection) {
    List<String> tableNames = new ArrayList<>();

    try {
      Class.forName(getDriverClassName(connection.getDbTypeId()));
      String jdbcUrl = buildJdbcUrl(connection);

      try (Connection conn =
          DriverManager.getConnection(
              jdbcUrl, connection.getUsername(), connection.getPassword())) {
        DatabaseMetaData metaData = conn.getMetaData();

        // 从URL中提取数据库名
        String databaseName = extractDatabaseName(connection.getUrl());
        
        if (databaseName == null || databaseName.isEmpty()) {
          throw new IllegalArgumentException(
              "无法从连接URL中提取数据库名。URL格式应为: host:port/database");
        }

        // 获取所有表
        try (ResultSet tables =
            metaData.getTables(databaseName, null, "%", new String[] {"TABLE"})) {
          while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            tableNames.add(tableName);
          }
        }
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "数据库驱动加载失败: " + getDriverClassName(connection.getDbTypeId()) + " - " + e.getMessage(), e);
    } catch (SQLException e) {
      throw new RuntimeException(
          "数据库连接失败或查询表失败: " + e.getMessage() + " (错误代码: " + e.getErrorCode() + ")", e);
    } catch (Exception e) {
      throw new RuntimeException("获取表名列表失败: " + e.getMessage(), e);
    }

    return tableNames;
  }

  @Override
  public Map<String, Object> getTableSchema(DbConnection connection, String tableName) {
    Map<String, Object> tableInfo = new LinkedHashMap<>();
    List<Map<String, String>> columns = new ArrayList<>();

    try {
      Class.forName(getDriverClassName(connection.getDbTypeId()));
      String jdbcUrl = buildJdbcUrl(connection);

      try (Connection conn =
          DriverManager.getConnection(
              jdbcUrl, connection.getUsername(), connection.getPassword())) {
        DatabaseMetaData metaData = conn.getMetaData();
        String databaseName = extractDatabaseName(connection.getUrl());

        // 获取表注释（MySQL特有）
        if (connection.getDbTypeId() == 1) {
          try (Statement stmt = conn.createStatement();
              ResultSet rs =
                  stmt.executeQuery(
                      "SELECT TABLE_COMMENT FROM information_schema.TABLES "
                          + "WHERE TABLE_SCHEMA = '"
                          + databaseName
                          + "' AND TABLE_NAME = '"
                          + tableName
                          + "'")) {
            if (rs.next()) {
              byte[] commentBytes = rs.getBytes("TABLE_COMMENT");
              tableInfo.put("comment", commentBytes != null ? new String(commentBytes, StandardCharsets.UTF_8) : "");
            }
          } catch (Exception e) {
            // 忽略获取注释失败的错误
          }
        }

        // 获取列信息
        try (ResultSet columnsRs = metaData.getColumns(databaseName, null, tableName, "%")) {
          while (columnsRs.next()) {
            Map<String, String> columnInfo = new LinkedHashMap<>();
            columnInfo.put("name", columnsRs.getString("COLUMN_NAME"));
            columnInfo.put("type", columnsRs.getString("TYPE_NAME"));
            columnInfo.put("size", columnsRs.getString("COLUMN_SIZE"));
            columnInfo.put(
                "nullable",
                columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? "YES" : "NO");

            String remarks = columnsRs.getString("REMARKS");
            if (remarks != null && !remarks.isEmpty()) {
              columnInfo.put("comment", remarks);
            }

            columns.add(columnInfo);
          }
        }

        // 获取主键信息
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet pkRs = metaData.getPrimaryKeys(databaseName, null, tableName)) {
          while (pkRs.next()) {
            primaryKeys.add(pkRs.getString("COLUMN_NAME"));
          }
        }

        tableInfo.put("tableName", tableName);
        tableInfo.put("columns", columns);
        tableInfo.put("primaryKeys", primaryKeys);
      }
    } catch (Exception e) {
      System.err.println("获取表结构失败: " + tableName + " - " + e.getMessage());
      e.printStackTrace();
    }

    return tableInfo;
  }

  /** 从URL中提取数据库名 */
  private String extractDatabaseName(String url) {
    // 格式：host:port/database 或 host:port/database?params
    if (url.contains("/")) {
      String[] parts = url.split("/");
      if (parts.length >= 2) {
        String dbPart = parts[parts.length - 1];
        // 去除参数
        if (dbPart.contains("?")) {
          dbPart = dbPart.split("\\?")[0];
        }
        return dbPart;
      }
    }
    return null;
  }

  /** 格式化表结构信息为易读的字符串 */
  private String formatTableSchema(String tableName, Map<String, Object> tableSchema) {
    StringBuilder sb = new StringBuilder();

    sb.append("表名: ").append(tableName);

    if (tableSchema.containsKey("comment")) {
      sb.append(" (").append(tableSchema.get("comment")).append(")");
    }
    sb.append("\n");

    @SuppressWarnings("unchecked")
    List<Map<String, String>> columns = (List<Map<String, String>>) tableSchema.get("columns");

    if (columns != null && !columns.isEmpty()) {
      sb.append("字段:\n");
      for (Map<String, String> column : columns) {
        sb.append("  - ")
            .append(column.get("name"))
            .append(" (")
            .append(column.get("type"))
            .append(", 可空: ")
            .append(column.get("nullable"))
            .append(")");

        if (column.containsKey("comment")) {
          sb.append(" // ").append(column.get("comment"));
        }
        sb.append("\n");
      }
    }

    @SuppressWarnings("unchecked")
    List<String> primaryKeys = (List<String>) tableSchema.get("primaryKeys");
    if (primaryKeys != null && !primaryKeys.isEmpty()) {
      sb.append("主键: ").append(String.join(", ", primaryKeys)).append("\n");
    }

    return sb.toString();
  }
}
