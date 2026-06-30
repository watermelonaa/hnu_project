package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.QueryLog;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface QueryLogService extends IService<QueryLog> {
  /** 根据用户ID查询查询日志 */
  List<QueryLog> listByUserId(Long userId);

  /** 根据对话ID查询查询日志 */
  List<QueryLog> listByDialogId(String dialogId);
}
