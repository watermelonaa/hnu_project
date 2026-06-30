package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("friend_relations")
public class FriendRelation {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;

  private Long friendId;

  private String friendUsername;

  private Integer onlineStatus;

  private String remarkName;

  private LocalDateTime createTime;
}
