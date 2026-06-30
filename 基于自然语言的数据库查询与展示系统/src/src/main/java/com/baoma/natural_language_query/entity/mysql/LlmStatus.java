package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("llm_status")
public class LlmStatus {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String statusName;

  private String statusCode;

  private String description;
}
