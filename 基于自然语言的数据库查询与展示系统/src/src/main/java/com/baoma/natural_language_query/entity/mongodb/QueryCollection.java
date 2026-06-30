package com.baoma.natural_language_query.entity.mongodb;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "query_collections")
public class QueryCollection {

  @Id private String id;

  private Long userId;

  private String groupName; // 收藏夹名称（对应前端的collectionName）

  private String description; // 收藏夹描述

  private LocalDateTime createTime;
}
