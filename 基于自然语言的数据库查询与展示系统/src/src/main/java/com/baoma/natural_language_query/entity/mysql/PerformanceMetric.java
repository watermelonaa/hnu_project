package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("performance_metrics")
public class PerformanceMetric {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String metricType;

  private BigDecimal metricValue;

  private LocalDateTime metricTime;

  private Integer trend;
}
