package com.baoma.natural_language_query.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;

/**
 * 收藏记录DTO，用于接收前端数据
 */
@Data
public class CollectionRecordDTO {

  /** 收藏夹ID（前端发送的是 collectionId，需要映射到 queryId）
   * 注意：MongoDB 返回的 ID 是 String 类型（ObjectId），所以这里使用 String 或 Long 都可以
   * 为了兼容性，同时支持 String 和 Long
   */
  @JsonProperty("collectionId")
  private Object collectionId; // 使用 Object 以同时支持 String 和 Long

  /** 查询日志ID（用于获取查询详情） */
  @JsonProperty("queryLogId")
  private Long queryLogId;

  /** 用户ID */
  private Long userId;

  /** SQL内容（可选，如果提供则直接使用） */
  private String sqlContent;

  /** 用户提示（可选，如果提供则直接使用） */
  private String userPrompt;

  /** 查询结果（可选，如果提供则直接使用） */
  private Map<String, Object> queryResult;

  /** 数据库连接ID（可选，如果提供则直接使用） */
  private Long dbConnectionId;

  /** 大模型配置ID（可选，如果提供则直接使用） */
  private Long llmConfigId;
}

