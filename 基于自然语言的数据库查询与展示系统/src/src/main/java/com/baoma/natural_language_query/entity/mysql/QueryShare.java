package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("query_shares")
public class QueryShare {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long shareUserId;

  private Long receiveUserId;

  private String dialogId;

  private String targetRounds;

  private String queryTitle;

  private LocalDateTime shareTime;

  private Integer receiveStatus;
}
