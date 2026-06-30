package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 数据库连接实体类
 *
 * <p>对应MySQL数据库中的db_connections表，存储系统管理的数据库连接配置信息。 管理员可以创建和管理多个数据库连接，用户根据权限可以访问授权的数据库。
 *
 * <p>数据库连接状态：
 *
 * <ul>
 *   <li>enabled - 启用（可以正常使用）
 *   <li>disabled - 禁用（暂时不可用）
 * </ul>
 *
 * @author 项目组
 * @version 1.0
 */
@Data
@TableName("db_connections")
public class DbConnection {

  /** 连接ID（主键，自增） */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /** 连接名称（用户友好的名称） */
  private String name;

  /**
   * 数据库类型ID
   *
   * <p>关联DbType表，如MySQL、PostgreSQL等
   */
  private Integer dbTypeId;

  /**
   * 数据库连接URL
   *
   * <p>例如：jdbc:mysql://localhost:3306/database_name
   */
  private String url;

  /** 数据库用户名 */
  private String username;

  /** 数据库密码（建议加密存储） */
  private String password;

  /**
   * 连接状态
   *
   * <p>enabled - 启用，disabled - 禁用
   */
  private String status;

  /** 创建者用户ID */
  private Long createUserId;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 软删除标志 */
  @TableLogic // 可选，但建议保留
  private Integer deleted;
}
