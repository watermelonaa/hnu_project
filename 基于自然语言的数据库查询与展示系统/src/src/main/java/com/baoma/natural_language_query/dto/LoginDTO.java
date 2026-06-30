package com.baoma.natural_language_query.dto;

import lombok.Data;

/**
 * 用户登录数据传输对象
 *
 * <p>用于接收前端提交的用户登录请求数据。
 */
@Data
public class LoginDTO {

  /** 用户名 */
  private String username;

  /** 密码（明文，后端会进行加密验证） */
  private String password;
}
