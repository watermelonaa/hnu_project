package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.dto.QueryRequestDTO;
import com.baoma.natural_language_query.dto.RecommendationRequestDTO;
import com.baoma.natural_language_query.vo.QueryResponseVO;
import java.util.List;
import java.util.Map;

/**
 * 自然语言查询服务接口
 *
 * <p>提供自然语言查询的核心业务逻辑，包括：
 *
 * <ul>
 *   <li>调用大模型解析用户自然语言查询
 *   <li>感知数据库元数据（表结构、字段信息等）
 *   <li>生成并验证SQL语句
 *   <li>检查用户权限（表权限）
 *   <li>执行SQL查询数据库
 *   <li>将查询结果转换为表格数据和图表数据
 *   <li>保存对话记录和查询日志
 * </ul>
 *
 * <p>支持多轮对话：用户可以根据查询结果调整查询内容，系统会基于对话历史优化SQL。
 */
public interface QueryService {

  /**
   * 执行自然语言查询
   *
   * <p>完整的查询流程：
   *
   * <ol>
   *   <li>验证请求参数（数据库连接ID、用户提示等）
   *   <li>获取数据库连接配置和表结构信息
   *   <li>调用大模型生成SQL（传入用户提示和数据库结构）
   *   <li>从生成的SQL中提取表名，检查用户权限
   *   <li>执行SQL查询数据库
   *   <li>将查询结果转换为表格数据和图表数据
   *   <li>保存对话记录到MongoDB
   *   <li>记录操作日志
   *   <li>返回查询结果
   * </ol>
   *
   * @param request 查询请求对象（包含用户提示、数据库连接ID、模型配置等）
   * @param userId 用户ID（用于权限检查和日志记录）
   * @return 查询响应对象（包含SQL、表格数据、图表数据等）
   * @throws RuntimeException 如果参数验证失败、权限不足、SQL执行失败等
   */
  QueryResponseVO executeQuery(QueryRequestDTO request, Long userId);

  /**
   * 生成智能推荐查询
   *
   * <p>基于对话历史，调用大模型生成3-5条相关的查询建议。
   *
   * <p>推荐流程：
   *
   * <ol>
   *   <li>分析对话历史（用户查询、SQL、查询结果等）
   *   <li>构建推荐prompt，包含对话上下文
   *   <li>调用大模型生成推荐
   *   <li>解析返回结果，提取推荐列表
   * </ol>
   *
   * @param request 推荐请求对象（包含对话历史、数据库连接ID、模型配置等）
   * @return 推荐查询列表（3-5条自然语言查询建议）
   * @throws RuntimeException 如果参数验证失败、大模型调用失败等
   */
  List<String> getRecommendations(RecommendationRequestDTO request);
  
  /**【新增】实现接口方法**/
  List<String> generateFollowupQuestions(String question, String schema);

  List<String> generateFollowupQuestionsWithResult(String question, String schema, List<Map<String, Object>> result);
}
