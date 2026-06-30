package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.LlmStatus;
import com.baoma.natural_language_query.service.LLMStatusService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/llm-status")
public class LLMStatusController {

  @Autowired private LLMStatusService llmStatusService;

  /** 查询所有大模型状态 */
  @GetMapping("/list")
  public Result<List<LlmStatus>> list() {
    return Result.success(llmStatusService.list());
  }

  /** 根据ID查询 */
  @GetMapping("/{id}")
  public Result<LlmStatus> getById(@PathVariable Integer id) {
    return Result.success(llmStatusService.getById(id));
  }

  /** 根据状态编码查询 */
  @GetMapping("/code/{statusCode}")
  public Result<LlmStatus> getByStatusCode(@PathVariable String statusCode) {
    return Result.success(llmStatusService.getByStatusCode(statusCode));
  }

  /** 添加大模型状态 */
  @PostMapping
  public Result<LlmStatus> save(@RequestBody LlmStatus llmStatus) {
    llmStatusService.save(llmStatus);
    return Result.success(llmStatus);
  }

  /** 更新大模型状态 */
  @PutMapping
  public Result<LlmStatus> update(@RequestBody LlmStatus llmStatus) {
    llmStatusService.updateById(llmStatus);
    return Result.success(llmStatus);
  }

  /** 删除大模型状态 */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Integer id) {
    llmStatusService.removeById(id);
    return Result.success();
  }
}
