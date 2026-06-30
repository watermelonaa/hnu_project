package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("llm_configs")
public class LlmConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String name;

  private String version;

  private String apiKey;

  private String apiUrl;

  private Integer statusId;

  private Integer isDisabled;

  private Integer timeout;

  private Long createUserId;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;
}

