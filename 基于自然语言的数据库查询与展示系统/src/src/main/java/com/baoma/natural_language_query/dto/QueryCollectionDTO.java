package com.baoma.natural_language_query.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 查询收藏夹DTO，用于接收前端数据
 * 前端字段名与后端实体类字段名不一致，需要映射
 */
@Data
public class QueryCollectionDTO {

  private String id;

  private Long userId;

  // 前端发送的是 collectionName，需要映射到 groupName
  @JsonProperty("collectionName")
  private String collectionName;

  private String description;

  private LocalDateTime createTime;
}

