package com.baoma.natural_language_query.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.sql.*;
import java.util.*;
import lombok.Data;

/**
 * 数据库Schema导出工具
 */
@Data
class TableSchema {
    private String name;
    private String comment;
    private List<ColumnSchema> columns;
}

@Data
class ColumnSchema {
    private String field;
    private String type;
    private Boolean nullable;
    private String key;
    private String defaultValue;
    private String extra;
}

@Data
class DatabaseSchema {
    private List<TableSchema> tables = new ArrayList<>();
}

public class DatabaseSchemaExporter {
    
    private final Connection connection;
    
    public DatabaseSchemaExporter(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * 快速导出schema为JSON对象
     */
    public DatabaseSchema fetchSchemaJson(String databaseName) throws SQLException {
        DatabaseSchema schema = new DatabaseSchema();
        
        // 获取所有表
        List<String> tableNames = getTableNames(databaseName);
        
        for (String tableName : tableNames) {
            TableSchema tableSchema = new TableSchema();
            tableSchema.setName(tableName);
            tableSchema.setComment(getTableComment(databaseName, tableName));
            tableSchema.setColumns(getTableColumns(tableName));
            schema.getTables().add(tableSchema);
        }
        
        return schema;
    }
    
    /**
     * 获取所有表名
     */
    private List<String> getTableNames(String databaseName) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }
    
    /**
     * 获取表注释
     */
    private String getTableComment(String databaseName, String tableName) throws SQLException {
        String sql = "SELECT TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, databaseName);
            pstmt.setString(2, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("TABLE_COMMENT");
                }
            }
        }
        return "";
    }
    
    /**
     * 获取表字段信息
     */
    private List<ColumnSchema> getTableColumns(String tableName) throws SQLException {
        List<ColumnSchema> columns = new ArrayList<>();
        String sql = "DESCRIBE `" + tableName + "`";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ColumnSchema column = new ColumnSchema();
                column.setField(rs.getString("Field"));
                column.setType(rs.getString("Type"));
                column.setNullable("YES".equals(rs.getString("Null")));
                column.setKey(rs.getString("Key"));
                column.setDefaultValue(rs.getString("Default"));
                column.setExtra(rs.getString("Extra"));
                columns.add(column);
            }
        }
        return columns;
    }
    
    /**
     * 将schema转换为JSON字符串
     */
    public String toJsonString(DatabaseSchema schema) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(schema);
    }
}