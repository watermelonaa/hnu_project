package com.baoma.natural_language_query.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Token黑名单服务
 *
 * <p>用于管理JWT token的黑名单，实现单点登录功能。当用户重新登录时，旧的token会被加入黑名单。
 *
 * <p>功能说明：
 *
 * <ul>
 *   <li>将token加入黑名单（用于登出或重复登录时使旧token失效）
 *   <li>检查token是否在黑名单中
 *   <li>管理用户当前活跃的token（用于单点登录）
 * </ul>
 */
@Service
public class TokenBlacklistService {

  @Autowired private StringRedisTemplate redisTemplate;

  /** Token过期时间（毫秒，从配置文件读取，默认24小时） */
  @Value("${jwt.expiration:86400000}")
  private long expirationTime;

  /** Redis key前缀：token黑名单 */
  private static final String BLACKLIST_PREFIX = "token:blacklist:";

  /** Redis key前缀：用户当前活跃token */
  private static final String USER_TOKEN_PREFIX = "user:token:";

  /**
   * 将token加入黑名单
   *
   * @param token JWT token
   */
  public void addToBlacklist(String token) {
    // 将token加入黑名单，设置过期时间与JWT token过期时间一致
    redisTemplate
        .opsForValue()
        .set(BLACKLIST_PREFIX + token, "1", expirationTime, TimeUnit.MILLISECONDS);
  }

  /**
   * 检查token是否在黑名单中
   *
   * @param token JWT token
   * @return true表示token在黑名单中（已失效），false表示token有效
   */
  public boolean isBlacklisted(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
  }

  /**
   * 保存用户当前活跃的token
   *
   * @param userId 用户ID
   * @param token JWT token
   */
  public void saveUserToken(Long userId, String token) {
    // 保存用户当前token，设置过期时间与JWT token过期时间一致
    redisTemplate
        .opsForValue()
        .set(USER_TOKEN_PREFIX + userId, token, expirationTime, TimeUnit.MILLISECONDS);
  }

  /**
   * 获取用户当前活跃的token
   *
   * @param userId 用户ID
   * @return 用户当前活跃的token，如果不存在则返回null
   */
  public String getUserToken(Long userId) {
    return redisTemplate.opsForValue().get(USER_TOKEN_PREFIX + userId);
  }

  /**
   * 使旧token失效（将旧token加入黑名单）
   *
   * @param userId 用户ID
   */
  public void invalidateOldToken(Long userId) {
    String oldToken = getUserToken(userId);
    if (oldToken != null && !oldToken.isEmpty()) {
      // 将旧token加入黑名单
      addToBlacklist(oldToken);
    }
  }

  /**
   * 删除用户当前活跃的token记录
   *
   * @param userId 用户ID
   */
  public void removeUserToken(Long userId) {
    redisTemplate.delete(USER_TOKEN_PREFIX + userId);
  }
}

