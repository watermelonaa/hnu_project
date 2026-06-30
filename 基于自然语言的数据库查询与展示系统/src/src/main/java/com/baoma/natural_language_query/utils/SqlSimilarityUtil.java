package com.baoma.natural_language_query.utils;

import java.util.Set;

/**
 * SQL相似度判断工具类
 *
 * <p>用于判断两个SQL查询是否相似，支持：
 * <ul>
 *   <li>SQL标准化（移除空格、注释、大小写等）
 *   <li>提取SQL关键特征（表名、操作类型等）
 *   <li>计算SQL相似度
 *   <li>判断是否为相同查询的不同版本
 * </ul>
 *
 * @author 项目组
 * @version 1.0
 */
public class SqlSimilarityUtil {

  /**
   * 标准化SQL语句
   *
   * @param sql 原始SQL
   * @return 标准化后的SQL
   */
  public static String normalizeSql(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return "";
    }

    // 移除注释
    sql = sql.replaceAll("--[^\r\n]*", "");
    sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", "");

    // 移除多余空格，统一为单个空格
    sql = sql.replaceAll("\\s+", " ").trim();

    // 统一大小写（关键字大写）
    sql = normalizeKeywords(sql);

    // 移除反引号
    sql = sql.replaceAll("`", "");

    return sql;
  }

  /**
   * 标准化SQL关键字（统一为大写）
   */
  private static String normalizeKeywords(String sql) {
    String[] keywords = {
      "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "GROUP", "BY",
      "ORDER", "HAVING", "LIMIT", "OFFSET", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
      "AS", "AND", "OR", "NOT", "IN", "EXISTS", "LIKE", "BETWEEN", "IS", "NULL", "DISTINCT",
      "COUNT", "SUM", "AVG", "MAX", "MIN", "UNION", "ALL"
    };

    String normalized = sql.toUpperCase();
    for (String keyword : keywords) {
      normalized = normalized.replaceAll("\\b" + keyword + "\\b", keyword);
    }
    return normalized;
  }

  /**
   * 提取SQL中的表名集合
   *
   * @param sql SQL语句
   * @return 表名集合
   */
  public static Set<String> extractTableNames(String sql) {
    return SqlTableExtractor.extractTableNames(sql);
  }

  /**
   * 判断两个SQL是否相似（基于标准化后的SQL）
   *
   * <p>相似度判断标准：
   * <ul>
   *   <li>标准化后的SQL完全相同 -> 100%相似
   *   <li>表名相同且操作类型相同 -> 高度相似
   *   <li>使用编辑距离计算相似度
   * </ul>
   *
   * @param sql1 第一个SQL
   * @param sql2 第二个SQL
   * @return 相似度（0.0-1.0），1.0表示完全相同
   */
  public static double calculateSimilarity(String sql1, String sql2) {
    if (sql1 == null || sql2 == null) {
      return 0.0;
    }

    String normalized1 = normalizeSql(sql1);
    String normalized2 = normalizeSql(sql2);

    // 完全相同
    if (normalized1.equals(normalized2)) {
      return 1.0;
    }

    // 提取表名
    Set<String> tables1 = extractTableNames(sql1);
    Set<String> tables2 = extractTableNames(sql2);

    // 如果表名完全不同，相似度较低
    if (tables1.isEmpty() || tables2.isEmpty() || tables1.equals(tables2)) {
      // 表名相同，计算编辑距离相似度
      return calculateEditDistanceSimilarity(normalized1, normalized2);
    } else {
      // 表名不同，相似度较低
      return 0.0;
    }
  }

  /**
   * 使用编辑距离计算字符串相似度
   *
   * @param s1 字符串1
   * @param s2 字符串2
   * @return 相似度（0.0-1.0）
   */
  private static double calculateEditDistanceSimilarity(String s1, String s2) {
    int maxLen = Math.max(s1.length(), s2.length());
    if (maxLen == 0) {
      return 1.0;
    }

    int editDistance = calculateEditDistance(s1, s2);
    return 1.0 - (double) editDistance / maxLen;
  }

  /**
   * 计算编辑距离（Levenshtein距离）
   */
  private static int calculateEditDistance(String s1, String s2) {
    int m = s1.length();
    int n = s2.length();
    int[][] dp = new int[m + 1][n + 1];

    for (int i = 0; i <= m; i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= n; j++) {
      dp[0][j] = j;
    }

    for (int i = 1; i <= m; i++) {
      for (int j = 1; j <= n; j++) {
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
          dp[i][j] = dp[i - 1][j - 1];
        } else {
          dp[i][j] =
              Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
        }
      }
    }

    return dp[m][n];
  }

  /**
   * 判断两个SQL是否为相同查询（相似度阈值：0.8）
   *
   * @param sql1 第一个SQL
   * @param sql2 第二个SQL
   * @return 是否为相同查询
   */
  public static boolean isSameQuery(String sql1, String sql2) {
    return calculateSimilarity(sql1, sql2) >= 0.8;
  }

  /**
   * 判断两个SQL是否为相同查询（自定义阈值）
   *
   * @param sql1 第一个SQL
   * @param sql2 第二个SQL
   * @param threshold 相似度阈值（0.0-1.0）
   * @return 是否为相同查询
   */
  public static boolean isSameQuery(String sql1, String sql2, double threshold) {
    return calculateSimilarity(sql1, sql2) >= threshold;
  }

  /**
   * 判断两个用户提示是否相似（用于自然语言查询分类）
   *
   * @param prompt1 第一个提示
   * @param prompt2 第二个提示
   * @return 相似度（0.0-1.0）
   */
  public static double calculatePromptSimilarity(String prompt1, String prompt2) {
    if (prompt1 == null || prompt2 == null) {
      return 0.0;
    }

    // 标准化：转小写、移除标点、统一空格
    String normalized1 = prompt1.toLowerCase().replaceAll("[^\\w\\s]", "").trim();
    String normalized2 = prompt2.toLowerCase().replaceAll("[^\\w\\s]", "").trim();

    if (normalized1.equals(normalized2)) {
      return 1.0;
    }

    // 使用编辑距离计算相似度
    return calculateEditDistanceSimilarity(normalized1, normalized2);
  }

  /**
   * 判断两个用户提示是否为相同查询（相似度阈值：0.7）
   *
   * @param prompt1 第一个提示
   * @param prompt2 第二个提示
   * @return 是否为相同查询
   */
  public static boolean isSamePrompt(String prompt1, String prompt2) {
    return calculatePromptSimilarity(prompt1, prompt2) >= 0.7;
  }
}

