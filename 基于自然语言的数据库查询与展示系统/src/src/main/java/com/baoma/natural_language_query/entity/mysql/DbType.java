package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_types")
public class DbType {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String typeName;

  private String typeCode;

  private String description;
}
