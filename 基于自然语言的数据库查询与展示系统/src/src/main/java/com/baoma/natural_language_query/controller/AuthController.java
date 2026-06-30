package com.baoma.natural_language_query.controller;

import com.baoma.natural_language_query.annotation.RequirePermission;
import com.baoma.natural_language_query.common.Result;
import com.baoma.natural_language_query.dto.LoginDTO;
import com.baoma.natural_language_query.dto.ResetPasswordDTO;
import com.baoma.natural_language_query.enums.Role;
import com.baoma.natural_language_query.entity.mysql.User;
import com.baoma.natural_language_query.service.AuthService;
import com.baoma.natural_language_query.service.EmailService;
import com.baoma.natural_language_query.service.UserService;
import com.baoma.natural_language_query.utils.JwtUtil;
import com.baoma.natural_language_query.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  /** 认证服务 */
  @Autowired private AuthService authService;

  /** JWT工具类 */
  @Autowired private JwtUtil jwtUtil;

  /** 用户服务 */
  @Autowired private UserService userService;

  /** 邮件服务 */
  @Autowired private EmailService emailService;

  /** 重置密码链接地址（从配置文件读取，默认为前端重置页面） */
  @Value("${app.reset-password-url:http://localhost:5173/reset-password}")
  private String resetPasswordUrl;

  /**
   * 用户登录
   */
  @PostMapping("/login")
  public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
    try {
      LoginVO loginVO = authService.login(loginDTO);
      return Result.success(loginVO);
    } catch (DisabledException e) {
      // 账号被禁用
      return Result.error("账号已被禁用或锁定，请联系管理员");
    } catch (BadCredentialsException e) {
      // 用户名或密码错误
      return Result.error("用户名或密码错误");
    } catch (AuthenticationException e) {
      // 其他认证异常
      return Result.error(e.getMessage());
    } catch (Exception e) {
      // 其他异常
      return Result.error(e.getMessage());
    }
  }

  /**
   * 用户登出
   */
  @PostMapping("/logout")
  public Result<Void> logout(HttpServletRequest request) {
    try {
      // 登出接口不再强制要求 Role.USER 权限，避免退出时 Token 失效导致报错
      // 从请求头中获取token
      String authHeader = request.getHeader("Authorization");
      String token = null;
      if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
      }

      // 从请求属性中获取userId（由JwtInterceptor设置）
      Long userId = (Long) request.getAttribute("userId");

      // 如果无法从请求属性获取userId，尝试从token中提取
      if (userId == null && token != null && jwtUtil.validateToken(token)) {
        try {
          userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
          // Token解析失败不影响退出流程
        }
      }

      // 执行登出操作（即便 userId 为空，也让它通过，前端只需确认接口返回成功）
      if (token != null || userId != null) {
        authService.logout(token, userId);
      }

      return Result.success();
    } catch (Exception e) {
      // 退出过程中的任何异常都不应阻止前端完成退出逻辑
      return Result.success(); 
    }
  }

  /**
   * 请求重置密码
   */
  @PostMapping("/forgot-password")
  public Result<Void> forgotPassword(@RequestBody java.util.Map<String, String> request) {
    try {
      String email = request.get("email");
      if (!StringUtils.hasText(email)) {
        return Result.error("邮箱地址不能为空");
      }

      // 验证邮箱格式
      if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
        return Result.error("邮箱格式不正确");
      }

      // 根据邮箱查找用户
      User user = userService.getByEmail(email);
      
      // 出于安全考虑，无论用户是否存在都返回成功，避免邮箱枚举攻击
      if (user != null) {
        // 检查用户账号状态
        if (user.getStatus() == 0) {
          // 账号被禁用，不发送邮件，但仍返回成功
          return Result.success();
        }

        // 生成重置密码令牌（1小时有效期）
        String resetToken = jwtUtil.generatePasswordResetToken(user.getId(), user.getEmail());

        // 发送重置密码邮件（使用配置文件中的重置密码URL）
        boolean emailSent = emailService.sendPasswordResetEmail(
            user.getEmail(), resetToken, user.getUsername(), resetPasswordUrl);

        if (!emailSent) {
          // 邮件发送失败，返回友好的错误信息
          return Result.error("邮件发送失败，请检查邮箱配置或稍后重试");
        }
      }

      // 返回成功
      return Result.success();
    } catch (Exception e) {
      e.printStackTrace();
      return Result.error("请求处理失败: " + e.getMessage());
    }
  }

  /**
   * 重置密码
   */
  @PostMapping("/reset-password")
  public Result<Void> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
    try {
      if (!StringUtils.hasText(resetPasswordDTO.getToken())) {
        return Result.error("令牌不能为空");
      }
      if (!StringUtils.hasText(resetPasswordDTO.getNewPassword())) {
        return Result.error("新密码不能为空");
      }

      // 验证令牌并获取用户 ID
      if (!jwtUtil.validateToken(resetPasswordDTO.getToken())) {
        return Result.error("重置链接已过期或无效，请重新请求");
      }

      Long userId = jwtUtil.getUserIdFromToken(resetPasswordDTO.getToken());
      
      // 更新用户密码
      boolean success = userService.resetPassword(userId, resetPasswordDTO.getNewPassword());
      
      if (success) {
        return Result.success();
      } else {
        return Result.error("密码重置失败");
      }
    } catch (Exception e) {
      e.printStackTrace();
      return Result.error("请求处理失败: " + e.getMessage());
    }
  }
}
