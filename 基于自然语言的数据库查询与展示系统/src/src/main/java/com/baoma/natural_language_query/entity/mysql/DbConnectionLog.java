package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("db_connection_logs")
public class DbConnectionLog {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long dbConnectionId;

  private String dbName;

  private LocalDateTime connectTime;

  private String status;

  private String remark;

  private Long handlerId;


}
