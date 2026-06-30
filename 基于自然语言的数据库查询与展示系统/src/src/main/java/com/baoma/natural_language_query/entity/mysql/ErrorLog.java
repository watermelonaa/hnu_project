package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("error_logs")
public class ErrorLog {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private Integer errorTypeId;

  private Integer errorCount;

  private BigDecimal errorRate;

  private String period;

  private LocalDateTime statTime;
}
