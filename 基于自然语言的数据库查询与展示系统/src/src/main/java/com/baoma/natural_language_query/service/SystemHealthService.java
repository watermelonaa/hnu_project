package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.SystemHealth;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface SystemHealthService extends IService<SystemHealth> {
  /** 查询最新的健康记录 */
  SystemHealth getLatest();

  /** 查询最近N条健康记录 */
  List<SystemHealth> listRecent(int limit);
}
