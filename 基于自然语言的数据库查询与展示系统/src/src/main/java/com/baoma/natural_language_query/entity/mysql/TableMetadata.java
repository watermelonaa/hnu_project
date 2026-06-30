package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("table_metadata")
public class TableMetadata {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private Long dbConnectionId;

  private String tableName;

  private String description;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;
}
