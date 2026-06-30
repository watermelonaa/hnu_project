package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("user_db_permissions")
public class UserDbPermission {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Long userId;

  @TableField("permission_details")
  private String permissionDetails; // JSON格式字符串

  @TableField("last_grant_user_id")
  private Long lastGrantUserId;

  @TableField("is_assigned")
  private Integer isAssigned;

  @TableField("last_grant_time")
  private LocalDateTime lastGrantTime;

  @TableField("update_time")
  private LocalDateTime updateTime;
}
