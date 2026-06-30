package com.baoma.natural_language_query.dto;

import lombok.Data;

/**
 * 修改密码数据传输对象
 *
 * <p>用于接收用户修改密码的请求数据。
 */
@Data
public class ChangePasswordDTO {

  /** 用户ID */
  private Long userId;

  /** 旧密码（明文，用于验证） */
  private String oldPassword;

  /** 新密码（明文，后端会进行加密存储） */
  private String newPassword;
}
