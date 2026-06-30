package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.DbConnection;
import java.util.Map;

/**
 * 大模型（LLM）调用服务接口
 *
 * <p>提供与大模型交互的接口，用于将用户的自然语言查询转换为SQL语句。 系统支持多种大模型（如GPT、Claude等），通过配置管理不同的模型。
 *
 * <p>功能说明：
 *
 * <ul>
 *   <li>调用大模型API，传入用户自然语言查询和数据库结构信息
 *   <li>大模型根据用户意图和数据库结构生成SQL语句
 *   <li>解析大模型返回的结果，提取SQL、表格数据、图表数据等
 * </ul>
 *
 * <p>数据库结构感知：
 *
 * <ul>
 *   <li>系统会自动获取数据库表结构信息（表名、字段名、字段类型、注释等）
 *   <li>将表结构信息传递给大模型，帮助大模型生成更准确的SQL
 *   <li>支持多表联合查询等复杂SQL生成
 * </ul>
 */
public interface LLMService {

  /**
   * 调用大模型生成SQL和结果（基础版本）
   *
   * @param prompt 用户的自然语言查询提示词
   * @param modelName 大模型名称
   * @param databaseName 数据库名称
   * @return 包含以下键的Map：
   *     <ul>
   *       <li>sql: 生成的SQL语句
   *       <li>tableData: 表格数据（如果大模型直接返回结果）
   *       <li>chartData: 图表数据（如果大模型直接返回结果）
   *     </ul>
   */
  Map<String, Object> generateQuery(String prompt, String modelName, String databaseName);

  /**
   * 调用大模型生成SQL和结果（包含数据库结构信息）
   *
   * <p>将数据库表结构信息传递给大模型，帮助生成更准确的SQL。
   *
   * @param prompt 用户的自然语言查询提示词
   * @param modelName 大模型名称
   * @param databaseName 数据库名称
   * @param schemaInfo 数据库表结构信息（格式化的字符串）
   * @return 包含SQL、表格数据和图表数据的Map
   */
  Map<String, Object> generateQueryWithSchema(
      String prompt, String modelName, String databaseName, String schemaInfo);

  /**
   * 调用大模型生成SQL（自动获取表结构）
   *
   * <p>根据数据库连接配置自动获取表结构信息，然后调用大模型生成SQL。 这是最常用的方法，系统会自动处理表结构获取和格式化。
   *
   * @param prompt 用户的自然语言查询提示词
   * @param modelConfigId 大模型配置ID（用于获取模型配置信息）
   * @param databaseName 数据库名称
   * @param dbConnection 数据库连接配置（用于自动获取表结构）
   * @return 包含SQL、表格数据和图表数据的Map
   */
  Map<String, Object> generateQueryWithConnection(
      String prompt, String modelConfigId, String databaseName, DbConnection dbConnection);

  /**
   * 调用大模型生成SQL（使用自定义的system和user消息）
   *
   * <p>使用自定义的system和user消息调用大模型，不进行额外的prompt包装。
   *
   * @param systemMessage 系统消息（角色为system）
   * @param userMessage 用户消息（角色为user）
   * @param modelConfigId 大模型配置ID（用于获取模型配置信息）
   * @return 包含SQL、表格数据和图表数据的Map，以及原始响应内容
   */
  Map<String, Object> generateQueryWithCustomMessages(
      String systemMessage, String userMessage, String modelConfigId);

  /**
   * 调用大模型生成SQL（支持多轮对话历史）
   *
   * <p>传入完整的对话历史消息列表，支持多轮对话的上下文感知。
   * 消息格式：[{"role": "system", "content": "..."}, {"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
   *
   * @param messages 完整的消息列表（包含历史对话）
   * @param modelConfigId 大模型配置ID
   * @return 包含SQL、表格数据和图表数据的Map，以及原始响应内容
   */
  Map<String, Object> generateQueryWithConversationHistory(
      java.util.List<java.util.Map<String, String>> messages, String modelConfigId);
}
