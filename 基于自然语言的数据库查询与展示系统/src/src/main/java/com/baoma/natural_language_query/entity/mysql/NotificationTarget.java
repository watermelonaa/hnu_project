package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("notification_targets")
public class NotificationTarget {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String targetName;

  private String targetCode;

  private String description;
}
