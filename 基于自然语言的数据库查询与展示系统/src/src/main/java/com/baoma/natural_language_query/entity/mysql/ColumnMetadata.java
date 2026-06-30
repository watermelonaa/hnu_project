package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("column_metadata")
public class ColumnMetadata {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private Long tableId;

  private String columnName;

  private String dataType;

  private String description;

  private Integer isPrimary;

  private LocalDateTime createTime;
}
