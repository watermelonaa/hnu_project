package com.baoma.natural_language_query.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL表名提取工具类
 *
 * <p>用于从SQL语句中提取所有被访问的表名，支持多种SQL语句类型：
 *
 * <ul>
 *   <li>SELECT语句（FROM子句、JOIN子句）
 *   <li>UPDATE语句
 *   <li>INSERT语句
 *   <li>DELETE语句
 *   <li>子查询中的表名（递归提取）
 * </ul>
 *
 * <p>使用场景：
 *
 * <ul>
 *   <li>权限验证：检查用户是否有权限访问SQL中涉及的表
 *   <li>SQL分析：分析SQL语句访问了哪些表
 *   <li>安全审计：记录用户访问的表信息
 * </ul>
 *
 * <p>功能说明：
 *
 * <ul>
 *   <li>提取表名（支持数据库前缀、反引号等格式）
 *   <li>清理表名（移除反引号、数据库前缀等）
 *   <li>判断SQL是否为只读操作
 *   <li>获取SQL操作类型（SELECT、INSERT、UPDATE等）
 * </ul>
 *
 * @author 项目组
 * @version 1.0
 */
public class SqlTableExtractor {

  // 匹配FROM子句中的表名
  private static final Pattern FROM_PATTERN =
      Pattern.compile(
          "\\bFROM\\s+([\\w\\.`]+)(?:\\s+(?:AS\\s+)?([\\w`]+))?", Pattern.CASE_INSENSITIVE);

  // 匹配JOIN子句中的表名
  private static final Pattern JOIN_PATTERN =
      Pattern.compile(
          "\\b(?:INNER\\s+|LEFT\\s+|RIGHT\\s+|FULL\\s+|CROSS\\s+)?JOIN\\s+([\\w\\.`]+)(?:\\s+(?:AS\\s+)?([\\w`]+))?",
          Pattern.CASE_INSENSITIVE);

  // 匹配UPDATE语句中的表名
  private static final Pattern UPDATE_PATTERN =
      Pattern.compile(
          "\\bUPDATE\\s+([\\w\\.`]+)(?:\\s+(?:AS\\s+)?([\\w`]+))?", Pattern.CASE_INSENSITIVE);

  // 匹配INSERT INTO语句中的表名
  private static final Pattern INSERT_PATTERN =
      Pattern.compile("\\bINSERT\\s+INTO\\s+([\\w\\.`]+)", Pattern.CASE_INSENSITIVE);

  // 匹配DELETE FROM语句中的表名
  private static final Pattern DELETE_PATTERN =
      Pattern.compile("\\bDELETE\\s+FROM\\s+([\\w\\.`]+)", Pattern.CASE_INSENSITIVE);

  // 匹配子查询中的表名（简单处理）
  private static final Pattern SUBQUERY_PATTERN =
      Pattern.compile("\\(\\s*SELECT[^)]+\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  /**
   * 从SQL语句中提取所有表名
   *
   * @param sql SQL语句
   * @return 表名集合（去重）
   */
  public static Set<String> extractTableNames(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return new HashSet<>();
    }

    Set<String> tableNames = new HashSet<>();

    // 预处理SQL：移除注释和多余空格
    String cleanSql = preprocessSql(sql);

    // 处理子查询（递归提取）
    List<String> subQueries = extractSubQueries(cleanSql);
    for (String subQuery : subQueries) {
      tableNames.addAll(extractTableNames(subQuery));
    }

    // 移除子查询后再处理主查询
    String mainSql = removeSubQueries(cleanSql);

    // 提取各种语句中的表名
    tableNames.addAll(extractFromClause(mainSql));
    tableNames.addAll(extractJoinClause(mainSql));
    tableNames.addAll(extractUpdateClause(mainSql));
    tableNames.addAll(extractInsertClause(mainSql));
    tableNames.addAll(extractDeleteClause(mainSql));

    // 清理表名（移除反引号、数据库前缀等）
    return cleanTableNames(tableNames);
  }

  /** 预处理SQL语句 */
  private static String preprocessSql(String sql) {
    // 移除单行注释
    sql = sql.replaceAll("--[^\r\n]*", "");
    // 移除多行注释
    sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", "");
    // 标准化空格
    sql = sql.replaceAll("\\s+", " ").trim();
    return sql;
  }

  /** 提取子查询 */
  private static List<String> extractSubQueries(String sql) {
    List<String> subQueries = new ArrayList<>();
    Matcher matcher = SUBQUERY_PATTERN.matcher(sql);
    while (matcher.find()) {
      String subQuery = matcher.group();
      // 移除外层括号
      subQuery = subQuery.substring(1, subQuery.length() - 1).trim();
      subQueries.add(subQuery);
    }
    return subQueries;
  }

  /** 移除子查询 */
  private static String removeSubQueries(String sql) {
    return SUBQUERY_PATTERN.matcher(sql).replaceAll("(SUBQUERY)");
  }

  /** 提取FROM子句中的表名（支持逗号分隔的多个表） */
  private static Set<String> extractFromClause(String sql) {
    Set<String> tables = new HashSet<>();

    // 匹配FROM关键字及其后的表列表
    Pattern fromPattern =
        Pattern.compile(
            "(?i)\\bFROM\\s+(.+?)(?:\\s+(?:JOIN|WHERE|GROUP|ORDER|HAVING|LIMIT|UNION)\\b|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    Matcher matcher = fromPattern.matcher(sql);
    if (matcher.find()) {
      String tableList = matcher.group(1).trim();
      if (tableList != null && !tableList.equalsIgnoreCase("SUBQUERY")) {
        // 分割逗号分隔的表名
        String[] tableNames = tableList.split(",");
        for (String tableName : tableNames) {
          tableName = tableName.trim();
          if (!tableName.isEmpty()) {
            // 移除可能的别名
            // 格式可能是：table_name 或 table_name AS alias 或 table_name alias
            // 使用正则匹配表名部分（第一个单词，可能包含点号如 database.table）
            Pattern tableNamePattern = Pattern.compile("([\\w\\.`]+)");
            Matcher nameMatcher = tableNamePattern.matcher(tableName);
            if (nameMatcher.find()) {
              String cleanTableName = nameMatcher.group(1);
              if (cleanTableName != null && !cleanTableName.equalsIgnoreCase("SUBQUERY")) {
                tables.add(cleanTableName);
              }
            }
          }
        }
      }
    }

    return tables;
  }

  /** 提取JOIN子句中的表名 */
  private static Set<String> extractJoinClause(String sql) {
    Set<String> tables = new HashSet<>();
    Matcher matcher = JOIN_PATTERN.matcher(sql);
    while (matcher.find()) {
      String tableName = matcher.group(1);
      if (tableName != null && !tableName.equalsIgnoreCase("SUBQUERY")) {
        tables.add(tableName);
      }
    }
    return tables;
  }

  /** 提取UPDATE语句中的表名 */
  private static Set<String> extractUpdateClause(String sql) {
    Set<String> tables = new HashSet<>();
    Matcher matcher = UPDATE_PATTERN.matcher(sql);
    while (matcher.find()) {
      String tableName = matcher.group(1);
      if (tableName != null) {
        tables.add(tableName);
      }
    }
    return tables;
  }

  /** 提取INSERT语句中的表名 */
  private static Set<String> extractInsertClause(String sql) {
    Set<String> tables = new HashSet<>();
    Matcher matcher = INSERT_PATTERN.matcher(sql);
    while (matcher.find()) {
      String tableName = matcher.group(1);
      if (tableName != null) {
        tables.add(tableName);
      }
    }
    return tables;
  }

  /** 提取DELETE语句中的表名 */
  private static Set<String> extractDeleteClause(String sql) {
    Set<String> tables = new HashSet<>();
    Matcher matcher = DELETE_PATTERN.matcher(sql);
    while (matcher.find()) {
      String tableName = matcher.group(1);
      if (tableName != null) {
        tables.add(tableName);
      }
    }
    return tables;
  }

  /** 清理表名 */
  private static Set<String> cleanTableNames(Set<String> tableNames) {
    Set<String> cleanNames = new HashSet<>();
    for (String tableName : tableNames) {
      if (tableName != null && !tableName.trim().isEmpty()) {
        // 移除反引号
        String clean = tableName.replace("`", "");

        // 如果包含数据库名前缀（如 database.table），只保留表名
        if (clean.contains(".")) {
          String[] parts = clean.split("\\.");
          clean = parts[parts.length - 1];
        }

        // 转换为小写以便比较
        clean = clean.toLowerCase().trim();

        if (!clean.isEmpty()) {
          cleanNames.add(clean);
        }
      }
    }
    return cleanNames;
  }

  /** 检查SQL语句是否为只读操作 */
  public static boolean isReadOnlyQuery(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return true;
    }

    String cleanSql = preprocessSql(sql).toUpperCase();

    // 检查是否包含修改操作的关键字
    String[] writeOperations = {
      "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE"
    };

    for (String operation : writeOperations) {
      if (cleanSql.matches(".*\\b" + operation + "\\b.*")) {
        return false;
      }
    }

    return true;
  }

  /** 获取SQL语句的操作类型 */
  public static String getSqlOperationType(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return "UNKNOWN";
    }

    String cleanSql = preprocessSql(sql).toUpperCase().trim();

    if (cleanSql.startsWith("SELECT")) {
      return "SELECT";
    } else if (cleanSql.startsWith("INSERT")) {
      return "INSERT";
    } else if (cleanSql.startsWith("UPDATE")) {
      return "UPDATE";
    } else if (cleanSql.startsWith("DELETE")) {
      return "DELETE";
    } else if (cleanSql.startsWith("CREATE")) {
      return "CREATE";
    } else if (cleanSql.startsWith("DROP")) {
      return "DROP";
    } else if (cleanSql.startsWith("ALTER")) {
      return "ALTER";
    } else if (cleanSql.startsWith("TRUNCATE")) {
      return "TRUNCATE";
    } else {
      return "OTHER";
    }
  }
}
