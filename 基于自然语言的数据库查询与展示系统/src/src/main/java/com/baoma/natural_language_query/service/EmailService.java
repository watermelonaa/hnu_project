package com.baoma.natural_language_query.service;

/**
 * 邮件服务接口
 *
 * <p>用于发送系统邮件，如密码重置邮件等。
 *
 * @author 项目组
 * @version 1.0
 */
public interface EmailService {

  /**
   * 发送密码重置邮件
   *
   * @param toEmail 收件人邮箱地址
   * @param resetToken 重置密码令牌
   * @param username 用户名
   * @param resetBaseUrl 重置密码页面基础URL
   * @return 是否发送成功
   */
  boolean sendPasswordResetEmail(String toEmail, String resetToken, String username, String resetBaseUrl);
}

