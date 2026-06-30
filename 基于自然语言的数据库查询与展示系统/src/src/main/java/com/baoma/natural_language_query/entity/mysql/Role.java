package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("roles")
public class Role {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private String roleName;

  private String roleCode;

  private String description;
}
