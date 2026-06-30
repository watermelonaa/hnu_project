package com.baoma.natural_language_query.repository;

import com.baoma.natural_language_query.entity.mongodb.AiInteractionLog;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiInteractionLogRepository extends MongoRepository<AiInteractionLog, String> {

  List<AiInteractionLog> findByUserId(Long userId);

  List<AiInteractionLog> findByLlmName(String llmName);

  List<AiInteractionLog> findByLlmNameAndStatus(String llmName, String status);
}
