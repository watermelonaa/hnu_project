package com.baoma.natural_language_query.vo;

import java.util.List;
import lombok.Data;

/**
 * 表格数据视图对象
 *
 * <p>用于封装SQL查询结果的表格数据，包含表头和行数据。 前端可以使用这些数据渲染表格组件。
 */
@Data
public class TableDataVO {

  /**
   * 表格列头（字段名）
   *
   * <p>例如：["产品名称", "销售额", "数量"]
   */
  private List<String> headers;

  /**
   * 表格行数据
   *
   * <p>每一行是一个字符串列表，对应headers中的列。 例如：[["产品A", "10000", "100"], ["产品B", "8000", "80"]]
   */
  private List<List<String>> rows;
}
