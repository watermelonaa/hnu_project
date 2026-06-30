package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mongodb.AiInteractionLog;
import com.baoma.natural_language_query.service.AiInteractionLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-interaction-log")
public class AiInteractionLogController {

  @Autowired private AiInteractionLogService aiInteractionLogService;

  @GetMapping("/list/user/{userId}")
  public Result<List<AiInteractionLog>> listByUserId(@PathVariable Long userId) {
    return Result.success(aiInteractionLogService.listByUserId(userId));
  }

  @GetMapping("/list/llm/{llmName}")
  public Result<List<AiInteractionLog>> listByLlmName(@PathVariable String llmName) {
    return Result.success(aiInteractionLogService.listByLlmName(llmName));
  }

  @GetMapping("/list/llm/{llmName}/{status}")
  public Result<List<AiInteractionLog>> listByLlmNameAndStatus(
      @PathVariable String llmName, @PathVariable String status) {
    return Result.success(aiInteractionLogService.listByLlmNameAndStatus(llmName, status));
  }

  @PostMapping
  public Result<AiInteractionLog> save(@RequestBody AiInteractionLog aiInteractionLog) {
    aiInteractionLog.setCreateTime(LocalDateTime.now());
    AiInteractionLog saved = aiInteractionLogService.save(aiInteractionLog);
    return Result.success(saved);
  }
}
