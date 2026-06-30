package com.baoma.natural_language_query.vo;

import java.util.List;
import lombok.Data;

/**
 * 图表数据视图对象
 *
 * <p>用于封装Chart图表的数据结构，支持多种图表类型。 前端可以使用这些数据渲染Chart.js等图表库。
 *
 * <p>支持的图表类型：
 *
 * <ul>
 *   <li>bar - 柱状图
 *   <li>line - 折线图
 *   <li>pie - 饼图
 * </ul>
 */
@Data
public class ChartDataVO {

  /**
   * 图表类型
   *
   * <p>可选值：bar（柱状图）、line（折线图）、pie（饼图）
   */
  private String type;

  /**
   * 图表标签（X轴标签或饼图标签）
   *
   * <p>例如：["1月", "2月", "3月"]
   */
  private List<String> labels;

  /**
   * 数据集列表
   *
   * <p>一个图表可以包含多个数据集（如多条折线、多个系列）
   */
  private List<DatasetVO> datasets;
}
