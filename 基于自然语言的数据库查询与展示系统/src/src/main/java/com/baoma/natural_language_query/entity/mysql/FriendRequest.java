package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("friend_requests")
public class FriendRequest {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long applicantId;

  private Long recipientId;

  private String applyMsg;

  private Integer status;

  private LocalDateTime createTime;

  private LocalDateTime handleTime;
}
