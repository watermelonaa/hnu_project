package com.baoma.natural_language_query.utils;

import com.baoma.natural_language_query.entity.mysql.DbConnection;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 动态数据库执行器
 *
 * <p>根据数据库连接配置（DbConnection）动态连接不同的数据库并执行SQL语句。 支持多种数据库类型（MySQL、PostgreSQL、Oracle、SQL Server等）。
 *
 * <p>主要功能：
 *
 * <ul>
 *   <li>测试数据库连接
 *   <li>执行SQL查询（SELECT）并返回结果
 *   <li>执行SQL更新（INSERT、UPDATE、DELETE）
 *   <li>获取数据库表结构信息（用于大模型感知元数据）
 *   <li>格式化表结构信息（用于传递给大模型）
 * </ul>
 *
 * <p>使用场景：
 *
 * <ul>
 *   <li>用户查询时，根据用户选择的数据库连接执行SQL
 *   <li>系统调用大模型前，获取数据库表结构信息
 *   <li>管理员测试数据库连接配置
 * </ul>
 *
 * @author 项目组
 * @version 1.0
 */
@Component
public class DynamicDatabaseExecutor {

  /** 表结构信息 */
  public static class TableSchema {
    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;

    public TableSchema() {
      this.columns = new ArrayList<>();
    }

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }

    public String getTableComment() {
      return tableComment;
    }

    public void setTableComment(String tableComment) {
      this.tableComment = tableComment;
    }

    public List<ColumnInfo> getColumns() {
      return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
      this.columns = columns;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("表名: ").append(tableName);
      if (tableComment != null && !tableComment.isEmpty()) {
        sb.append(" (").append(tableComment).append(")");
      }
      sb.append("\n列信息:\n");
      for (ColumnInfo col : columns) {
        sb.append("  - ").append(col.columnName).append(" (").append(col.dataType).append(")");
        if (col.columnComment != null && !col.columnComment.isEmpty()) {
          sb.append(" - ").append(col.columnComment);
        }
        if (col.isPrimaryKey) {
          sb.append(" [主键]");
        }
        sb.append("\n");
      }
      return sb.toString();
    }
  }

  /** 列信息 */
  public static class ColumnInfo {
    private String columnName;
    private String dataType;
    private String columnComment;
    private boolean isPrimaryKey;
    private boolean isNullable;

    public String getColumnName() {
      return columnName;
    }

    public void setColumnName(String columnName) {
      this.columnName = columnName;
    }

    public String getDataType() {
      return dataType;
    }

    public void setDataType(String dataType) {
      this.dataType = dataType;
    }

    public String getColumnComment() {
      return columnComment;
    }

    public void setColumnComment(String columnComment) {
      this.columnComment = columnComment;
    }

    public boolean isPrimaryKey() {
      return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
      isPrimaryKey = primaryKey;
    }

    public boolean isNullable() {
      return isNullable;
    }

    public void setNullable(boolean nullable) {
      isNullable = nullable;
    }
  }

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

  /** 构建完整的 JDBC URL */
  private String buildJdbcUrl(DbConnection connection) {
    String url = connection.getUrl();

    // 如果 URL 已经包含 jdbc: 前缀，说明是完整的 JDBC URL，直接返回
    if (url != null && url.startsWith("jdbc:")) {
      System.out.println("🔗 使用完整JDBC URL: " + url);
      return url;
    }

    // URL 不能为空
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("数据库连接URL不能为空");
    }

    // 否则，需要添加前缀（兼容旧格式：host:port/database）
    String prefix = getJdbcUrlPrefix(connection.getDbTypeId());
    String finalUrl;

    // 数据库已正确存储 UTF-8，直接使用 UTF-8 连接
    if (connection.getDbTypeId() != null && connection.getDbTypeId() == 1) {
      String mysqlParams =
          "useUnicode=true&characterEncoding=UTF-8"
              + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
      if (url.contains("?")) {
        // 检查参数是否已存在，避免重复
        if (!url.contains("useUnicode=true")) {
          finalUrl = prefix + url + "&" + mysqlParams;
        } else {
          finalUrl = prefix + url;
        }
      } else {
        finalUrl = prefix + url + "?" + mysqlParams;
      }
    } else {
      finalUrl = prefix + url;
    }

    System.out.println("🔗 构建JDBC URL: " + finalUrl);
    return finalUrl;
  }

  /**
   * 测试数据库连接
   *
   * @param connection 数据库连接配置
   * @return 是否连接成功
   */
  public boolean testConnection(DbConnection connection) {
    try {
      // 加载驱动
      Class.forName(getDriverClassName(connection.getDbTypeId()));

      // 构建 JDBC URL
      String jdbcUrl = buildJdbcUrl(connection);

      System.out.println("测试连接: " + jdbcUrl);
      System.out.println("用户名: " + connection.getUsername());

      // 尝试连接
      try (Connection conn =
          DriverManager.getConnection(
              jdbcUrl, connection.getUsername(), connection.getPassword())) {
        return conn != null && !conn.isClosed();
      }
    } catch (Exception e) {
      System.err.println("数据库连接测试失败: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  /**
   * 执行 SQL 查询
   *
   * @param connection 数据库连接配置
   * @param sql SQL 查询语句
   * @return 包含 headers 和 rows 的 Map
   */
  public Map<String, Object> executeQuery(DbConnection connection, String sql) {
    Map<String, Object> result = new HashMap<>();
    List<String> headers = new ArrayList<>();
    List<List<Object>> rows = new ArrayList<>();

    try {
      // 加载驱动
      Class.forName(getDriverClassName(connection.getDbTypeId()));

      // 构建 JDBC URL
      String jdbcUrl = buildJdbcUrl(connection);

      System.out.println("========== 动态数据库查询开始 ==========");
      System.out.println("执行查询 URL: " + jdbcUrl);
      System.out.println("执行 SQL: " + sql);
      System.out.println("==========================================");

      // 连接数据库并执行查询
      try (Connection conn =
          DriverManager.getConnection(
              jdbcUrl, connection.getUsername(), connection.getPassword())) {

        // 不需要设置任何字符集，因为我们使用 ISO-8859-1 编码读取原始字节
        // ISO-8859-1 是 1:1 映射，不会丢失任何数据

        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

          // 获取列信息
          ResultSetMetaData metaData = rs.getMetaData();
          int columnCount = metaData.getColumnCount();

          // 提取列名
          for (int i = 1; i <= columnCount; i++) {
            headers.add(metaData.getColumnLabel(i));
          }

          // 提取数据行
          int rowCount = 0;
          while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
              // 根据列类型选择合适的读取方法
              int columnType = metaData.getColumnType(i);
              Object value;
              
              // 数据库已正确存储 UTF-8，JDBC 也使用 UTF-8，直接读取即可
              if (columnType == Types.VARCHAR
                  || columnType == Types.CHAR
                  || columnType == Types.LONGVARCHAR
                  || columnType == Types.NVARCHAR
                  || columnType == Types.NCHAR) {
                value = rs.getString(i);
              } else {
                value = rs.getObject(i);
              }
              
              row.add(value != null ? value.toString() : "");
            }
            rows.add(row);
            rowCount++;
            
            // 打印前3行数据用于调试
            if (rowCount <= 3) {
              System.out.println("📊 第" + rowCount + "行数据: " + row);
            }
          }
          System.out.println("✅ 共读取 " + rowCount + " 行数据");
        }
      }

      result.put("headers", headers);
      result.put("rows", rows);

      System.out.println("查询成功，返回 " + rows.size() + " 行数据");

    } catch (Exception e) {
      System.err.println("SQL执行失败: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("SQL执行失败: " + e.getMessage(), e);
    }

    return result;
  }

  /**
   * 执行 SQL 更新（INSERT, UPDATE, DELETE）
   *
   * @param connection 数据库连接配置
   * @param sql SQL 更新语句
   * @return 影响的行数
   */
  public int executeUpdate(DbConnection connection, String sql) {
    try {
      // 加载驱动
      Class.forName(getDriverClassName(connection.getDbTypeId()));

      // 构建 JDBC URL
      String jdbcUrl = buildJdbcUrl(connection);

      System.out.println("执行更新 URL: " + jdbcUrl);
      System.out.println("执行 SQL: " + sql);

      // 连接数据库并执行更新
      try (Connection conn =
          DriverManager.getConnection(
              jdbcUrl, connection.getUsername(), connection.getPassword())) {
        try (Statement stmt = conn.createStatement()) {
          int affectedRows = stmt.executeUpdate(sql);
          System.out.println("更新成功，影响 " + affectedRows + " 行");
          return affectedRows;
        }
      }

    } catch (Exception e) {
      System.err.println("SQL更新失败: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("SQL更新失败: " + e.getMessage(), e);
    }
  }

  /**
   * 获取数据库中所有表的结构信息
   *
   * @param connection 数据库连接配置
   * @return 表结构列表
   */
  public List<TableSchema> getDatabaseSchema(DbConnection connection) {
    List<TableSchema> schemas = new ArrayList<>();

    try {
      // 加载驱动
      Class.forName(getDriverClassName(connection.getDbTypeId()));

      // 构建 JDBC URL
      String jdbcUrl = buildJdbcUrl(connection);

      System.out.println("获取数据库结构: " + jdbcUrl);

      // 连接数据库
      try (Connection conn =
          DriverManager.getConnection(
              jdbcUrl, connection.getUsername(), connection.getPassword())) {
        DatabaseMetaData metaData = conn.getMetaData();

        // 从URL中提取数据库名
        String databaseName = extractDatabaseName(connection.getUrl());

        // 获取所有表
        try (ResultSet tables =
            metaData.getTables(databaseName, null, "%", new String[] {"TABLE"})) {
          while (tables.next()) {
            TableSchema tableSchema = new TableSchema();
            String tableName = tables.getString("TABLE_NAME");
            String tableComment = tables.getString("REMARKS");

            tableSchema.setTableName(tableName);
            tableSchema.setTableComment(tableComment);

            // 获取该表的所有列
            List<ColumnInfo> columns = new ArrayList<>();
            try (ResultSet columns_rs = metaData.getColumns(databaseName, null, tableName, "%")) {
              while (columns_rs.next()) {
                ColumnInfo columnInfo = new ColumnInfo();
                columnInfo.setColumnName(columns_rs.getString("COLUMN_NAME"));
                columnInfo.setDataType(columns_rs.getString("TYPE_NAME"));
                columnInfo.setColumnComment(columns_rs.getString("REMARKS"));
                columnInfo.setNullable("YES".equalsIgnoreCase(columns_rs.getString("IS_NULLABLE")));
                columns.add(columnInfo);
              }
            }

            // 获取主键信息
            try (ResultSet primaryKeys = metaData.getPrimaryKeys(databaseName, null, tableName)) {
              while (primaryKeys.next()) {
                String pkColumnName = primaryKeys.getString("COLUMN_NAME");
                for (ColumnInfo col : columns) {
                  if (col.getColumnName().equals(pkColumnName)) {
                    col.setPrimaryKey(true);
                    break;
                  }
                }
              }
            }

            tableSchema.setColumns(columns);
            schemas.add(tableSchema);
          }
        }

        System.out.println("成功获取 " + schemas.size() + " 个表的结构信息");
      }

    } catch (Exception e) {
      System.err.println("获取数据库结构失败: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("获取数据库结构失败: " + e.getMessage(), e);
    }

    return schemas;
  }

  /**
   * 从URL中提取数据库名
   *
   * @param url 格式如 "localhost:3306/database_name"
   * @return 数据库名
   */
  private String extractDatabaseName(String url) {
    if (url.contains("/")) {
      String[] parts = url.split("/");
      if (parts.length > 1) {
        // 移除可能的查询参数
        String dbName = parts[parts.length - 1];
        if (dbName.contains("?")) {
          dbName = dbName.substring(0, dbName.indexOf("?"));
        }
        return dbName;
      }
    }
    return null;
  }

  /**
   * 将表结构格式化为易读的字符串（用于传递给大模型）
   *
   * @param schemas 表结构列表
   * @return 格式化的字符串
   */
  public String formatSchemaForLLM(List<TableSchema> schemas) {
    StringBuilder sb = new StringBuilder();
    sb.append("数据库表结构信息：\n\n");

    for (TableSchema schema : schemas) {
      sb.append("表名: ").append(schema.getTableName());
      if (schema.getTableComment() != null && !schema.getTableComment().isEmpty()) {
        sb.append(" (").append(schema.getTableComment()).append(")");
      }
      sb.append("\n");

      for (ColumnInfo col : schema.getColumns()) {
        sb.append("  - ").append(col.getColumnName()).append(": ").append(col.getDataType());
        if (col.getColumnComment() != null && !col.getColumnComment().isEmpty()) {
          sb.append(" // ").append(col.getColumnComment());
        }
        if (col.isPrimaryKey()) {
          sb.append(" [主键]");
        }
        sb.append("\n");
      }
      sb.append("\n");
    }

    return sb.toString();
  }
}
