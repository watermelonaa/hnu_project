package com.baoma.natural_language_query.entity.mongodb;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "dialog_details")
public class DialogDetail {

  @Id private String id;

  private String dialogId;

  private List<Round> rounds;

  @Data
  public static class Round {
    private Integer roundNum;
    private String userInput;
    private String aiResponse;
    private String generatedSql;
    private LocalDateTime roundTime;
  }
}
