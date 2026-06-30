package com.baoma.natural_language_query.vo;

import java.util.List;

import lombok.Data;

/**
 * 自然语言查询响应视图对象
 *
 * <p>用于返回自然语言查询的执行结果，包括：
 *
 * <ul>
 *   <li>生成的SQL语句
 *   <li>查询结果数据（表格格式）
 *   <li>图表数据（用于前端Chart图展示）
 *   <li>查询元数据（时间、数据库、模型等）
 * </ul>
 *
 * <p>前端可以根据这些数据：
 *
 * <ul>
 *   <li>展示查询结果表格
 *   <li>渲染Chart图表（柱状图、折线图、饼图等）
 *   <li>显示查询历史信息
 *   <li>支持用户调整查询内容进行多轮对话
 * </ul>
 */
@Data
public class QueryResponseVO {

  /** 查询记录ID */
  private String id;

  /** 用户的自然语言查询语句 */
  private String userPrompt;

  /** 系统生成的SQL查询语句 */
  private String sqlQuery;

  /** 对话ID（用于多轮对话） */
  private String conversationId;

  /** 查询时间 */
  private String queryTime;

  /** SQL执行时间（毫秒） */
  private String executionTime;

  /** 表格数据（用于表格展示） */
  private TableDataVO tableData;

  /** 图表数据（用于Chart图展示） */
  private ChartDataVO chartData;

  /** 数据库名称 */
  private String database;

  /** 数据库连接ID（用于保存查询日志） */
  private Long dbConnectionId;

  /** 使用的大模型名称 */
  private String model;
  
  /** 大模型配置ID（用于保存查询日志） */
  private Long llmConfigId;

  
  /**【新增】后续探索问题 **/
  private List<String> followupQuestions;

  /** 空构造方法，初始化列表 **/
  public QueryResponseVO() {
    this.followupQuestions = new java.util.ArrayList<>();
  }
  
}
