package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("error_types")
public class ErrorType {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String errorName;

  private String errorCode;

  private String description;
}
