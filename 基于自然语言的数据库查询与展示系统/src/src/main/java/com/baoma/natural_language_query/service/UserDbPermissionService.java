package com.baoma.natural_language_query.service;

import com.baoma.natural_language_query.entity.mysql.UserDbPermission;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface UserDbPermissionService extends IService<UserDbPermission> {
  /** 根据用户ID查询权限 */
  UserDbPermission getByUserId(Long userId);

  /** 查询所有已分配权限的用户 */
  List<UserDbPermission> listAssigned();

  /** 查询所有未分配权限的用户 */
  List<UserDbPermission> listUnassigned();
}
