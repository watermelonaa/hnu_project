package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("notifications")
public class Notification {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String title;

  private String content;

  @TableField("target_id")
  private Integer targetId;

  @TableField("priority_id")
  private Integer priorityId;

  @TableField("publisher_id")
  private Long publisherId;

  @TableField("is_top")
  private Integer isTop;

  @TableField("publish_time")
  private LocalDateTime publishTime;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("latest_update_time")
  private LocalDateTime latestUpdateTime;
}
