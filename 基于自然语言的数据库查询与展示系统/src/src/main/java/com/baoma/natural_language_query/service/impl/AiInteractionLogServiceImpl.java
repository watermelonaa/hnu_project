package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mongodb.AiInteractionLog;
import com.baoma.natural_language_query.repository.AiInteractionLogRepository;
import com.baoma.natural_language_query.service.AiInteractionLogService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiInteractionLogServiceImpl implements AiInteractionLogService {

  @Autowired private AiInteractionLogRepository aiInteractionLogRepository;

  @Override
  public List<AiInteractionLog> listByUserId(Long userId) {
    return aiInteractionLogRepository.findByUserId(userId);
  }

  @Override
  public List<AiInteractionLog> listByLlmName(String llmName) {
    return aiInteractionLogRepository.findByLlmName(llmName);
  }

  @Override
  public List<AiInteractionLog> listByLlmNameAndStatus(String llmName, String status) {
    return aiInteractionLogRepository.findByLlmNameAndStatus(llmName, status);
  }

  @Override
  public AiInteractionLog save(AiInteractionLog aiInteractionLog) {
    return aiInteractionLogRepository.save(aiInteractionLog);
  }
}
