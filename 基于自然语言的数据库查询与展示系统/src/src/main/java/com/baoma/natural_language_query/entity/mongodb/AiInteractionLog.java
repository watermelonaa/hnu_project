package com.baoma.natural_language_query.entity.mongodb;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "ai_interaction_logs")
public class AiInteractionLog {

  @Id private String id;

  private Long userId;

  private String requestType;

  private String llmName;

  private Map<String, Object> requestParams;

  private Map<String, Object> responseResult;

  private TokenUsage tokenUsage;

  private Integer responseTime;

  private String status;

  private String errorMsg;

  private LocalDateTime createTime;

  @Data
  public static class TokenUsage {
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
  }
}
