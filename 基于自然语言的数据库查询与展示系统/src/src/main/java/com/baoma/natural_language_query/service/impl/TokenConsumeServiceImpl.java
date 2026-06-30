package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.entity.mysql.TokenConsume;
import com.baoma.natural_language_query.mapper.TokenConsumeMapper;
import com.baoma.natural_language_query.service.TokenConsumeService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenConsumeServiceImpl implements TokenConsumeService {

  @Autowired private TokenConsumeMapper tokenConsumeMapper;

  @Override
  public List<TokenConsume> list() {
    return tokenConsumeMapper.selectList(null);
  }

  @Override
  public TokenConsume getByLlmNameAndDate(String llmName, LocalDate consumeDate) {
    QueryWrapper<TokenConsume> wrapper = new QueryWrapper<>();
    wrapper.eq("llm_name", llmName).eq("consume_date", consumeDate);
    return tokenConsumeMapper.selectOne(wrapper);
  }

  @Override
  public List<TokenConsume> listByDateRange(LocalDate startDate, LocalDate endDate) {
    QueryWrapper<TokenConsume> wrapper = new QueryWrapper<>();
    wrapper.between("consume_date", startDate, endDate);
    return tokenConsumeMapper.selectList(wrapper);
  }

  @Override
  public boolean save(TokenConsume tokenConsume) {
    return tokenConsumeMapper.insert(tokenConsume) > 0;
  }

  @Override
  public boolean updateById(TokenConsume tokenConsume) {
    return tokenConsumeMapper.updateById(tokenConsume) > 0;
  }
}
