package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.DbConnection;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;
import java.util.List;

public interface DbConnectionService extends IService<DbConnection> {
  /** 根据创建者ID查询数据库连接列表 */
  List<DbConnection> listByCreateUserId(Long createUserId);

  /** 测试数据库连接 */
  boolean testConnection(Long id);

  /** 覆写父接口方法，改为软删除 */
  @Override
   boolean removeById(Serializable id);
    
	@Override
  List<DbConnection> list();
}
