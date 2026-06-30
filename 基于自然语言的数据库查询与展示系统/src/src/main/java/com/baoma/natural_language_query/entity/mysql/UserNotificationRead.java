package com.baoma.natural_language_query.entity.mysql;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("user_notification_reads")
public class UserNotificationRead {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Long userId;

  @TableField("notification_id")
  private Long notificationId;

  @TableField("is_read")
  private Integer isRead;

  @TableField("read_time")
  private LocalDateTime readTime;

  @TableField("create_time")
  private LocalDateTime createTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}

