package com.baoma.natural_language_query.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 查询分享视图对象
 *
 * <p>用于返回查询分享信息给前端
 */
@Data
public class QueryShareVO {

  /** 分享ID */
  private String shareId;

  /** 分享者用户ID */
  private Long shareUserId;

  /** 分享者用户名 */
  private String shareUserName;

  /** 分享者头像 */
  private String shareUserAvatar;

  /** 接收者用户ID */
  private Long receiveUserId;

  /** 接收者用户名 */
  private String receiveUserName;

  /** 对话ID */
  private String dialogId;

  /** 查询标题/原始提问 */
  private String queryTitle;

  /** SQL语句 */
  private String sqlQuery;

  /** 数据库信息 */
  private String databaseName;

  /** 数据库连接ID */
  private Long dbConnectionId;

  /** 大模型信息 */
  private String llmName;

  /** 大模型配置ID */
  private Long llmConfigId;

  /** 执行耗时（毫秒） */
  private Long executionTime;

  /** 执行耗时（格式化字符串） */
  private String executionTimeText;

  /** 查询时间 */
  private String queryTime;

  /** 分享时间 */
  private LocalDateTime shareTime;

  /** 表格数据 */
  private TableDataVO tableData;

  /** 图表数据 */
  private ChartDataVO chartData;

  /** 分享留言 */
  private String shareMessage;

  /** 接收状态：0-未处理，1-已保存，2-已删除 */
  private Integer receiveStatus;

  /** 表格数据视图对象 */
  @Data
  public static class TableDataVO {
    /** 表头 */
    private List<String> headers;

    /** 数据行 */
    private List<List<Object>> rows;

    /** 总行数 */
    private Integer totalRows;
  }

  /** 图表数据视图对象 */
  @Data
  public static class ChartDataVO {
    /** 图表类型 */
    private String type;

    /** X轴数据 */
    private List<String> xAxis;

    /** Y轴数据 */
    private List<Object> yAxis;

    /** 数据集 */
    private List<Map<String, Object>> datasets;
  }
}

