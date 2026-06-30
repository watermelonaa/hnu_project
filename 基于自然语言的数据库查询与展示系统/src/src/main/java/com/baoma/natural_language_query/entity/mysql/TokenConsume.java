package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("token_consume")
public class TokenConsume {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String llmName;

  private Integer totalTokens;

  private Integer promptTokens;

  private Integer completionTokens;

  private LocalDate consumeDate;

  private BigDecimal growthRate;
}
