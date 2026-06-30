package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("operation_logs")
public class OperationLog {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private Long userId;

  private String username;

  @JsonAlias({"operateType", "operation"})
  private String operation;

  private String module;

  private String relatedLlm;

  @JsonAlias({"ip", "ipAddress"})
  private String ipAddress;

  private LocalDateTime operateTime;

  @JsonAlias({"status", "result"})
  private Integer result;

  private String errorMsg;
}
