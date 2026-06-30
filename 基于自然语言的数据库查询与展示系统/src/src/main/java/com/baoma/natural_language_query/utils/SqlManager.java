package com.baoma.natural_language_query.utils;

import com.fasterxml.jackson.databind.ObjectMapper;   
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * SQL 管理器
 * 
 * 提供 NL2SQL项目中 SQL 语句的后处理和验证功能。
 * 主要功能包括：
 * 1. 从模型返回结果中提取并清理 SQL 语句
 * 2. 标准化 SQL 格式
 * 3. 提供 SQL 验证的扩展点
 * 
 * 使用单例模式的 ObjectMapper 以提高性能，所有方法均为静态方法，可直接调用。
 * 
 * @author baoma
 * @version 1.0
 * @since 2024
 */

public class SqlManager {

    /**
     * 单例 ObjectMapper 实例，用于 JSON 解析
     * Jackson 的 ObjectMapper 是线程安全的，可以全局复用
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 私有构造函数，防止实例化
     * 这是一个工具类，所有方法都是静态的
     */
    private SqlManager() {
        throw new UnsupportedOperationException("这是一个工具类，不需要实例化");
    }

    /**
     * 提取后处理 SQL
     * 
     * 对从 NL2SQL 模型中提取的 SQL 语句进行清洗和标准化处理，包括：
     * 1. 移除 JSON 包装（如果 SQL 被包裹在 JSON 中）
     * 2. 移除末尾多余的分号
     * 3. 标准化空白字符（多个空格合并为一个）
     * 4. 确保 SQL 以 SELECT 开头
     * 5. 统一添加标准分号结尾
     * 
     * @param sql 从模型提取的原始 SQL 字符串，可能包含 JSON 包装或其他格式问题
     * @return 处理后的标准化 SQL 语句，如果无法处理则返回空字符串
     * 
     * @example
     * 输入: {"sql": "SELECT * FROM users WHERE age > 25;;"}
     * 输出: SELECT * FROM users WHERE age > 25;
     * 
     * 输入: "无效的前缀 SELECT name FROM users"
     * 输出: SELECT name FROM users;
     */

    public static String postProcessExtractedSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        String processedSql = sql.trim();
        // 1. 移除 JSON 包装
        // 模型可能返回 JSON 格式: {"sql": "SELECT * FROM table"}
        if (processedSql.startsWith("{") && processedSql.contains("\"sql\":")) {
            try {
                ObjectNode json = (ObjectNode) objectMapper.readTree(processedSql);
                String extractedSql = json.path("sql").asText();
                if (extractedSql != null && !extractedSql.trim().isEmpty()) {
                    processedSql = extractedSql.trim();
                }
            } catch (Exception ignored) {
                // 非合法 JSON 则保持原样，继续后续处理
            }
        }

        // 2. 移除末尾多余分号
        // 处理多个分号的情况，如: SELECT * FROM table;;
        while (processedSql.endsWith(";")) {
            processedSql = processedSql.substring(0, processedSql.length() - 1).trim();
        }

        // 3. 标准化空白
        // 将多个连续空白字符替换为单个空格
        processedSql = processedSql.replaceAll("\\s+", " ");

        // 4. 确保以 SELECT 开头
        // 模型可能在 SQL 前添加了无关文本
        if (!processedSql.toUpperCase().startsWith("SELECT")) {
            int idx = processedSql.toUpperCase().indexOf("SELECT");
            if (idx > 0) {
                // 找到 SELECT 关键字，截取其后部分
                processedSql = processedSql.substring(idx);
            } else {
                // 没有找到 SELECT，返回空字符串
                return "";
            }
        }
        // 5. 统一添加分号结尾
        return processedSql + ";";
    }

    /**
     * 验证提取的 SQL
     * 
     * 对处理后的 SQL 进行基本验证：
     * 1. 基本格式检查
     * 2. 安全性检查（防止非查询操作）
     * 3. 简单语法检查
     * 
     * @param sql 需要验证的 SQL 语句
     * 
     * @throws IllegalArgumentException 如果 SQL 不合法或包含危险操作
     */
    public static void validateExtractedSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 语句不能为空");
        }
        
        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase();
        
        // 1. 基本格式检查
        if (!trimmedSql.endsWith(";")) {
            throw new IllegalArgumentException("SQL 语句必须以分号结尾");
        }
        
        // 2. 必须是以 SELECT 开头的查询语句
        if (!upperSql.startsWith("SELECT")) {
            throw new IllegalArgumentException("只支持 SELECT 查询语句");
        }
        
        // 3. 安全性检查 - 禁止数据修改操作
        String[] dangerousKeywords = {
            "INSERT", "UPDATE", "DELETE", "DROP", 
            "ALTER", "CREATE", "TRUNCATE", "GRANT",
            "REVOKE", "MERGE", "REPLACE", "LOCK"
        };
        
        for (String keyword : dangerousKeywords) {
            // 使用正则确保是独立的 SQL 关键字，而不是列名或值的一部分
            String pattern = "\\b" + keyword + "\\b";
            if (upperSql.matches(".*" + pattern + ".*")) {
                throw new SecurityException("SQL 包含危险操作: " + keyword);
            }
        }
        
        // 4. 检查是否有 WHERE 1=1 这种恒真条件（可能的 SQL 注入特征）
        if (upperSql.contains("WHERE 1=1") || upperSql.contains("WHERE 'A'='A'")) {
            throw new SecurityException("SQL 可能包含注入特征");
        }
        
        // 5. 检查子查询嵌套深度（简单实现）
        int selectCount = countOccurrences(upperSql, "SELECT");
        int fromCount = countOccurrences(upperSql, "FROM");
        
        if (selectCount > 5) {
            throw new IllegalArgumentException("SQL 过于复杂，子查询嵌套过深");
        }
        
        // 6. 检查基本语法结构
        if (fromCount == 0) {
            throw new IllegalArgumentException("SQL 缺少 FROM 子句");
        }

        //  7.检查是否有明显的语法错误（简单的括号匹配）
        if (!isParenthesesBalanced(trimmedSql)) {
            throw new IllegalArgumentException("SQL 括号不匹配");
        }
        
    }

    /**
     * 计算字符串中某个子串出现的次数
     */
    private static int countOccurrences(String str, String sub) {
        if (str == null || sub == null || str.isEmpty() || sub.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * 检查括号是否匹配
     */
    private static boolean isParenthesesBalanced(String sql) {
        int balance = 0;
        for (char c : sql.toCharArray()) {
            if (c == '(') {
                balance++;
            } else if (c == ')') {
                balance--;
                if (balance < 0) {
                    return false; // 右括号多于左括号
                }
            }
        }
        return balance == 0; // 括号完全匹配
    }


   
    /**
     * 完整的 SQL 验证方法（包含异常信息收集）
     * 
     * @param sql 需要验证的 SQL 语句
     * @return 验证结果对象，包含是否通过和错误信息
     */
    public static ValidationResult validateSqlWithResult(String sql) {
        try {
            validateExtractedSql(sql);
            return new ValidationResult(true, "SQL 验证通过");
        } catch (Exception e) {
            return new ValidationResult(false, e.getMessage());
        }
    }

    /**
     * 验证结果封装类
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String message;
        
        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return "ValidationResult{isValid=" + isValid + ", message='" + message + "'}";
        }
    }
    
    /**
     * 获取 ObjectMapper 实例
     * 
     * 提供给外部使用的 ObjectMapper 实例，确保全局使用同一个实例
     * 
     * @return 单例 ObjectMapper 实例
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
  

}