package com.baoma.natural_language_query.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 分享查询记录数据传输对象
 *
 * <p>用于好友之间分享查询结果
 */
@Data
public class ShareQueryDTO {

  /** 分享者用户ID */
  private Long shareUserId;

  /** 接收者用户ID */
  private Long receiveUserId;

  /** 对话ID */
  private String dialogId;

  /** 查询标题/原始提问 */
  private String queryTitle;

  /** SQL语句 */
  private String sqlQuery;

  /** 数据库名称 */
  private String databaseName;

  /** 数据库连接ID */
  private Long dbConnectionId;

  /** 大模型名称 */
  private String llmName;

  /** 大模型配置ID */
  private Long llmConfigId;

  /** 执行耗时（毫秒） */
  private Long executionTime;

  /** 查询时间 */
  private String queryTime;

  /** 表格数据 */
  private TableDataDTO tableData;

  /** 图表数据 */
  private ChartDataDTO chartData;

  /** 分享留言（可选） */
  private String shareMessage;

  /** 表格数据结构 */
  @Data
  public static class TableDataDTO {
    /** 表头 */
    private List<String> headers;

    /** 数据行 */
    private List<List<Object>> rows;
  }

  /** 图表数据结构 */
  @Data
  public static class ChartDataDTO {
    /** 图表类型（bar/line/pie等） */
    private String type;

    /** X轴数据 */
    private List<String> xAxis;

    /** Y轴数据 */
    private List<Object> yAxis;

    /** 数据集（用于多系列图表） */
    private List<Map<String, Object>> datasets;
  }
}

