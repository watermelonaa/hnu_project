package com.baoma.natural_language_query.vo;

import java.util.List;
import lombok.Data;

/**
 * 图表数据集视图对象
 *
 * <p>用于封装Chart图表中单个数据集的信息。 一个图表可以包含多个数据集（如多条折线、多个柱状图系列）。
 */
@Data
public class DatasetVO {

  /**
   * 数据集标签（图例名称）
   *
   * <p>例如："销售额"、"利润"
   */
  private String label;

  /**
   * 数据值列表
   *
   * <p>与ChartDataVO中的labels一一对应 例如：[10000, 8000, 12000]
   */
  private List<Number> data;

  /**
   * 背景颜色
   *
   * <p>可以是字符串（单一颜色）或数组（每个数据点一个颜色） 例如："#FF6384" 或 ["#FF6384", "#36A2EB", "#FFCE56"]
   */
  private Object backgroundColor;
}
