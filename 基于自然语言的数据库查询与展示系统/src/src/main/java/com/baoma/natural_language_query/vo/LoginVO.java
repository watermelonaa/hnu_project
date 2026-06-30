package com.baoma.natural_language_query.vo;

import lombok.Data;

/**
 * 登录响应视图对象
 *
 * <p>用于返回用户登录成功后的信息，包括JWT令牌和用户基本信息。 前端可以使用这些信息进行后续的认证和用户界面展示。
 */
@Data
public class LoginVO {

  /**
   * JWT认证令牌
   *
   * <p>用于后续请求的身份验证，应存储在客户端（如localStorage）
   */
  private String token;

  /** 用户ID */
  private Long userId;

  /** 用户名 */
  private String username;

  /** 用户邮箱 */
  private String email;

  /** 角色ID */
  private Integer roleId;

  /** 角色名称 */
  private String roleName;

  /** 用户头像URL */
  private String avatarUrl;
}
