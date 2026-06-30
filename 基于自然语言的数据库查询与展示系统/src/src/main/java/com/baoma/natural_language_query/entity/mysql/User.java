package com.baoma.natural_language_query.entity.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户实体类
 *
 * <p>对应MySQL数据库中的users表，存储系统用户的基本信息。 用户包括管理员和普通用户，通过roleId区分角色。
 *
 * <p>用户状态说明：
 *
 * <ul>
 *   <li>status: 账号状态（0-禁用，1-启用）
 *   <li>onlineStatus: 在线状态（0-离线，1-在线）
 * </ul>
 *
 * @author 项目组
 * @version 1.0
 */
@Data
@TableName("users")
public class User {

  /** 用户ID（主键，自增） */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 用户名（唯一，用于登录） */
  private String username;

  /** 密码（BCrypt加密存储） */
  private String password;

  /** 邮箱地址 */
  private String email;

  /** 手机号码 */
  private String phonenumber;

  /**
   * 角色ID
   *
   * <p>关联Role表，用于权限控制
   */
  private Integer roleId;

  /** 头像URL */
  private String avatarUrl;

  /**
   * 账号状态
   *
   * <p>0-禁用，1-启用
   */
  private Integer status;

  /**
   * 在线状态
   *
   * <p>0-离线，1-在线
   */
  private Integer onlineStatus;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 更新时间 */
  private LocalDateTime updateTime;
}
