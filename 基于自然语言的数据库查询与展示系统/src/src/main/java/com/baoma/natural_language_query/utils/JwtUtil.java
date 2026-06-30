package com.baoma.natural_language_query.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT工具类
 *
 * <p>用于生成、验证和解析JWT（JSON Web Token）令牌。 JWT用于无状态的身份认证，用户登录后获得令牌，后续请求携带令牌进行身份验证。
 *
 * <p>功能说明：
 *
 * <ul>
 *   <li>生成JWT令牌（包含用户ID和用户名）
 *   <li>验证令牌有效性（签名、过期时间等）
 *   <li>从令牌中提取用户信息（用户ID、用户名）
 * </ul>
 *
 * <p>令牌有效期：24小时
 *
 * @author 项目组
 * @version 1.0
 */
@Component
public class JwtUtil {

  /**
   * JWT签名密钥（从配置文件读取）
   *
   * <p>如果未配置，使用默认密钥（仅用于开发环境）
   */
  @Value("${jwt.secret:natural_language_query_system_jwt_secret_key_2024}")
  private String jwtSecret;

  /**
   * 令牌过期时间（毫秒，从配置文件读取）
   *
   * <p>默认24小时 = 86400000毫秒
   */
  @Value("${jwt.expiration:86400000}")
  private long expirationTime;

  /** 获取JWT签名密钥 */
  private Key getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
  }

  /**
   * 生成JWT令牌
   *
   * <p>令牌包含以下信息：
   *
   * <ul>
   *   <li>subject: 用户名
   *   <li>userId: 用户ID（自定义声明）
   *   <li>issuedAt: 签发时间
   *   <li>expiration: 过期时间（24小时后）
   * </ul>
   *
   * @param userId 用户ID
   * @param username 用户名
   * @return JWT令牌字符串
   */
  public String generateToken(Long userId, String username) {
    return Jwts.builder()
        .setSubject(username)
        .claim("userId", userId)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * 从令牌中解析Claims（声明）
   *
   * @param token JWT令牌
   * @return Claims对象，包含令牌中的所有声明
   * @throws JwtException 如果令牌无效或已过期
   */
  public Claims getClaimsFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSigningKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * 验证JWT令牌的有效性
   *
   * <p>验证包括：
   *
   * <ul>
   *   <li>签名验证（确保令牌未被篡改）
   *   <li>过期时间验证（确保令牌未过期）
   * </ul>
   *
   * @param token JWT令牌
   * @return true表示令牌有效，false表示令牌无效或已过期
   */
  public boolean validateToken(String token) {
    try {
      getClaimsFromToken(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * 从令牌中提取用户ID
   *
   * @param token JWT令牌
   * @return 用户ID
   */
  public Long getUserIdFromToken(String token) {
    Claims claims = getClaimsFromToken(token);
    return claims.get("userId", Long.class);
  }

  /**
   * 从令牌中提取用户名
   *
   * @param token JWT令牌
   * @return 用户名（subject）
   */
  public String getUsernameFromToken(String token) {
    return getClaimsFromToken(token).getSubject();
  }

  /**
   * 生成密码重置令牌
   *
   * <p>用于密码重置功能，令牌有效期1小时（3600000毫秒）
   *
   * <p>令牌包含以下信息：
   *
   * <ul>
   *   <li>subject: 用户邮箱
   *   <li>userId: 用户ID（自定义声明）
   *   <li>type: 令牌类型，值为"password-reset"（自定义声明）
   *   <li>issuedAt: 签发时间
   *   <li>expiration: 过期时间（1小时后）
   * </ul>
   *
   * @param userId 用户ID
   * @param email 用户邮箱
   * @return 重置密码令牌字符串
   */
  public String generatePasswordResetToken(Long userId, String email) {
    return Jwts.builder()
        .setSubject(email)
        .claim("userId", userId)
        .claim("type", "password-reset")
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1小时
        .signWith(getSigningKey())
        .compact();
  }
}
