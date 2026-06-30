package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.ErrorLog;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ErrorLogService extends IService<ErrorLog> {
  /** 根据错误类型查询 */
  List<ErrorLog> listByErrorTypeId(Integer errorTypeId);

  /** 根据统计周期查询 */
  List<ErrorLog> listByPeriod(String period);
}
