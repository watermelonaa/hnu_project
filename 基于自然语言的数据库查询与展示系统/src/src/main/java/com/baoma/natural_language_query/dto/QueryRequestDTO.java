package com.baoma.natural_language_query.dto;

import lombok.Data;

/**
 * 自然语言查询请求数据传输对象
 *
 * <p>用于接收前端提交的自然语言查询请求，包含用户查询意图、 数据库连接信息、大模型配置等。
 *
 * <p>系统会根据这些信息：
 *
 * <ol>
 *   <li>调用大模型解析用户的自然语言查询
 *   <li>感知数据库元数据（表结构、字段信息等）
 *   <li>生成并验证SQL语句
 *   <li>执行查询并返回结果
 * </ol>
 */
@Data
public class QueryRequestDTO {

  /**
   * 用户的自然语言查询语句
   *
   * <p>例如："查询销售额最高的前10个产品"
   */
  private String userPrompt;

  /**
   * 使用的大模型配置ID
   *
   * <p>用于指定调用哪个LLM配置来生成SQL
   */
  private String model;

  /**
   * 使用的数据库名称（兼容旧版）
   *
   * <p>注意：实际使用dbConnectionId，此字段保留用于向后兼容
   */
  private String database;

  /**
   * 数据库连接ID
   *
   * <p>指定要查询的数据库连接配置
   */
  private Long dbConnectionId;

  /**
   * 对话ID（用于多轮对话）
   *
   * <p>如果为空，系统会生成新的对话ID。 相同对话ID的多次查询可以保持上下文，支持用户调整查询内容。
   */
  private String conversationId;
}
