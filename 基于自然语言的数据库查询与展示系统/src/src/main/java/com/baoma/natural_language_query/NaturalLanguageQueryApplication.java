package com.baoma.natural_language_query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基于自然语言的数据库查询和展示系统 - 主应用启动类
 *
 * <p>系统功能概述：
 *
 * <ul>
 *   <li>管理员可以管理和授权用户
 *   <li>授权的用户可以查询授权的数据库和表格
 *   <li>采用自然语言描述查询请求，系统调用大模型感知数据库元数据并解析用户查询请求
 *   <li>采用SQL引擎生成和验证SQL语句，测试通过后将查询的数据采用Chart图显示
 *   <li>用户可调整查询内容，系统根据用户调整请求修改SQL语句，重新查询和可视化查询结果
 *   <li>支持多表联合等复杂SQL语句查询
 *   <li>支持至少5个及以上并发用户，每个用户的任务在Docker容器独立运行
 * </ul>
 *
 * <p>技术栈：
 *
 * <ul>
 *   <li>后端：SpringBoot框架，JDK21，Java21
 *   <li>数据库：MySQL（关系型数据），MongoDB（非关系型数据）
 *   <li>ORM：MyBatis Plus
 *   <li>安全：Spring Security + JWT
 *   <li>容器化：Docker
 * </ul>
 *
 * @author 神奇宝码队
 * @version 2.0
 * @since 2025
 */
@SpringBootApplication
public class NaturalLanguageQueryApplication {

  /** 应用程序入口方法 */
  public static void main(String[] args) {
    SpringApplication.run(NaturalLanguageQueryApplication.class, args);
  }
}
