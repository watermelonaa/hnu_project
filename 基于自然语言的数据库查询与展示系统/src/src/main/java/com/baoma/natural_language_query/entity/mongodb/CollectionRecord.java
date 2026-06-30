package com.baoma.natural_language_query.entity.mongodb;

import java.time.LocalDateTime;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Data
@Document(collection = "collection_records")
public class CollectionRecord {

  @Id private String id;

  @Field("queryId")
  private ObjectId queryId; // MongoDB schema 要求 objectId 类型

  private Long userId;

  private String sqlContent;

  private String userPrompt; // 用户查询的自然语言提示，用于分类和对比

  private Map<String, Object> queryResult;

  private Long dbConnectionId;

  private Long llmConfigId;

  private LocalDateTime createTime;
}
