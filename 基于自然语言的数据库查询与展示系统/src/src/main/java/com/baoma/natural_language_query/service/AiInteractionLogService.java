package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mongodb.AiInteractionLog;
import java.util.List;

public interface AiInteractionLogService {

  List<AiInteractionLog> listByUserId(Long userId);

  List<AiInteractionLog> listByLlmName(String llmName);

  List<AiInteractionLog> listByLlmNameAndStatus(String llmName, String status);

  AiInteractionLog save(AiInteractionLog aiInteractionLog);
}
