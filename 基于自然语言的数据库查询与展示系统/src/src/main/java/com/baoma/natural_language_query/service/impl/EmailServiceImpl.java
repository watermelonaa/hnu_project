package com.baoma.natural_language_query.service.impl;

import com.baoma.natural_language_query.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现类
 *
 * <p>使用Spring Mail发送邮件，支持HTML格式邮件。
 *
 * @author 项目组
 * @version 1.0
 */
@Service
public class EmailServiceImpl implements EmailService {

  @Autowired private JavaMailSender mailSender;

  /** 发件人邮箱地址 */
  @Value("${spring.mail.username}")
  private String fromEmail;

  /** 前端应用地址，用于生成重置密码链接 */
  @Value("${app.frontend-url:http://localhost:5173}")
  private String frontendUrl;

  @Override
  public boolean sendPasswordResetEmail(String toEmail, String resetToken, String username, String resetBaseUrl) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // 设置发件人、收件人、主题
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject("密码重置 - 自然语言查询系统");

      // 构建重置密码链接（有效期1小时）
      // 优先使用传入的 resetBaseUrl，如果没有则使用默认的 frontendUrl
      String baseUrl = (resetBaseUrl != null && !resetBaseUrl.isEmpty()) 
          ? resetBaseUrl 
          : frontendUrl + "/reset-password";
      
      // 确保 baseUrl 包含 ? 或 & 符号
      String resetUrl = baseUrl.contains("?") 
          ? baseUrl + "&token=" + resetToken 
          : baseUrl + "?token=" + resetToken;

      // 构建邮件内容（HTML格式）
      String htmlContent =
          "<!DOCTYPE html>"
              + "<html>"
              + "<head>"
              + "<meta charset='UTF-8'>"
              + "<style>"
              + "  body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
              + "  .container { max-width: 600px; margin: 0 auto; padding: 20px; }"
              + "  .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }"
              + "  .content { background-color: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; }"
              + "  .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5 !important; color: white !important; text-decoration: none; border-radius: 5px; margin: 20px 0; }"
              + "  .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 20px; }"
              + "  .warning { color: #dc2626; font-size: 14px; margin-top: 20px; }"
              + "</style>"
              + "</head>"
              + "<body>"
              + "<div class='container'>"
              + "<div class='header'>"
              + "<h1>密码重置</h1>"
              + "</div>"
              + "<div class='content'>"
              + "<p>尊敬的 <strong>" + username + "</strong>，</p>"
              + "<p>您请求重置密码。请点击下面的按钮来重置您的密码：</p>"
              + "<p style='text-align: center;'>"
              + "<a href='" + resetUrl + "' class='button' style='color: #ffffff;'>重置密码</a>"
              + "</p>"
              + "<p>或者复制以下链接到浏览器中打开：</p>"
              + "<p style='word-break: break-all;'>"
              + "<a href='" + resetUrl + "' style='color: #4F46E5; text-decoration: underline;'>" + resetUrl + "</a>"
              + "</p>"
              + "<p class='warning'>"
              + "<strong>注意：</strong>此链接将在1小时后过期。如果您没有请求重置密码，请忽略此邮件。"
              + "</p>"
              + "</div>"
              + "<div class='footer'>"
              + "<p>此邮件由系统自动发送，请勿回复。</p>"
              + "<p>© 2024 自然语言查询系统. All Rights Reserved.</p>"
              + "</div>"
              + "</div>"
              + "</body>"
              + "</html>";

      helper.setText(htmlContent, true);

      // 发送邮件
      mailSender.send(message);
      return true;
    } catch (MessagingException e) {
      e.printStackTrace();
      return false;
    }
  }
}

