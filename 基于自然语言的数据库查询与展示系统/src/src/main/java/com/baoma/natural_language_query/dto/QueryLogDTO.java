package com.baoma.natural_language_query.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.Data;

/**
 * 查询日志DTO，用于接收前端数据
 * 前端字段名与后端实体类字段名不一致，需要映射
 */
@Data
public class QueryLogDTO {

  private Long id;

  private Long userId;

  private String dialogId;

  // 前端发送的是 dbConnectionId，需要映射到 dataSourceId
  @JsonProperty("dbConnectionId")
  private Long dbConnectionId;

  // 以下字段前端发送但后端实体类可能没有，先接收但不使用
  private String userPrompt;
  private String sqlQuery;
  private String queryResult;
  private String executionTime;
  private Long llmConfigId;

  private LocalDate queryDate;
  
  // 前端发送的是 ISO 字符串格式，使用 String 接收，然后手动转换
  @JsonProperty("queryTime")
  private String queryTimeStr;
  
  private Integer executeResult;
  
  /**
   * 将字符串格式的 queryTime 转换为 LocalDateTime
   * 使用 @JsonIgnore 避免 Jackson 将其识别为 queryTime 属性的 getter
   */
  @JsonIgnore
  public LocalDateTime getQueryTime() {
    if (queryTimeStr == null || queryTimeStr.trim().isEmpty()) {
      return null;
    }
    try {
      // 尝试解析 ISO 格式：2025-12-27T16:45:22.395Z 或 2025-12-27T16:45:22.395
      if (queryTimeStr.contains("T")) {
        // ISO 格式，移除 Z 后缀（如果有）
        String cleanTime = queryTimeStr.replace("Z", "");
        // 如果包含毫秒
        if (cleanTime.contains(".")) {
          return LocalDateTime.parse(cleanTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        } else {
          return LocalDateTime.parse(cleanTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        }
      } else {
        // 普通日期时间格式
        return LocalDateTime.parse(queryTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      }
    } catch (DateTimeParseException e) {
      // 解析失败，返回 null，Controller 会使用当前时间
      return null;
    }
  }
}

