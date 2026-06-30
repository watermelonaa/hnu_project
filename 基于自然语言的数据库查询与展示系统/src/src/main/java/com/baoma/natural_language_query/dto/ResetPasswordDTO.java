package com.baoma.natural_language_query.dto;

import lombok.Data;

/**
 * 重置密码DTO
 */
@Data
public class ResetPasswordDTO {
    /** 重置令牌 */
    private String token;
    /** 新密码 */
    private String newPassword;
}

