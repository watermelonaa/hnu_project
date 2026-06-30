package com.baoma.natural_language_query.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 推荐查询请求数据传输对象
 *
 * <p>用于接收前端提交的推荐查询请求，包含对话历史、数据库连接信息、大模型配置等。
 *
 * <p>系统会根据这些信息：
 *
 * <ol>
 *   <li>分析对话历史中的用户查询和AI回复
 *   <li>调用大模型基于上下文生成智能推荐
 *   <li>返回3-5条相关的查询建议
 * </ol>
 */
@Data
public class RecommendationRequestDTO {

  /**
   * 对话ID（可选）
   *
   * <p>如果提供，系统可以从数据库中加载完整的对话历史
   */
  private String conversationId;

  /**
   * 对话历史（用户消息和AI回复的列表）
   *
   * <p>格式：[
   *   {"role": "user", "content": "查询2023年各季度订单量"},
   *   {"role": "ai", "content": {"sqlQuery": "...", "tableData": {...}}}
   * ]
   */
  private List<Map<String, Object>> conversationHistory;

  /**
   * 数据库连接ID
   *
   * <p>用于获取数据库信息，帮助生成更准确的推荐
   */
  private Long dbConnectionId;

  /**
   * 大模型配置ID
   *
   * <p>用于指定调用哪个LLM配置来生成推荐
   */
  private String llmConfigId;

  /**
   * 查询是否失败
   *
   * <p>true: 查询失败，生成错误处理建议
   * <p>false: 查询成功，生成继续探索建议
   */
  private Boolean queryFailed;
}

