package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.Priority;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface PriorityService extends IService<Priority> {
  /** 根据优先级编码查询 */
  Priority getByPriorityCode(String priorityCode);

  /** 按排序权重查询所有优先级 */
  List<Priority> listOrderBySort();
}
