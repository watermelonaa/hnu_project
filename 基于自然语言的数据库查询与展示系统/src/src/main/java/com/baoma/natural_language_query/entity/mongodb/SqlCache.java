package com.baoma.natural_language_query.entity.mongodb;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "sql_cache")
public class SqlCache {

  @Id private String id;

  private String nlHash;

  private Long userId;

  private Long connectionId;

  private List<Long> tableIds;

  private String dbType;

  private String generatedSql;

  private Integer hitCount;

  private LocalDateTime expireTime;
}
