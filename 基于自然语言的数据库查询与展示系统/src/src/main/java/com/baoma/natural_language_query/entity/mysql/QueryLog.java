package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("query_logs")
public class QueryLog {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String dialogId;

  private Long dataSourceId;

  private Long userId;

  /** 用户原始提问（自然语言） */
  private String userPrompt;

  private LocalDate queryDate;

  private LocalDateTime queryTime;

  private Integer executeResult;
}
