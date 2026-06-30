package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.entity.mysql.TokenConsume;
import com.baoma.natural_language_query.service.TokenConsumeService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token-consume")
public class TokenConsumeController {

  @Autowired private TokenConsumeService tokenConsumeService;

  @GetMapping("/list")
  public Result<List<TokenConsume>> list() {
    return Result.success(tokenConsumeService.list());
  }

  @GetMapping("/{llmName}/{consumeDate}")
  public Result<TokenConsume> getByLlmNameAndDate(
      @PathVariable String llmName, @PathVariable String consumeDate) {
    LocalDate date = LocalDate.parse(consumeDate);
    return Result.success(tokenConsumeService.getByLlmNameAndDate(llmName, date));
  }

  @GetMapping("/range/{startDate}/{endDate}")
  public Result<List<TokenConsume>> listByDateRange(
      @PathVariable String startDate, @PathVariable String endDate) {
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);
    return Result.success(tokenConsumeService.listByDateRange(start, end));
  }

  @PostMapping
  public Result<TokenConsume> save(@RequestBody TokenConsume tokenConsume) {
    tokenConsumeService.save(tokenConsume);
    return Result.success(tokenConsume);
  }

  @PutMapping
  public Result<TokenConsume> update(@RequestBody TokenConsume tokenConsume) {
    tokenConsumeService.updateById(tokenConsume);
    return Result.success(tokenConsume);
  }
}
