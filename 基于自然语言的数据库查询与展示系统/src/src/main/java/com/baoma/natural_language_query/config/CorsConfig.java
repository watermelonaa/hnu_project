package com.baoma.natural_language_query.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS跨域资源共享配置类
 *
 * <p>配置允许前端应用跨域访问后端API接口。 解决浏览器同源策略限制，允许不同域名、端口或协议的前端应用访问后端服务。
 *
 * <p>配置说明：
 *
 * <ul>
 *   <li>允许所有来源（生产环境建议限制为具体域名）
 *   <li>允许携带凭证（cookies、authorization headers等）
 *   <li>允许所有HTTP方法（GET、POST、PUT、DELETE等）
 *   <li>允许所有请求头
 *   <li>预检请求缓存时间：3600秒
 * </ul>
 */
@Configuration
public class CorsConfig {

  /**
   * 创建CORS过滤器Bean
   *
   * <p>配置跨域访问规则，对所有路径（/**）应用CORS配置。
   *
   * @return CORS过滤器实例
   */
  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    // 允许所有来源（生产环境应限制为具体域名）
    config.addAllowedOriginPattern("*");
    // 允许携带凭证
    config.setAllowCredentials(true);
    // 允许所有HTTP方法
    config.addAllowedMethod("*");
    // 允许所有请求头
    config.addAllowedHeader("*");
    // 预检请求缓存时间（秒）
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 对所有路径应用CORS配置
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
