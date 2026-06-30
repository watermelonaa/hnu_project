package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.TokenConsume;
import java.time.LocalDate;
import java.util.List;

public interface TokenConsumeService {

  List<TokenConsume> list();

  TokenConsume getByLlmNameAndDate(String llmName, LocalDate consumeDate);

  List<TokenConsume> listByDateRange(LocalDate startDate, LocalDate endDate);

  boolean save(TokenConsume tokenConsume);

  boolean updateById(TokenConsume tokenConsume);
}
