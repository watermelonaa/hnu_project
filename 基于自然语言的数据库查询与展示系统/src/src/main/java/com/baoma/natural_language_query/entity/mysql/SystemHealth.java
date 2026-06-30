package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("system_health")
public class SystemHealth {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private Integer dbDelay;

  private Integer cacheDelay;

  private Integer llmDelay;

  private BigDecimal storageUsage;

  private LocalDateTime collectTime;
}
